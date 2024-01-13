/*
 * Copyright (c) 2022 KCloud-Platform-Alibaba Author or Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.laokou.common.elasticsearch.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.cluster.metadata.AliasMetadata;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.laokou.common.core.utils.CollectionUtil;
import org.laokou.common.core.utils.JacksonUtil;
import org.laokou.common.core.utils.MapUtil;
import org.laokou.common.elasticsearch.clientobject.SettingsCO;
import org.laokou.common.elasticsearch.constant.EsConstant;
import org.laokou.common.elasticsearch.utils.FieldMapping;
import org.laokou.common.elasticsearch.utils.FieldMappingUtil;
import org.laokou.common.i18n.common.exception.SystemException;
import org.laokou.common.i18n.dto.Datas;
import org.laokou.common.i18n.dto.Search;
import org.laokou.common.i18n.utils.LogUtil;
import org.laokou.common.i18n.utils.ObjectUtil;
import org.laokou.common.i18n.utils.StringUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import static org.laokou.common.i18n.common.StringConstants.EMPTY;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class ElasticsearchTemplate {

	private final RestHighLevelClient restHighLevelClient;

	private static final String PRIMARY_KEY = "id";

	private static final String HIGHLIGHT_PRE_TAGS = "<span style='color:red;'>";

	private static final String HIGHLIGHT_POST_TAGS = "</span>";

	/**
	 * 批量同步数据到ES.
	 * @param indexName 索引名称
	 * @param jsonDataList 数据列表
	 * @throws IOException IOException
	 */
	public void syncBatchIndex(String indexName, String jsonDataList) throws IOException {
		// 判空
		if (StringUtil.isEmpty(jsonDataList)) {
			log.error("数据不能为空，批量同步索引失败");
			return;
		}
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，批量同步索引失败", indexName);
			return;
		}
		// 批量操作Request
		BulkRequest bulkRequest = packBulkIndexRequest(indexName, jsonDataList);
		if (bulkRequest.requests().isEmpty()) {
			log.error("批量同步索引失败");
			return;
		}
		final BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
		if (bulk.hasFailures()) {
			for (BulkItemResponse item : bulk.getItems()) {
				log.error("索引：{}，主键：{}，状态为：{}，错误信息：{} -> 批量同步索引失败", indexName, item.getId(), item.status(),
						item.getFailureMessage());
			}
			return;
		}
		// 记录索引新增与修改数量
		Integer createdCount = 0;
		Integer updatedCount = 0;
		for (BulkItemResponse item : bulk.getItems()) {
			if (DocWriteResponse.Result.CREATED.equals(item.getResponse().getResult())) {
				createdCount++;
			}
			else if (DocWriteResponse.Result.UPDATED.equals(item.getResponse().getResult())) {
				updatedCount++;
			}
		}
		log.info("索引：{}，新增{}个，修改{}个 -> 批量同步索引成功", indexName, createdCount, updatedCount);
	}

	/**
	 * 批量修改ES.
	 * @param indexName 索引名称
	 * @param jsonDataList 数据列表
	 * @param clazz 类型
	 */
	public Boolean updateBatchIndex(String indexName, String jsonDataList, Class<?> clazz) {
		// 判空
		if (StringUtil.isEmpty(jsonDataList)) {
			log.error("数据不能为空，批量修改索引失败");
			return false;
		}
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，批量修改索引失败", indexName);
			return false;
		}
		BulkRequest bulkRequest = packBulkUpdateRequest(indexName, jsonDataList);
		if (bulkRequest.requests().isEmpty()) {
			log.error("批量修改索引失败");
			return false;
		}
		try {
			// 同步执行
			BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
			if (bulk.hasFailures()) {
				for (BulkItemResponse item : bulk.getItems()) {
					log.error("索引：{}，主键：{}，状态：{}，错误信息：{} -> 批量修改索引失败", indexName, item.getId(), item.status(),
							item.getFailureMessage());
				}
				return false;
			}
			// 记录索引新增与修改数量
			Integer createCount = 0;
			Integer updateCount = 0;
			for (BulkItemResponse item : bulk.getItems()) {
				if (DocWriteResponse.Result.CREATED.equals(item.getResponse().getResult())) {
					createCount++;
				}
				else if (DocWriteResponse.Result.UPDATED.equals(item.getResponse().getResult())) {
					updateCount++;
				}
			}
			log.info("索引：{}，新增{}个，修改{}个 -> 批量修改索引成功", indexName, createCount, updateCount);
		}
		catch (IOException e) {
			log.error("索引：{} -> 批量修改索引更新，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
			return false;
		}
		return true;
	}

	/**
	 * ES修改数据.
	 * @param indexName 索引名称
	 * @param id 主键
	 * @param paramJson 参数JSON
	 */
	public Boolean updateIndex(String indexName, String id, String paramJson) {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，修改索引失败", indexName);
			return false;
		}
		UpdateRequest updateRequest = new UpdateRequest(indexName, id);
		// 如果修改索引中不存在则进行新增
		updateRequest.docAsUpsert(true);
		// 立即刷新数据
		updateRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		updateRequest.doc(paramJson, XContentType.JSON);
		try {
			UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
			log.info("索引：{}，主键：{}，状态：{}", indexName, id, updateResponse.getResult());
			if (DocWriteResponse.Result.CREATED.equals(updateResponse.getResult())) {
				// 新增
				log.info("索引{}，主键：{} -> 新增索引成功", indexName, id);
			}
			else if (DocWriteResponse.Result.UPDATED.equals(updateResponse.getResult())) {
				// 修改
				log.info("索引：{}，主键：{} -> 修改索引成功", indexName, id);
			}
			else if (DocWriteResponse.Result.NOOP.equals(updateResponse.getResult())) {
				// 无变化
				log.info("索引：{}，主键：{} -> 索引无变化", indexName, id);
			}
		}
		catch (IOException e) {
			log.error("索引：{}，主键：{} -> 修改索引失败，错误信息：{}，详情见日志", indexName, id, LogUtil.result(e.getMessage()), e);
		}
		return true;
	}

	/**
	 * 删除数据.
	 * @param indexName 索引名称
	 * @param id 主键
	 * @return Boolean
	 */
	public Boolean deleteById(String indexName, String id) {
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{}，索引不存在，删除索引失败", indexName);
			return false;
		}
		DeleteRequest deleteRequest = new DeleteRequest(indexName);
		deleteRequest.id(id);
		deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		try {
			DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
			if (DocWriteResponse.Result.NOT_FOUND.equals(deleteResponse.getResult())) {
				log.error("索引：{}，主键：{} -> 删除索引失败", indexName, id);
			}
			else {
				log.info("索引：{}，主键：{} -> 删除索引成功", indexName, id);
			}
		}
		catch (IOException e) {
			log.error("索引：{} -> 删除索引失败，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
		}
		return true;
	}

	/**
	 * 批量删除ES.
	 * @param indexName 索引名称
	 * @param ids id列表
	 * @return Boolean
	 */
	public Boolean deleteBatchIndex(String indexName, List<String> ids) {
		if (CollectionUtil.isEmpty(ids)) {
			log.error("IDS不能为空，批量删除索引失败");
			return false;
		}
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{}，索引不存在，批量删除索引失败", indexName);
			return false;
		}
		BulkRequest bulkRequest = packBulkDeleteRequest(indexName, ids);
		try {
			BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
			if (bulk.hasFailures()) {
				for (BulkItemResponse item : bulk.getItems()) {
					log.error("索引：{}，主键：{}，错误信息：{} -> 批量删除索引失败", indexName, item.getId(), item.getFailureMessage());
				}
				return false;
			}
			// 记录索引新增与修改数量
			Integer deleteCount = 0;
			for (BulkItemResponse item : bulk.getItems()) {
				if (DocWriteResponse.Result.DELETED.equals(item.getResponse().getResult())) {
					deleteCount++;
				}
			}
			log.info("索引：{}，删除{}个 -> 批量删除索引成功", indexName, deleteCount);
		}
		catch (IOException e) {
			log.error("索引：{} -> 批量删除索引失败，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
		}
		return true;
	}

	/**
	 * 组装删除操作.
	 * @param indexName 索引名称
	 * @param ids id列表
	 * @return BulkRequest
	 */
	private BulkRequest packBulkDeleteRequest(String indexName, List<String> ids) {
		BulkRequest bulkRequest = new BulkRequest();
		bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		ids.forEach(id -> {
			DeleteRequest deleteRequest = new DeleteRequest(indexName);
			deleteRequest.id(id);
			bulkRequest.add(deleteRequest);
		});
		return bulkRequest;
	}

	/**
	 * 组装bulkUpdate.
	 * @param indexName 索引名称
	 * @param jsonDataList 数据列表
	 * @return BulkRequest
	 */
	private BulkRequest packBulkUpdateRequest(String indexName, String jsonDataList) {
		BulkRequest bulkRequest = new BulkRequest();
		bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		List<Object> jsonList = JacksonUtil.toList(jsonDataList, Object.class);
		if (jsonList.isEmpty()) {
			return bulkRequest;
		}
		// 循环数据封装bulkRequest
		jsonList.forEach(obj -> {
			Map<String, Object> map = (Map<String, Object>) obj;
			UpdateRequest updateRequest = new UpdateRequest(indexName, map.get(PRIMARY_KEY).toString());
			// 修改索引中不存在就新增
			updateRequest.docAsUpsert(true);
			updateRequest.doc(JacksonUtil.toJsonStr(obj), XContentType.JSON);
			bulkRequest.add(updateRequest);
		});
		return bulkRequest;
	}

	/**
	 * 根据主键查询ES.
	 * @param indexName 索引名称
	 * @param id 主键
	 * @return String
	 */
	public String getIndexById(String indexName, String id) {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，查询索引失败", indexName);
			return null;
		}
		GetRequest getRequest = new GetRequest(indexName, id);
		try {
			GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
			String resultJson = getResponse.getSourceAsString();
			log.info("索引：{}，主键：{}，查询结果：{}", indexName, id, resultJson);
			return resultJson;
		}
		catch (IOException e) {
			log.error("索引：{}，主键：{} -> 查询索引失败，错误信息：{}，详情见日志", indexName, id, LogUtil.result(e.getMessage()), e);
			return null;
		}
	}

	/**
	 * 清空内容.
	 * @param indexName 索引名称
	 */
	public Boolean deleteAll(String indexName) {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，删除索引失败", indexName);
			return false;
		}
		DeleteRequest deleteRequest = new DeleteRequest(indexName);
		deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		try {
			DeleteResponse deleteResponse = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
			if (DocWriteResponse.Result.NOT_FOUND.equals(deleteResponse.getResult())) {
				log.error("索引:：{} -> 删除索引失败", indexName);
				return false;
			}
			log.info("索引：{} -> 删除索引成功", indexName);
		}
		catch (IOException e) {
			log.error("索引：{} -> 删除索引失败，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
		}
		return true;
	}

	/**
	 * 批量数据保存到ES-异步.
	 * @param indexName 索引名称
	 * @param jsonDataList 数据列表
	 */
	public void syncAsyncBatchIndex(String indexName, String jsonDataList) {
		// 判空
		if (StringUtil.isEmpty(jsonDataList)) {
			log.error("数据不能为空，批量异步同步索引失败");
			return;
		}
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，批量异步同步索引失败", indexName);
			return;
		}
		// 批量操作Request
		BulkRequest bulkRequest = packBulkIndexRequest(indexName, jsonDataList);
		if (bulkRequest.requests().isEmpty()) {
			log.error("批量异步同步索引失败");
			return;
		}
		// 异步执行
		ActionListener<BulkResponse> listener = new ActionListener<>() {
			@Override
			public void onResponse(BulkResponse bulkItemResponses) {
				if (bulkItemResponses.hasFailures()) {
					for (BulkItemResponse item : bulkItemResponses.getItems()) {
						log.error("索引：{}，主键：{}，状态：{}，错误信息：{} -> 批量异步同步索引失败", indexName, item.getId(), item.status(),
								item.getFailureMessage());
					}
				}
			}

			// 失败操作
			@Override
			public void onFailure(Exception e) {
				log.error("索引：{} -> 批量异步同步索引成功，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
			}
		};
		restHighLevelClient.bulkAsync(bulkRequest, RequestOptions.DEFAULT, listener);
		log.info("索引：{} -> 批量异步同步索引成功", indexName);
	}

	/**
	 * 删除索引.
	 * @param indexName 索引名称
	 * @throws IOException IOException
	 */
	public void deleteIndex(String indexName) throws IOException {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，删除索引失败", indexName);
			return;
		}
		// 删除操作Request
		DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
		deleteIndexRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
		AcknowledgedResponse acknowledgedResponse = restHighLevelClient.indices()
			.delete(deleteIndexRequest, RequestOptions.DEFAULT);
		if (!acknowledgedResponse.isAcknowledged()) {
			log.error("索引：{} -> 删除索引失败", indexName);
			return;
		}
		log.info("索引：{} -> 删除索引成功", indexName);
	}

	/**
	 * 异步删除索引.
	 * @param indexName 索引名称
	 */
	public void deleteAsyncIndex(String indexName) {
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，异步删除索引失败", indexName);
			return;
		}
		DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
		deleteIndexRequest.indicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);
		ActionListener<AcknowledgedResponse> listener = new ActionListener<>() {
			@Override
			public void onResponse(AcknowledgedResponse acknowledgedResponse) {
				if (acknowledgedResponse.isAcknowledged()) {
					log.info("索引：{} -> 异步删除索引成功", indexName);
				}
				else {
					log.error("索引：{} -> 异步删除索引失败", indexName);
				}
			}

			@Override
			public void onFailure(Exception e) {
				log.error("索引：{} -> 异步删除索引失败，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
			}
		};
		restHighLevelClient.indices().deleteAsync(deleteIndexRequest, RequestOptions.DEFAULT, listener);
	}

	/**
	 * 批量操作的Request.
	 * @param indexName 索引名称
	 * @param jsonDataList json数据列表
	 * @return BulkRequest
	 */
	private BulkRequest packBulkIndexRequest(String indexName, String jsonDataList) {
		BulkRequest bulkRequest = new BulkRequest();
		// IMMEDIATE > 请求向es提交数据，立即进行数据刷新<实时性高，资源消耗高>
		// WAIT_UNTIL > 请求向es提交数据，等待数据完成刷新<实时性高，资源消耗低>
		// NONE > 默认策略<实时性低>
		bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		List<Object> jsonList = JacksonUtil.toList(jsonDataList, Object.class);
		if (jsonList.isEmpty()) {
			return bulkRequest;
		}
		// 循环数据封装bulkRequest
		jsonList.forEach(obj -> {
			Map<String, Object> map = (Map<String, Object>) obj;
			IndexRequest indexRequest = new IndexRequest(indexName);
			indexRequest.source(JacksonUtil.toJsonStr(obj), XContentType.JSON);
			indexRequest.id(map.get(PRIMARY_KEY).toString());
			bulkRequest.add(indexRequest);
		});
		return bulkRequest;
	}

	/**
	 * 创建ES索引.
	 * @param indexName 索引名称
	 * @param indexAlias 别名
	 * @param clazz 类型
	 * @throws IOException IOException
	 */
	public void createIndex(String indexName, String indexAlias, Class<?> clazz) throws IOException {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (indexExists) {
			log.error("索引：{} -> 索引已存在，创建索引失败", indexName);
			return;
		}
		// 创建索引
		boolean createResult = createIndexAndCreateMapping(indexName, indexAlias, FieldMappingUtil.getFieldInfo(clazz));
		if (!createResult) {
			log.info("索引：{} -> 创建索引失败", indexName);
			return;
		}
		log.info("索引：{} -> 创建索引成功", indexName);

	}

	/**
	 * 异步创建ES索引.
	 * @param indexName 索引名称
	 * @param indexAlias 别名
	 * @param clazz 类型
	 * @throws IOException IOException
	 */
	public void createAsyncIndex(String indexName, String indexAlias, Class<?> clazz) throws IOException {
		boolean indexExists = isIndexExists(indexName);
		if (indexExists) {
			log.error("索引：{} -> 索引已存在，异步创建索引失败", indexName);
			return;
		}
		CreateIndexRequest createIndexRequest = getCreateIndexRequest(indexName, indexAlias,
				FieldMappingUtil.getFieldInfo(clazz));
		// 异步方式创建索引
		ActionListener<CreateIndexResponse> listener = new ActionListener<>() {
			@Override
			public void onResponse(CreateIndexResponse createIndexResponse) {
				boolean acknowledged = createIndexResponse.isAcknowledged();
				if (acknowledged) {
					log.info("索引：{} -> 异步创建索引成功", indexName);
				}
				else {
					log.error("索引：{} -> 异步创建索引失败", indexName);
				}
			}

			@Override
			public void onFailure(Exception e) {
				log.error("索引：{} -> 异步创建索引失败，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
			}
		};
		// 异步执行
		restHighLevelClient.indices().createAsync(createIndexRequest, RequestOptions.DEFAULT, listener);
	}

	private CreateIndexRequest getCreateIndexRequest(String indexName, String indexAlias,
			List<FieldMapping> fieldMappingList) throws IOException {
		// 封装es索引的mapping
		XContentBuilder mapping = packEsMapping(fieldMappingList);
		mapping.endObject().endObject();
		mapping.close();
		// 索引创建
		CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
		// 配置分词器
		XContentBuilder settings = packSettingMapping();
		// 别名
		XContentBuilder aliases = packEsAliases(indexAlias);
		createIndexRequest.settings(settings);
		createIndexRequest.mapping(mapping);
		createIndexRequest.aliases(aliases);
		return createIndexRequest;
	}

	/**
	 * 数据同步到ES.
	 * @param id 主键
	 * @param indexName 索引名称
	 * @param jsonData json数据
	 */
	public Boolean syncIndex(String id, String indexName, String jsonData) throws IOException {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，同步索引失败", indexName);
			return false;
		}
		// 创建操作Request
		IndexRequest indexRequest = new IndexRequest(indexName);
		// 配置相关信息
		indexRequest.source(jsonData, XContentType.JSON);
		// IMMEDIATE > 立即刷新
		indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		indexRequest.id(id);
		IndexResponse response = restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
		// 判断索引是新增还是修改
		if (IndexResponse.Result.CREATED.equals(response.getResult())) {
			log.info("索引：{} -> 同步索引成功（新增）", indexName);
		}
		else if (IndexResponse.Result.UPDATED.equals(response.getResult())) {
			log.info("索引：{} -> 同步索引成功（修改）", indexName);
		}
		return true;
	}

	/**
	 * 异步同步.
	 * @param id 编号
	 * @param indexName 索引名称
	 * @param jsonData 数据
	 */
	public void syncIndexAsync(String id, String indexName, String jsonData) {
		// 判断索引是否存在
		boolean indexExists = isIndexExists(indexName);
		if (!indexExists) {
			log.error("索引：{} -> 索引不存在，异步同步索引失败", indexName);
			return;
		}
		// 创建操作Request
		IndexRequest indexRequest = new IndexRequest(indexName);
		// 配置相关信息
		indexRequest.source(jsonData, XContentType.JSON);
		// IMMEDIATE > 立即刷新
		indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
		indexRequest.id(id);
		ActionListener<IndexResponse> actionListener = new ActionListener<>() {
			@Override
			public void onResponse(IndexResponse indexResponse) {
				if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {
					log.info("索引：{} -> 异步同步索引成功（新增）", indexName);
				}
				else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {
					log.info("索引：{} -> 异步同步索引成功（修改）", indexName);
				}
			}

			@Override
			public void onFailure(Exception e) {
				log.error("索引：{} -> 异步同步索引失败，错误信息：{}，详情见日志", indexName, LogUtil.result(e.getMessage()), e);
			}
		};
		restHighLevelClient.indexAsync(indexRequest, RequestOptions.DEFAULT, actionListener);
	}

	/**
	 * 判断索引是否存在.
	 * @param indexName 索引名称
	 * @return Boolean
	 */
	public Boolean isIndexExists(String indexName) {
		try {
			GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
			return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
		}
		catch (Exception e) {
			log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
		}
		return false;
	}

	/**
	 * 创建索引设置相关配置信息.
	 * @param indexName 索引名称
	 * @param indexAlias 索引别名
	 * @param fieldMappingList 数据列表
	 * @return Boolean
	 * @throws IOException IOException
	 */
	private Boolean createIndexAndCreateMapping(String indexName, String indexAlias,
			List<FieldMapping> fieldMappingList) throws IOException {
		// 封装es索引的mapping
		CreateIndexRequest createIndexRequest = getCreateIndexRequest(indexName, indexAlias, fieldMappingList);
		// 同步方式创建索引
		CreateIndexResponse createIndexResponse = restHighLevelClient.indices()
			.create(createIndexRequest, RequestOptions.DEFAULT);
		boolean acknowledged = createIndexResponse.isAcknowledged();
		if (acknowledged) {
			log.info("索引：{} -> 创建索引成功", indexName);
			return true;
		}
		else {
			log.error("索引：{} -> 创建索引失败", indexName);
			return false;
		}
	}

	/**
	 * 配置ES别名.
	 * @author laokou
	 * @param alias 别名
	 * @return XContentBuilder
	 * @throws IOException IOException
	 */
	private XContentBuilder packEsAliases(String alias) throws IOException {
		XContentBuilder aliases = XContentFactory.jsonBuilder()
			.startObject()
			.startObject(alias)
			.field("is_write_index", false)
			.endObject();
		aliases.endObject();
		aliases.close();
		return aliases;
	}

	/**
	 * 配置Mapping.
	 * @param fieldMappingList 组装的实体类信息
	 * @return XContentBuilder
	 * @throws IOException IOException
	 */
	private XContentBuilder packEsMapping(List<FieldMapping> fieldMappingList) throws IOException {
		XContentBuilder mapping = XContentFactory.jsonBuilder()
			.startObject()
			.field("dynamic", true)
			.startObject("properties");
		// 循环实体对象的类型集合封装ES的Mapping
		for (FieldMapping fieldMapping : fieldMappingList) {
			String field = fieldMapping.getField();
			String dataType = fieldMapping.getType();
			Integer participle = fieldMapping.getParticiple();
			// 设置分词规则
			if (EsConstant.NOT_ANALYZED.equals(participle)) {
				if ("date".equals(dataType)) {
					mapping.startObject(field)
						.field("type", dataType)
						.field("format", "yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
						.endObject();
				}
				else {
					mapping.startObject(field).field("type", dataType).endObject();
				}
			}
			else if (EsConstant.IK_INDEX.equals(participle)) {
				mapping.startObject(field)
					.field("type", dataType)
					.field("eager_global_ordinals", true)
					// fielddata=true 用来解决text字段不能进行聚合操作
					.field("fielddata", true)
					.field("analyzer", "ik_pinyin")
					.field("search_analyzer", "ik_smart")
					.endObject();
			}
		}
		return mapping;
	}

	/**
	 * 配置Settings.
	 * @return XContentBuilder
	 * @throws IOException IOException
	 */
	private XContentBuilder packSettingMapping() throws IOException {
		XContentBuilder setting = XContentFactory.jsonBuilder()
			.startObject()
			.startObject("index")
			.field("number_of_shards", 1)
			.field("number_of_replicas", 1)
			.field("refresh_interval", "30s")
			.startObject("analysis");
		// ik分词拼音
		setting.startObject("analyzer")
			.startObject("ik_pinyin")
			.field("tokenizer", "ik_max_word")
			.field("filter", "laokou_pinyin")
			.endObject();
		setting.endObject();
		// 设置拼音分词器分词
		setting.startObject("filter")
			.startObject("laokou_pinyin")
			.field("type", "pinyin")
			// 不会这样划分：刘德华 > [liu,de,hua]
			.field("keep_full_pinyin", false)
			// 这样划分：刘德华 > [liudehua]
			.field("keep_joined_full_pinyin", true)
			// 保留原始中文
			.field("keep_original", true)
			.field("limit_first_letter_length", 16)
			.field("remove_duplicated_term", true)
			.field("none_chinese_pinyin_tokenize", false)
			.endObject()
			.endObject();
		setting.endObject().endObject().endObject();
		setting.close();
		return setting;
	}

	/**
	 * 关键字高亮显示.
	 * @param search 查询实体类
	 * @return 高亮索引列表
	 */
	public Datas<Map<String, Object>> highlightSearchIndex(Search search) {
		try {
			final String[] indexNames = search.getIndexNames();
			// 用于搜索文档，聚合，定制查询有关操作
			SearchRequest searchRequest = new SearchRequest();
			searchRequest.indices(indexNames);
			searchRequest.source(buildSearchSource(search, true, null));
			SearchHits hits = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT).getHits();
			List<Map<String, Object>> data = new ArrayList<>();
			for (SearchHit hit : hits) {
				Map<String, Object> sourceData = hit.getSourceAsMap();
				Map<String, HighlightField> highlightFields = hit.getHighlightFields();
				for (String key : highlightFields.keySet()) {
					sourceData.put(key, highlightFields.get(key).getFragments()[0].string());
				}
				data.add(sourceData);
			}
			Datas<Map<String, Object>> datas = new Datas<>();
			final long total = hits.getTotalHits().value;
			datas.setRecords(data);
			datas.setTotal(total);
			return datas;
		}
		catch (Exception e) {
			throw new SystemException("高亮搜索查询失败");
		}
	}

	/**
	 * 构建query.
	 * @param searchIndex 查询参数
	 * @return BoolQueryBuilder
	 */
	private BoolQueryBuilder buildBoolQuery(Search searchIndex) {
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		// 分词查询
		List<Search.Query> queryStringList = searchIndex.getQueryStringList();
		// or查询
		List<Search.Query> orQueryList = searchIndex.getOrQueryList();
		if (CollectionUtil.isNotEmpty(orQueryList)) {
			// or查询
			BoolQueryBuilder orQuery = QueryBuilders.boolQuery();
			for (Search.Query query : orQueryList) {
				String value = query.getValue();
				if (StringUtil.isEmpty(value)) {
					continue;
				}
				orQuery.should(QueryBuilders.termQuery(query.getField(), value));
			}
			boolQueryBuilder.must(orQuery);
		}
		if (CollectionUtil.isNotEmpty(queryStringList)) {
			// 分词查询
			BoolQueryBuilder analysisQuery = QueryBuilders.boolQuery();
			for (Search.Query query : queryStringList) {
				String field = query.getField();
				String value = query.getValue();
				if (StringUtil.isEmpty(value)) {
					continue;
				}
				// 清除左右空格并处理特殊字符
				String keyword = QueryParser.escape(value.trim());
				analysisQuery.should(QueryBuilders.queryStringQuery(keyword).field(field));
			}
			boolQueryBuilder.must(analysisQuery);
		}
		return boolQueryBuilder;
	}

	/**
	 * 构建搜索.
	 * @param searchIndex 查询参数
	 * @param isHighlightSearchFlag 是否高亮搜索
	 * @param aggregationBuilder 聚合参数
	 * @return SearchSourceBuilder
	 */
	private SearchSourceBuilder buildSearchSource(Search searchIndex, boolean isHighlightSearchFlag,
			TermsAggregationBuilder aggregationBuilder) {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		Integer pageNum = searchIndex.getPageNum();
		Integer pageSize = searchIndex.getPageSize();
		List<Search.Query> sortFieldList = searchIndex.getSortFieldList();
		List<Search.Query> queryStringList = searchIndex.getQueryStringList();
		if (isHighlightSearchFlag && CollectionUtil.isNotEmpty(queryStringList)) {
			List<String> highlightFieldList = queryStringList.stream().map(Search.Query::getField).toList();
			// 高亮显示数据
			HighlightBuilder highlightBuilder = new HighlightBuilder();
			// 设置关键字显示颜色
			highlightBuilder.preTags(HIGHLIGHT_PRE_TAGS);
			highlightBuilder.postTags(HIGHLIGHT_POST_TAGS);
			// 设置显示的关键字
			for (String field : highlightFieldList) {
				highlightBuilder.field(field, 0, 0);
			}
			// 多个字段高亮,这项要为false
			highlightBuilder.requireFieldMatch(false);
			// 设置高亮
			searchSourceBuilder.highlighter(highlightBuilder);
		}
		// 分页
		int pageIndex = (pageNum - 1) * pageSize;
		searchSourceBuilder.from(pageIndex);
		searchSourceBuilder.size(pageSize);
		// 追踪分数开启
		searchSourceBuilder.trackScores(true);
		// 注解
		searchSourceBuilder.explain(true);
		// 匹配度倒排，数值越大匹配度越高
		searchSourceBuilder.sort("_score", SortOrder.DESC);
		// 排序
		if (CollectionUtil.isNotEmpty(sortFieldList)) {
			for (Search.Query query : sortFieldList) {
				SortOrder sortOrder;
				String desc = SortOrder.DESC.toString();
				String value = query.getValue();
				String field = query.getField();
				if (desc.equalsIgnoreCase(value)) {
					sortOrder = SortOrder.DESC;
				}
				else {
					sortOrder = SortOrder.ASC;
				}
				searchSourceBuilder.sort(field, sortOrder);
			}
		}
		searchSourceBuilder.query(buildBoolQuery(searchIndex));
		// 获取真实总数
		searchSourceBuilder.trackTotalHits(true);
		// 聚合对象
		if (ObjectUtil.isNotNull(aggregationBuilder)) {
			searchSourceBuilder.aggregation(aggregationBuilder);
		}
		return searchSourceBuilder;
	}

	/**
	 * 聚合查询.
	 * @param search 搜索
	 * @return SearchVO
	 * @throws IOException IOException
	 */
	public Datas<Map<String, Long>> aggregationSearchIndex(Search search) throws IOException {
		Datas<Map<String, Long>> datas = new Datas<>();
		String[] indexNames = search.getIndexNames();
		Search.Aggregation aggregationKey = search.getAggregationKey();
		String field = aggregationKey.getField();
		String groupKey = aggregationKey.getGroupKey();
		String script = aggregationKey.getScript();
		TermsAggregationBuilder aggregationBuilder;
		if (StringUtil.isNotEmpty(field)) {
			aggregationBuilder = AggregationBuilders.terms(groupKey).field(field).size(100000);
		}
		else {
			aggregationBuilder = AggregationBuilders.terms(groupKey).script(new Script(script)).size(100000);
		}
		// 用于搜索文档，聚合，定制查询有关操作
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.indices(indexNames);
		searchRequest.source(buildSearchSource(search, false, aggregationBuilder));
		SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
		Aggregations aggregations = searchResponse.getAggregations();
		Terms aggregation = aggregations.get(groupKey);
		List<? extends Terms.Bucket> buckets = aggregation.getBuckets();
		List<Map<String, Long>> list = new ArrayList<>(buckets.size());
		for (Terms.Bucket bucket : buckets) {
			list.add(Map.of(bucket.getKeyAsString(), bucket.getDocCount()));
		}
		datas.setRecords(list);
		datas.setTotal(list.size());
		return datas;
	}

	public Map<String, String> getIndexNames(String[] indexAliasNames) {
		try {
			GetAliasesRequest getAliasesRequest = new GetAliasesRequest(indexAliasNames);
			Map<String, Set<AliasMetadata>> aliases = restHighLevelClient.indices()
				.getAlias(getAliasesRequest, RequestOptions.DEFAULT)
				.getAliases();
			Map<String, String> indexMap = new HashMap<>(aliases.size());
			aliases
				.forEach((k, v) -> indexMap.put(k, v.stream().map(AliasMetadata::getAlias).findFirst().orElse(EMPTY)));
			return indexMap;
		}
		catch (Exception e) {
			log.error("获取索引失败，错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
			throw new SystemException("获取索引失败");
		}
	}

	public Map<String, Object> getIndexProperties(String indexName) {
		try {
			GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
			GetIndexResponse getIndexResponse = restHighLevelClient.indices()
				.get(getIndexRequest, RequestOptions.DEFAULT);
			Map<String, Object> indexPropertiesMap = new HashMap<>(2);
			Map<String, Settings> settings = getIndexResponse.getSettings();
			Map<String, MappingMetadata> mappings = getIndexResponse.getMappings();
			indexPropertiesMap.put("mappings", JacksonUtil.toJsonStr(mappings.get(indexName).getSourceAsMap(), true));
			indexPropertiesMap.put("settings", JacksonUtil.toJsonStr(toCO(indexName, settings), true));
			return indexPropertiesMap;
		}
		catch (Exception e) {
			log.error("获取索引属性失败，错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
			throw new SystemException("获取索引属性失败");
		}
	}

	private SettingsCO toCO(String indexName, Map<String, Settings> settings) {
		SettingsCO co = new SettingsCO();
		Settings indexSetting = settings.get(indexName).getAsGroups().get("index");
		String uuid = indexSetting.get("uuid");
		String creation_date = indexSetting.get("creation_date");
		String number_of_replicas = indexSetting.get("number_of_replicas");
		String number_of_shards = indexSetting.get("number_of_shards");
		String provided_name = indexSetting.get("provided_name");
		String refresh_interval = indexSetting.get("refresh_interval");
		String created = indexSetting.get("version.created");
		String _tier_preference = indexSetting.get("routing.allocation.include._tier_preference");
		Map<String, Object> map1 = toMap(indexSetting, "analysis.analyzer");
		Map<String, Object> map2 = toMap(indexSetting, "analysis.filter");
		SettingsCO.Analysis analysis = new SettingsCO.Analysis(map1, map2);
		SettingsCO.Version version = new SettingsCO.Version(created);
		SettingsCO.Include include = new SettingsCO.Include(_tier_preference);
		SettingsCO.Allocation allocation = new SettingsCO.Allocation(include);
		SettingsCO.Routing routing = new SettingsCO.Routing(allocation);
		SettingsCO.Index index = new SettingsCO.Index(uuid, creation_date, number_of_replicas, number_of_shards,
				provided_name, refresh_interval, version, routing, analysis);
		co.setIndex(index);
		return co;
	}

	private Map<String, Object> toMap(Settings indexSetting, String key) {
		Map<String, Settings> settingsMap = indexSetting.getAsSettings(key).getAsGroups();
		if (MapUtil.isEmpty(settingsMap)) {
			return new HashMap<>(0);
		}
		Map<String, Object> map = new HashMap<>(settingsMap.size());
		settingsMap.forEach((k, v) -> map.put(k, JacksonUtil.toMap(v.toString(), String.class, String.class)));
		return map;
	}

}
