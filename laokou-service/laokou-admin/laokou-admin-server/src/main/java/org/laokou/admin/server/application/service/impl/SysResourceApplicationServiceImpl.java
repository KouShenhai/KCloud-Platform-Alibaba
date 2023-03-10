/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.admin.server.application.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.laokou.admin.client.dto.MessageDTO;
import org.laokou.admin.client.enums.AuditEnum;
import org.laokou.admin.client.enums.AuditStatusEnum;
import org.laokou.admin.server.application.service.SysMessageApplicationService;
import org.laokou.admin.server.application.service.SysResourceApplicationService;
import org.laokou.admin.server.domain.sys.entity.SysResourceAuditDO;
import org.laokou.admin.server.domain.sys.entity.SysResourceDO;
import org.laokou.admin.server.domain.sys.repository.service.*;
import org.laokou.admin.server.infrastructure.feign.elasticsearch.ElasticsearchApiFeignClient;
import org.laokou.admin.server.infrastructure.feign.flowable.WorkTaskApiFeignClient;
import org.laokou.admin.server.infrastructure.feign.oss.OssApiFeignClient;
import org.laokou.admin.server.interfaces.qo.TaskQo;
import org.laokou.common.core.utils.*;
import org.laokou.admin.client.enums.MessageTypeEnum;
import org.laokou.admin.client.dto.SysResourceAuditDTO;
import org.laokou.admin.server.interfaces.qo.SysResourceQo;
import org.laokou.admin.client.vo.SysResourceVO;
import org.laokou.auth.client.utils.UserUtil;
import org.laokou.common.jasypt.utils.AESUtil;
import org.laokou.common.log.dto.AuditLogDTO;
import org.laokou.common.log.service.SysAuditLogService;
import org.laokou.common.log.vo.SysAuditLogVO;
import org.laokou.common.i18n.core.CustomException;
import org.laokou.common.i18n.core.HttpResult;
import org.laokou.common.i18n.utils.ValidatorUtil;
import org.laokou.elasticsearch.client.dto.CreateIndexDTO;
import org.laokou.elasticsearch.client.dto.ElasticsearchDTO;
import org.laokou.elasticsearch.client.index.ResourceIndex;
import org.laokou.elasticsearch.client.utils.ElasticsearchFieldUtil;
import org.laokou.flowable.client.dto.*;
import org.laokou.flowable.client.vo.AssigneeVO;
import org.laokou.flowable.client.vo.PageVO;
import org.laokou.flowable.client.vo.TaskVO;
import org.laokou.admin.client.enums.AuditTypeEnum;
import org.laokou.redis.utils.RedisUtil;
import org.laokou.oss.client.vo.UploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import static org.laokou.common.core.constant.Constant.DEFAULT;

