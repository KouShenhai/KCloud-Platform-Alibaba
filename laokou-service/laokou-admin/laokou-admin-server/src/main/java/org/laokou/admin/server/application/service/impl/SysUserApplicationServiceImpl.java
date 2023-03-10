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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.laokou.admin.client.vo.UserInfoVO;
import org.laokou.admin.server.application.service.SysUserApplicationService;
import org.laokou.admin.server.domain.sys.entity.SysUserDO;
import org.laokou.admin.server.domain.sys.entity.SysUserRoleDO;
import org.laokou.admin.server.domain.sys.repository.service.SysRoleService;
import org.laokou.admin.server.domain.sys.repository.service.SysUserRoleService;
import org.laokou.admin.server.domain.sys.repository.service.SysUserService;
import org.laokou.common.core.vo.OptionVO;
import org.laokou.admin.server.interfaces.qo.SysUserQo;
import org.laokou.admin.client.vo.SysUserVO;
import org.laokou.admin.client.dto.SysUserDTO;
import org.laokou.auth.client.utils.UserUtil;
import org.laokou.common.core.enums.SuperAdminEnum;
import org.laokou.common.core.utils.StringUtil;
import org.laokou.common.data.filter.annotation.DataFilter;
import org.laokou.common.i18n.core.CustomException;
import org.laokou.auth.client.user.UserDetail;
import org.apache.commons.collections.CollectionUtils;
import org.laokou.common.core.utils.ConvertUtil;
import org.laokou.common.i18n.utils.ValidatorUtil;
import org.laokou.common.jasypt.utils.AESUtil;
import org.laokou.common.jasypt.utils.JasyptUtil;
import org.laokou.common.mybatisplus.utils.BatchUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
/**
 * @author laokou
 */
@Service
@RequiredArgsConstructor
public class SysUserApplicationServiceImpl implements SysUserApplicationService {

    private final SysUserService sysUserService;

    private final SysRoleService sysRoleService;

    private final SysUserRoleService sysUserRoleService;

    private final PasswordEncoder passwordEncoder;

    private final BatchUtil<SysUserRoleDO> batchUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(SysUserDTO dto) {
        ValidatorUtil.validateEntity(dto);
        Long id = dto.getId();
        if (null == id) {
            throw new CustomException("?????????????????????");
        }
        if (CollectionUtils.isEmpty(dto.getRoleIds())) {
            throw new CustomException("?????????????????????????????????????????????");
        }
        if (dto.getDeptId() == null) {
            throw new CustomException("???????????????");
        }
        dto.setEditor(UserUtil.getUserId());
        Integer version = sysUserService.getVersion(id);
        dto.setVersion(version);
        sysUserService.updateUser(dto);
        List<Long> roleIds = dto.getRoleIds();
        //???????????????
        sysUserRoleService.remove(Wrappers.lambdaQuery(SysUserRoleDO.class).eq(SysUserRoleDO::getUserId, dto.getId()));
        if (CollectionUtils.isNotEmpty(roleIds)) {
            saveOrUpdate(dto.getId(),roleIds);
        }
        return true;
    }

    @Override
    public Boolean updatePassword(Long id, String newPassword) {
        Integer version = sysUserService.getVersion(id);
        SysUserDTO dto = new SysUserDTO();
        dto.setEditor(UserUtil.getUserId());
        dto.setId(id);
        dto.setPassword(passwordEncoder.encode(newPassword));
        dto.setVersion(version);
        sysUserService.updateUser(dto);
        return true;
    }

    @Override
    public Boolean updateStatus(Long id, Integer status) {
        Integer version = sysUserService.getVersion(id);
        SysUserDTO dto = new SysUserDTO();
        dto.setEditor(UserUtil.getUserId());
        dto.setId(id);
        dto.setStatus(status);
        dto.setVersion(version);
        sysUserService.updateUser(dto);
        return true;
    }

