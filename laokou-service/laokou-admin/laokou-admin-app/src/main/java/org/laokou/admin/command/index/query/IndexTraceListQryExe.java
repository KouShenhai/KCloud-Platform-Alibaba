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

package org.laokou.admin.command.index.query;

import lombok.RequiredArgsConstructor;
import org.laokou.admin.dto.index.IndexTraceListQry;
import org.laokou.common.elasticsearch.template.ElasticsearchTemplate;
import org.laokou.common.i18n.dto.Datas;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.i18n.dto.Search;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

import static org.laokou.common.i18n.common.IndexConstants.TRACE;

/**
 * @author laokou
 */
@Component
@RequiredArgsConstructor
public class IndexTraceListQryExe {

	private static final String TRACE_ID = "traceId";

	private final ElasticsearchTemplate elasticsearchTemplate;

	public Result<Datas<Map<String, Object>>> execute(IndexTraceListQry qry) {
		Search search = new Search();
		search.setIndexNames(new String[] { TRACE });
		search.setPageSize(qry.getPageSize());
		search.setPageNum(qry.getPageNum());
		search.setOrQueryList(Collections.singletonList(new Search.Query(TRACE_ID, qry.getTraceId())));
		return Result.of(elasticsearchTemplate.highlightSearchIndex(search));
	}

}