/**
 * @author laokou
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SysResourceApplicationServiceImpl implements SysResourceApplicationService {
    private static final String PROCESS_KEY = "Process_88888888";
    private static final String AUDIT_STATUS = "auditStatus";
    private final SysResourceService sysResourceService;
    private final SysAuditLogService sysAuditLogService;
    private final ElasticsearchApiFeignClient elasticsearchApiFeignClient;
    private final SysMessageApplicationService sysMessageApplicationService;
    private final WorkTaskApiFeignClient workTaskApiFeignClient;
    private final OssApiFeignClient ossApiFeignClient;
    private final RedisUtil redisUtil;
    private final SysResourceAuditService sysResourceAuditService;
    @Override
    public IPage<SysResourceVO> queryResourcePage(SysResourceQo qo) {
        ValidatorUtil.validateEntity(qo);
        IPage<SysResourceVO> page = new Page<>(qo.getPageNum(), qo.getPageSize());
        return sysResourceService.getResourceList(page,qo);
    }

    @Override
    public Boolean syncResource(String code,String key) {
        long resourceTotal = sysResourceService.getResourceTotal(code);
        if (resourceTotal == 0) {
            throw new CustomException("?????????????????????????????????");
        }
        // ???????????????????????????????????????
        Object obj = redisUtil.get(key);
        if (obj != null) {
            throw new CustomException("?????????????????????????????????");
        }
        String indexAlias = ElasticsearchFieldUtil.RESOURCE_INDEX;
        String indexName = indexAlias + "_" + code;
        try {
            // ????????????
            deleteResourceIndex(indexName);
            // ????????????
            createResourceIndex(indexAlias, indexName);
            // ????????????
            syncResourceIndex(code, indexAlias, indexName);
        } catch (CustomException e) {
            throw e;
        }
        // ??????redis
        redisUtil.set(key, DEFAULT,RedisUtil.HOUR_ONE_EXPIRE);
        return true;
    }

    @Override
    public SysResourceVO getResourceById(Long id) {
        return sysResourceService.getResourceById(id);
    }

    @Override
    public void downLoadResource(Long id, HttpServletResponse response) throws IOException {
        SysResourceVO resource = sysResourceService.getResourceById(id);
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + System.currentTimeMillis() + FileUtil.getFileSuffix(resource.getUrl()));
        InputStream inputStream = FileUtil.getInputStream(resource.getUrl());
        ServletOutputStream outputStream = response.getOutputStream();
        IOUtils.write(inputStream.readAllBytes(),outputStream);
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    @Override
    public SysResourceVO getResourceAuditByResourceId(Long id) {
        return sysResourceService.getResourceAuditByResourceId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    @GlobalTransactional
    public Boolean insertResource(SysResourceAuditDTO dto) {
        ValidatorUtil.validateEntity(dto);
        log.info("??????????????? XID:{}", RootContext.getXID());
        SysResourceDO sysResourceDO = ConvertUtil.sourceToTarget(dto, SysResourceDO.class);
        sysResourceDO.setEditor(UserUtil.getUserId());
        sysResourceService.save(sysResourceDO);
        Long id = sysResourceDO.getId();
        // ????????????
        String instanceId = startTask(id, sysResourceDO.getTitle());
        dto.setResourceId(id);
        return insertResourceAudit(dto,instanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
    @GlobalTransactional
    public Boolean updateResource(SysResourceAuditDTO dto) {
        ValidatorUtil.validateEntity(dto);
        log.info("??????????????? XID:{}", RootContext.getXID());
        Long resourceId = dto.getResourceId();
        if (resourceId == null) {
            throw new CustomException("?????????????????????");
        }
        // ????????????
        String instanceId = startTask(resourceId, dto.getTitle());
        return insertResourceAudit(dto,instanceId);
    }

    /**
     * ?????????????????????
     * @param dto
     * @param instanceId
     * @return
     */
    private Boolean insertResourceAudit(SysResourceAuditDTO dto,String instanceId) {
        SysResourceAuditDO sysResourceAuditDO = ConvertUtil.sourceToTarget(dto, SysResourceAuditDO.class);
        sysResourceAuditDO.setCreator(UserUtil.getUserId());
        sysResourceAuditDO.setStatus(AuditStatusEnum.INIT.ordinal());
        sysResourceAuditDO.setProcessInstanceId(instanceId);
        return sysResourceAuditService.save(sysResourceAuditDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteResource(Long id) {
        sysResourceService.deleteResource(id);
        return true;
    }

    @Override
    public UploadVO uploadResource(String code, MultipartFile file,String md5) {
        if (file.isEmpty()) {
            throw new CustomException("???????????????????????????");
        }
        // ????????????
        String fileName = file.getOriginalFilename();
        String fileSuffix = FileUtil.getFileSuffix(fileName);
        if (!FileUtil.checkFileExt(code,fileSuffix)) {
            throw new CustomException("???????????????????????????????????????");
        }
        HttpResult<UploadVO> result = ossApiFeignClient.upload(file,md5);
        if (!result.success()) {
            throw new CustomException(result.getCode(), result.getMsg());
        }
        return result.getData();
    }

    private void syncResourceIndex(String code, String indexAlias, String indexName) {
        beforeSync();
        // https://mybatis.org/mybatis-3/zh/sqlmap-xml.html
        // FORWARD_ONLY ??????????????????
        // ????????????
        int chunkSize = 500;
        List<ResourceIndex> list = Collections.synchronizedList(new ArrayList<>(chunkSize));
        sysResourceService.handleResourceList(code, resultContext -> {
            ResourceIndex resultObject = resultContext.getResultObject();
            list.add(resultObject);
            if (list.size() % chunkSize == 0) {
                syncIndex(list,indexName,indexAlias);
            }
        });
        if (list.size() % chunkSize != 0) {
            syncIndex(list,indexName,indexAlias);
        }
        afterSync();
    }

    /**
     * ????????????
     * @param list ????????????
     * @param indexName ????????????
     * @param indexAlias ????????????
     */
    private void syncIndex(List<ResourceIndex> list,String indexName,String indexAlias) {
        ElasticsearchDTO dto = new ElasticsearchDTO();
        dto.setData(JacksonUtil.toJsonStr(list));
        dto.setIndexAlias(indexAlias);
        dto.setIndexName(indexName);
        HttpResult<Boolean> result = elasticsearchApiFeignClient.syncBatch(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        // ??????list
        list.clear();
    }

    @Override
    public List<SysAuditLogVO>   queryAuditLogList(Long businessId) {
        return sysAuditLogService.getAuditLogList(businessId,AuditTypeEnum.RESOURCE.ordinal());
    }

    private void beforeCreateIndex() {
        log.info("??????????????????...");
    }

    private void afterCreateIndex() {
        log.info("??????????????????...");
    }

    private void createResourceIndex(String indexAlias, String indexName) {
        beforeCreateIndex();
        final CreateIndexDTO dto = new CreateIndexDTO();
        dto.setIndexName(indexName);
        dto.setIndexAlias(indexAlias);
        HttpResult<Boolean> result = elasticsearchApiFeignClient.create(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        afterCreateIndex();
    }

    private void deleteResourceIndex( String resourceIndex) {
        beforeDeleteIndex();
        HttpResult<Boolean> result = elasticsearchApiFeignClient.delete(resourceIndex);
        if (!result.success()) {
            throw new CustomException(result.getMsg());
        }
        afterDeleteIndex();
    }

    private void beforeDeleteIndex() {
        log.info("??????????????????...");
    }

    private void afterDeleteIndex() {
        log.info("??????????????????...");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional
    public Boolean auditResourceTask(AuditDTO dto) {
        ValidatorUtil.validateEntity(dto);
        log.info("??????????????? XID:{}", RootContext.getXID());
        HttpResult<AssigneeVO> result = workTaskApiFeignClient.audit(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        // ????????????
        AssigneeVO vo = result.getData();
        String assignee = vo.getAssignee();
        String instanceId = vo.getInstanceId();
        Map<String, Object> values = dto.getValues();
        String instanceName = dto.getInstanceName();
        String businessKey = dto.getBusinessKey();
        Long businessId = Long.valueOf(businessKey);
        String comment = dto.getComment();
        String username = AESUtil.decrypt(UserUtil.getUsername());
        Long userId = UserUtil.getUserId();
        int auditStatus = Integer.valueOf(values.get(AUDIT_STATUS).toString());
        int status;
        //1 ????????? 2 ???????????? 3????????????
        if (StringUtil.isNotEmpty(assignee)) {
            // ?????????
            status = AuditStatusEnum.AUDIT.ordinal();
        } else {
            // auditStatus => 0?????? 1??????
            if (AuditEnum.NO.ordinal() == auditStatus) {
                //????????????
                status = AuditStatusEnum.REJECT.ordinal();
            } else {
                // ????????????
                status = AuditStatusEnum.AGREE.ordinal();
            }
        }
        switch (AuditStatusEnum.getStatus(status)) {
            case AUDIT -> // ?????????,????????????????????????
                    insertAuditMessage(assignee,businessId,instanceName);
            case AGREE -> // ????????????
                    auditAgree(businessId,instanceId);
            case REJECT -> {
                // ????????????
            }
            default -> {}
        }
        // ??????????????????
        updateAuditStatus(status,instanceId);
        // ????????????
        insertAuditLog(businessId,auditStatus,comment,username,userId);
        return true;
    }

    /**
     * ????????????
     * @param businessId
     */
    private void auditAgree(Long businessId,String instanceId) {
        // ??????????????????????????????????????????
        LambdaQueryWrapper<SysResourceAuditDO> queryWrapper = Wrappers.lambdaQuery(SysResourceAuditDO.class).eq(SysResourceAuditDO::getProcessInstanceId, instanceId)
                .eq(SysResourceAuditDO::getResourceId,businessId)
                .select(SysResourceAuditDO::getUrl
                        , SysResourceAuditDO::getTitle
                        , SysResourceAuditDO::getRemark);
        SysResourceAuditDO auditDO = sysResourceAuditService.getOne(queryWrapper);
        Integer version = sysResourceService.getVersion(businessId);
        LambdaUpdateWrapper<SysResourceDO> updateWrapper = Wrappers.lambdaUpdate(SysResourceDO.class).eq(SysResourceDO::getId, businessId)
                .eq(SysResourceDO::getVersion,version)
                .set(SysResourceDO::getVersion, version + 1)
                .set(SysResourceDO::getTitle, auditDO.getTitle())
                .set(SysResourceDO::getRemark, auditDO.getRemark())
                .set(SysResourceDO::getUrl, auditDO.getUrl());
        sysResourceService.update(updateWrapper);
    }

    /**
     * ??????????????????
     * @param status
     * @param instanceId
     */
    private void updateAuditStatus(int status,String instanceId) {
        // ????????????
        Integer version = sysResourceAuditService.getVersion(instanceId);
        LambdaUpdateWrapper<SysResourceAuditDO> updateWrapper = Wrappers.lambdaUpdate(SysResourceAuditDO.class)
                .set(SysResourceAuditDO::getStatus, status)
                .set(SysResourceAuditDO::getVersion, version + 1)
                .eq(SysResourceAuditDO::getVersion,version)
                .eq(SysResourceAuditDO::getProcessInstanceId, instanceId);
        sysResourceAuditService.update(updateWrapper);
    }

    private void insertAuditLog(Long businessId,int auditStatus,String comment,String username,Long userId) {
        AuditLogDTO auditLogDTO = new AuditLogDTO();
        auditLogDTO.setBusinessId(businessId);
        auditLogDTO.setAuditStatus(auditStatus);
        auditLogDTO.setAuditDate(new Date());
        auditLogDTO.setAuditName(username);
        auditLogDTO.setCreator(userId);
        auditLogDTO.setComment(comment);
        auditLogDTO.setType(AuditTypeEnum.RESOURCE.ordinal());
        sysAuditLogService.insertAuditLog(auditLogDTO);
    }

    @Override
    public IPage<TaskVO> queryResourceTask(TaskQo qo) {
        IPage<TaskVO> page = new Page<>();
        TaskDTO dto = new TaskDTO();
        dto.setPageNum(qo.getPageNum());
        dto.setPageSize(qo.getPageSize());
        dto.setUsername(AESUtil.decrypt(UserUtil.getUsername()));
        dto.setUserId(UserUtil.getUserId());
        dto.setProcessName(qo.getProcessName());
        dto.setProcessKey(PROCESS_KEY);
        HttpResult<PageVO<TaskVO>> result = workTaskApiFeignClient.query(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        PageVO<TaskVO> taskVOPageVO = Optional.ofNullable(result.getData()).orElseGet(PageVO::new);
        page.setRecords(taskVOPageVO.getRecords());
        page.setSize(dto.getPageSize());
        page.setCurrent(dto.getPageNum());
        page.setTotal(Optional.ofNullable(taskVOPageVO.getTotal()).orElse(0L));
        return page;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional
    public Boolean resolveResourceTask(ResolveDTO dto) {
        HttpResult<AssigneeVO> result = workTaskApiFeignClient.resolve(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        // ????????????
        AssigneeVO vo = result.getData();
        String assignee = vo.getAssignee();
        Long businessId = Long.valueOf(dto.getBusinessKey());
        String instanceName = dto.getInstanceName();
        insertAuditMessage(assignee,businessId,instanceName);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional
    public Boolean transferResourceTask(TransferDTO dto) {
        HttpResult<AssigneeVO> result = workTaskApiFeignClient.transfer(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        // ????????????
        AssigneeVO vo = result.getData();
        String assignee = vo.getAssignee();
        Long businessId = Long.valueOf(dto.getBusinessKey());
        String instanceName = dto.getInstanceName();
        insertAuditMessage(assignee,businessId,instanceName);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @GlobalTransactional
    public Boolean delegateResourceTask(DelegateDTO dto) {
        HttpResult<AssigneeVO> result = workTaskApiFeignClient.delegate(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        // ????????????
        AssigneeVO vo = result.getData();
        String assignee = vo.getAssignee();
        Long businessId = Long.valueOf(dto.getBusinessKey());
        String instanceName = dto.getInstanceName();
        insertResolveMessage(assignee,businessId,instanceName);
        return true;
    }

    private void beforeSync() {
        log.info("??????????????????...");
    }

    private void afterSync() {
        log.info("??????????????????...");
    }

    private void insertMessage(String assignee,String title,String content) {
        Set<String> set = new HashSet<>(1);
        set.add(assignee);
        MessageDTO dto = new MessageDTO();
        dto.setContent(content);
        dto.setTitle(title);
        dto.setReceiver(set);
        dto.setType(MessageTypeEnum.REMIND.ordinal());
        sysMessageApplicationService.insertMessage(dto);
    }

   private void insertAuditMessage(String assignee,Long id,String name) {
        String title = "????????????????????????";
        String content = String.format("?????????%s????????????%s????????????????????????????????????????????????",id,name);
        insertMessage(assignee,title,content);
   }

   private void insertResolveMessage(String assignee,Long id,String name) {
        String title = "????????????????????????";
        String content = String.format("?????????%s????????????%s????????????????????????????????????????????????",id,name);
        insertMessage(assignee,title,content);
   }

    /**
     * ????????????
     * @param businessKey ????????????
     * @param businessName ????????????
     * @return ??????????????????
     */
    private String startTask(Long businessKey,String businessName) {
        ProcessDTO dto = new ProcessDTO();
        dto.setBusinessKey(businessKey.toString());
        dto.setBusinessName(businessName);
        dto.setProcessKey(PROCESS_KEY);
        HttpResult<AssigneeVO> result = workTaskApiFeignClient.start(dto);
        if (!result.success()) {
            throw new CustomException(result.getCode(),result.getMsg());
        }
        AssigneeVO vo = result.getData();
        String instanceId = vo.getInstanceId();
        String assignee = vo.getAssignee();
        insertAuditMessage(assignee,businessKey,businessName);
        return instanceId;
    }

}