    @Override
    public Boolean updateInfo(SysUserDTO dto) {
        Long id = dto.getId();
        if (null == id) {
            throw new CustomException("?????????????????????");
        }
        // ??????
        JasyptUtil.setFieldValue(dto);
        // ?????????????????????
        String mobile = dto.getMobile();
        if (StringUtil.isNotEmpty(mobile)) {
            long mobileCount = sysUserService.count(Wrappers.lambdaQuery(SysUserDO.class).eq(SysUserDO::getTenantId,UserUtil.getTenantId()).eq(SysUserDO::getMobile, mobile).ne(SysUserDO::getId, id));
            if (mobileCount > 0) {
                throw new CustomException("???????????????????????????????????????");
            }
        }
        // ??????????????????
        String mail = dto.getMail();
        if (StringUtil.isNotEmpty(mail)) {
            long mailCount = sysUserService.count(Wrappers.lambdaQuery(SysUserDO.class).eq(SysUserDO::getTenantId,UserUtil.getTenantId()).eq(SysUserDO::getMail, mail).ne(SysUserDO::getId, id));
            if (mailCount > 0) {
                throw new CustomException("????????????????????????????????????");
            }
        }
        dto.setEditor(UserUtil.getUserId());
        Integer version = sysUserService.getVersion(id);
        dto.setVersion(version);
        sysUserService.updateUser(dto);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertUser(SysUserDTO dto) {
        ValidatorUtil.validateEntity(dto);
        long count = sysUserService.count(Wrappers.lambdaQuery(SysUserDO.class).eq(SysUserDO::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new CustomException("????????????????????????????????????");
        }
        if (CollectionUtils.isEmpty(dto.getRoleIds())) {
            throw new CustomException("?????????????????????????????????????????????");
        }
        if (dto.getDeptId() == null) {
            throw new CustomException("???????????????");
        }
        if (StringUtil.isEmpty(dto.getPassword())) {
            throw new CustomException("???????????????");
        }
        SysUserDO sysUserDO = ConvertUtil.sourceToTarget(dto, SysUserDO.class);
        sysUserDO.setCreator(UserUtil.getUserId());
        sysUserDO.setTenantId(UserUtil.getTenantId());
        sysUserDO.setPassword(passwordEncoder.encode(dto.getPassword()));
        sysUserService.save(sysUserDO);
        List<Long> roleIds = dto.getRoleIds();
        if (CollectionUtils.isNotEmpty(roleIds)) {
            saveOrUpdate(sysUserDO.getId(),roleIds);
        }
        return true;
    }

    @Override
    @DataFilter(tableAlias = "boot_sys_user")
    public IPage<SysUserVO> queryUserPage(SysUserQo qo) {
        ValidatorUtil.validateEntity(qo);
        qo.setTenantId(UserUtil.getTenantId());
        IPage<SysUserVO> page = new Page<>(qo.getPageNum(),qo.getPageSize());
        IPage<SysUserVO> userPage = sysUserService.getUserPage(page, qo);
        List<SysUserVO> records = userPage.getRecords();
        if (CollectionUtils.isNotEmpty(records)) {
            records.forEach(item -> item.setUsername(AESUtil.decrypt(item.getUsername())));
        }
        return userPage;
    }

    @Override
    public SysUserVO getUserById(Long id) {
        SysUserDO sysUserDO = sysUserService.getById(id);
        SysUserVO sysUserVO = ConvertUtil.sourceToTarget(sysUserDO, SysUserVO.class);
        sysUserVO.setRoleIds(sysRoleService.getRoleIdsByUserId(sysUserVO.getId()));
        return sysUserVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long id) {
        SysUserDO sysUser = sysUserService.getById(id);
        UserDetail userDetail = UserUtil.userDetail();
        if (SuperAdminEnum.YES.ordinal() == sysUser.getSuperAdmin() && SuperAdminEnum.YES.ordinal() != userDetail.getSuperAdmin()) {
            throw new CustomException("?????????????????????????????????");
        }
        sysUserService.deleteUser(id);
        return true;
    }

    @Override
    public List<OptionVO> getOptionList() {
        Long tenantId = UserUtil.getTenantId();
        List<OptionVO> optionList = sysUserService.getOptionList(tenantId);
        if (CollectionUtils.isNotEmpty(optionList)) {
            optionList.forEach(item -> item.setLabel(AESUtil.decrypt(item.getLabel())));
        }
        return optionList;
    }

    @Override
    public UserInfoVO getUserInfo() {
        UserDetail userDetail = UserUtil.userDetail();
        UserInfoVO userInfoVO = ConvertUtil.sourceToTarget(userDetail, UserInfoVO.class);
        // ??????
        JasyptUtil.setFieldValue(userInfoVO);
        return userInfoVO;
    }

    private void saveOrUpdate(Long userId, List<Long> roleIds) {
        List<SysUserRoleDO> doList = new ArrayList<>(roleIds.size());
        if (CollectionUtils.isNotEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                SysUserRoleDO sysUserRoleDO = new SysUserRoleDO();
                sysUserRoleDO.setRoleId(roleId);
                sysUserRoleDO.setUserId(userId);
                doList.add(sysUserRoleDO);
            }
            batchUtil.insertBatch(doList,500,sysUserRoleService);
        }
    }

}
