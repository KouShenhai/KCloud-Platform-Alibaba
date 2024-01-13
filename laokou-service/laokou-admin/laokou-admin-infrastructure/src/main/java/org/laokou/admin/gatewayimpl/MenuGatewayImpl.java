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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.laokou.admin.convertor.MenuConvertor;
import org.laokou.admin.domain.gateway.MenuGateway;
import org.laokou.admin.domain.menu.Menu;
import org.laokou.common.i18n.common.SuperAdminEnums;
import org.laokou.admin.domain.user.User;
import org.laokou.admin.gatewayimpl.database.MenuMapper;
import org.laokou.admin.gatewayimpl.database.dataobject.MenuDO;
import org.laokou.common.i18n.common.exception.SystemException;
import org.laokou.common.i18n.utils.LogUtil;
import org.laokou.common.mybatisplus.utils.TransactionalUtil;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MenuGatewayImpl implements MenuGateway {

	private final MenuMapper menuMapper;

	private final TransactionalUtil transactionalUtil;

	private final MenuConvertor menuConvertor;

	@Override
	public List<Menu> list(User user, Integer type) {
		return menuConvertor.convertEntityList(getMenuList(type, user));
	}

	@Override
	public Boolean update(Menu menu) {
		MenuDO menuDO = menuConvertor.toDataObject(menu);
		menuDO.setVersion(menuMapper.getVersion(menuDO.getId(), MenuDO.class));
		return updateMenu(menuDO);
	}

	@Override
	public Boolean insert(Menu menu) {
		MenuDO menuDO = menuConvertor.toDataObject(menu);
		return insertMenu(menuDO);
	}

	@Override
	public Boolean deleteById(Long id) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				return menuMapper.deleteById(id) > 0;
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(e.getMessage());
			}
		});
	}

	@Override
	public Menu getById(Long id) {
		return menuConvertor.convertEntity(menuMapper.selectById(id));
	}

	@Override
	public List<Long> getIdsByRoleId(Long roleId) {
		return menuMapper.getMenuIdsByRoleId(roleId);
	}

	@Override
	public List<Menu> list(Menu menu, Long tenantId) {
		List<MenuDO> list = menuMapper.getMenuListLikeName(null, menu.getName());
		return menuConvertor.convertEntityList(list);
	}

	@Override
	public List<Menu> getTenantMenuList() {
		return menuConvertor.convertEntityList(menuMapper.getTenantMenuList());
	}

	private List<MenuDO> getMenuList(Integer type, User user) {
		Long userId = user.getId();
		Integer superAdmin = user.getSuperAdmin();
		if (superAdmin == SuperAdminEnums.YES.ordinal()) {
			return menuMapper.getMenuListLikeName(type, null);
		}
		return menuMapper.getMenuListByUserId(type, userId);
	}

	private Boolean updateMenu(MenuDO menuDO) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				return menuMapper.updateById(menuDO) > 0;
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(e.getMessage());
			}
		});
	}

	private Boolean insertMenu(MenuDO menuDO) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				return menuMapper.insertTable(menuDO);
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(e.getMessage());
			}
		});
	}

}
