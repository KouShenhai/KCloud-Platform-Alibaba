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

package org.laokou.admin.gatewayimpl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jodd.io.ZipUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.laokou.admin.config.DefaultConfigProperties;
import org.laokou.admin.convertor.TenantConvertor;
import org.laokou.admin.domain.annotation.DataFilter;
import org.laokou.admin.domain.gateway.TenantGateway;
import org.laokou.admin.domain.tenant.Tenant;
import org.laokou.common.i18n.common.SuperAdminEnums;
import org.laokou.admin.dto.common.clientobject.OptionCO;
import org.laokou.admin.gatewayimpl.database.MenuMapper;
import org.laokou.admin.gatewayimpl.database.TenantMapper;
import org.laokou.admin.gatewayimpl.database.dataobject.DeptDO;
import org.laokou.admin.gatewayimpl.database.dataobject.MenuDO;
import org.laokou.admin.gatewayimpl.database.dataobject.TenantDO;
import org.laokou.admin.gatewayimpl.database.dataobject.UserDO;
import org.laokou.common.core.utils.*;
import org.laokou.common.i18n.common.NumberConstants;
import org.laokou.common.i18n.common.exception.SystemException;
import org.laokou.common.i18n.dto.Datas;
import org.laokou.common.i18n.dto.PageQuery;
import org.laokou.common.i18n.utils.DateUtil;
import org.laokou.common.i18n.utils.LogUtil;
import org.laokou.common.i18n.utils.ObjectUtil;
import org.laokou.common.i18n.utils.StringUtil;
import org.laokou.common.crypto.utils.AesUtil;
import org.laokou.common.mybatisplus.template.TableTemplate;
import org.laokou.common.mybatisplus.utils.TransactionalUtil;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.laokou.common.i18n.common.DatasourceConstants.*;
import static org.laokou.common.i18n.common.StringConstants.*;
import static org.laokou.common.i18n.common.TenantConstants.*;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class TenantGatewayImpl implements TenantGateway {

	private final TenantMapper tenantMapper;

	private final TransactionalUtil transactionalUtil;

	private final PasswordEncoder passwordEncoder;

	private final MenuMapper menuMapper;

	private final TenantConvertor tenantConvertor;

	private final DefaultConfigProperties defaultConfigProperties;

	private final Environment env;

	@Override
	public Boolean insert(Tenant tenant) {
		TenantDO tenantDO = tenantConvertor.toDataObject(tenant);
		tenantDO.setLabel(defaultConfigProperties.getTenantPrefix() + tenantMapper.maxLabelNum());
		return insertTenant(tenantDO);
	}

	@Override
	@DataFilter(alias = BOOT_SYS_TENANT)
	public Datas<Tenant> list(Tenant tenant, PageQuery pageQuery) {
		IPage<TenantDO> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
		IPage<TenantDO> newPage = tenantMapper.getTenantListFilter(page, tenant.getName(), pageQuery);
		Datas<Tenant> datas = new Datas<>();
		datas.setTotal(newPage.getTotal());
		datas.setRecords(tenantConvertor.convertEntityList(newPage.getRecords()));
		return datas;
	}

	@Override
	public Tenant getById(Long id) {
		return tenantConvertor.convertEntity(tenantMapper.selectById(id));
	}

	@Override
	public Boolean update(Tenant tenant) {
		TenantDO tenantDO = tenantConvertor.toDataObject(tenant);
		tenantDO.setVersion(tenantMapper.getVersion(tenant.getId(), TenantDO.class));
		return updateTenant(tenantDO);
	}

	@Override
	public Boolean deleteById(Long id) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				return tenantMapper.deleteById(id) > 0;
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(e.getMessage());
			}
		});
	}

	@Override
	@SneakyThrows
	public void download(Long id, HttpServletResponse response) {
		String fileName = "kcloud_platform_alibaba_tenant.sql";
		String fileExt = FileUtil.getFileExt(fileName);
		String name = DateUtil.format(DateUtil.now(), DateUtil.YYYYMMDDHHMMSS) + fileExt;
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Content-disposition",
				"attachment;filename=" + StandardCharsets.UTF_8.encode(fileName + ".zip"));
		TenantDO tenantDO = tenantMapper.selectById(id);
		Assert.isTrue(ObjectUtil.isNotNull(tenantDO), "tenantDO is null");
		try (ServletOutputStream outputStream = response.getOutputStream()) {
			File file = writeTempFile(fileName, name, id, tenantDO.getPackageId());
			File zipFile = zipTempFile(file);
			List<File> list = List.of(file, zipFile);
			IOUtils.copy(new FileInputStream(zipFile), outputStream);
			deleteTempFile(list);
		}
	}

	private Boolean insertTenant(TenantDO tenantDO) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				return tenantMapper.insertTable(tenantDO);
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(e.getMessage());
			}
		});
	}

	private Boolean updateTenant(TenantDO tenantDO) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				return tenantMapper.updateById(tenantDO) > 0;
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(e.getMessage());
			}
		});
	}

	@SneakyThrows
	private File writeTempFile(String fileName, String name, long tenantId, long packageId) {
		String tempPath = env.getProperty("file.temp-path");
		File file = FileUtil.createFile(tempPath, name);
		Assert.isTrue(StringUtil.isNotEmpty(tempPath), "tempPath is empty");
		try (InputStream inputStream = ResourceUtil.getResource("scripts/" + fileName).getInputStream();
				FileOutputStream outputStream = new FileOutputStream(file);
				FileChannel outChannel = outputStream.getChannel()) {
			ByteBuffer buffer = ByteBuffer.wrap(inputStream.readAllBytes());
			ByteBuffer buff = ByteBuffer.wrap(CollectionUtil.toStr(getSql(tenantId, packageId), EMPTY).getBytes());
			outChannel.write(buffer);
			outChannel.write(buff);
			buffer.clear();
			buff.clear();
		}
		return file;
	}

	@SneakyThrows
	private File zipTempFile(File file) {
		return ZipUtil.zip(file);
	}

	private void deleteTempFile(List<File> files) {
		if (CollectionUtil.isEmpty(files)) {
			return;
		}
		List<OptionCO> list = files.stream().map(i -> new OptionCO(i.getName(), i.delete() + EMPTY)).toList();
		log.debug("删除结果：{}", StringUtil.collectionToDelimitedString(list, CHINESE_COMMA));
	}

	private List<String> getSql(long tenantId, long packageId) {
		long userId = IdGenerator.defaultSnowflakeId();
		long deptId = IdGenerator.defaultSnowflakeId();
		String deptPath = DEFAULT + COMMA + deptId;
		UserDO user = getUser(tenantId, userId, deptId, deptPath);
		DeptDO dept = getDept(tenantId, userId, deptId, deptPath);
		List<MenuDO> menuList = getMenuList(tenantId, userId, deptId, deptPath, packageId);
		List<Map<String, String>> menuMapList = getMenuMapList(menuList);
		Map<String, String> userMap = JacksonUtil.toMap(user, String.class, String.class);
		Map<String, String> deptMap = JacksonUtil.toMap(dept, String.class, String.class);
		List<String> userSqlList = TableTemplate.getInsertSqlScriptList(Collections.singletonList(userMap),
				BOOT_SYS_USER);
		List<String> deptSqlList = TableTemplate.getInsertSqlScriptList(Collections.singletonList(deptMap),
				BOOT_SYS_DEPT);
		List<String> menuSqlList = TableTemplate.getInsertSqlScriptList(menuMapList, BOOT_SYS_MENU);
		List<String> list = new ArrayList<>(userSqlList.size() + deptSqlList.size() + menuSqlList.size() + 1);
		list.addAll(userSqlList);
		list.addAll(deptSqlList);
		list.addAll(menuSqlList);
		list.add(getUpdateUsernameSql(userId));
		return list;
	}

	private List<Map<String, String>> getMenuMapList(List<MenuDO> menuList) {
		List<Map<String, String>> menuMapList = new ArrayList<>(menuList.size());
		menuList.forEach(item -> menuMapList.add(JacksonUtil.toMap(item, String.class, String.class)));
		return menuMapList;
	}

	private List<MenuDO> getMenuList(long tenantId, long userId, long deptId, String deptPath, long packageId) {
		List<MenuDO> menuList = menuMapper.getTenantMenuListByPackageId(packageId);
		menuList.forEach(item -> {
			item.setTenantId(tenantId);
			item.setCreateDate(DateUtil.now());
			item.setUpdateDate(DateUtil.now());
			item.setCreator(userId);
			item.setEditor(userId);
			item.setVersion(NumberConstants.DEFAULT);
			item.setDelFlag(NumberConstants.DEFAULT);
			item.setDeptId(deptId);
			item.setDeptPath(deptPath);
		});
		return menuList;
	}

	private UserDO getUser(long tenantId, long userId, long deptId, String deptPath) {
		// 初始化超级管理员
		UserDO userDO = new UserDO();
		userDO.setId(userId);
		userDO.setUsername(TENANT_USERNAME);
		userDO.setTenantId(tenantId);
		userDO.setPassword(passwordEncoder.encode(TENANT_PASSWORD));
		userDO.setSuperAdmin(SuperAdminEnums.YES.ordinal());
		userDO.setDeptId(deptId);
		userDO.setDeptPath(deptPath);
		userDO.setCreateDate(DateUtil.now());
		userDO.setUpdateDate(DateUtil.now());
		userDO.setCreator(userId);
		userDO.setEditor(userId);
		userDO.setVersion(NumberConstants.DEFAULT);
		userDO.setDelFlag(NumberConstants.DEFAULT);
		userDO.setStatus(NumberConstants.DEFAULT);
		return userDO;
	}

	private DeptDO getDept(long tenantId, long userId, long deptId, String deptPath) {
		DeptDO deptDO = new DeptDO();
		deptDO.setId(deptId);
		deptDO.setName("多租户集团");
		deptDO.setPath(deptPath);
		deptDO.setSort(1000);
		deptDO.setDeptPath(deptDO.getPath());
		deptDO.setDeptId(deptDO.getId());
		deptDO.setPid(0L);
		deptDO.setTenantId(tenantId);
		deptDO.setCreateDate(DateUtil.now());
		deptDO.setUpdateDate(DateUtil.now());
		deptDO.setCreator(userId);
		deptDO.setEditor(userId);
		deptDO.setVersion(NumberConstants.DEFAULT);
		deptDO.setDelFlag(NumberConstants.DEFAULT);
		return deptDO;
	}

	private String getUpdateUsernameSql(long userId) {
		return String.format(UPDATE_USERNAME_BY_ID_SQL_TEMPLATE, BOOT_SYS_USER, TENANT_USERNAME, AesUtil.getKey(),
				userId);
	}

}
