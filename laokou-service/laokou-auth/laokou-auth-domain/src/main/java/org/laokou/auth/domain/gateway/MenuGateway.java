/*
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
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

package org.laokou.auth.domain.gateway;

import java.util.List;

/**
 * @author laokou
 */
public interface MenuGateway {

	/**
	 * 查询权限标识
	 * @param userId 用户ID
	 * @param tenantId 租户ID
	 * @param superAdmin 是否是超级管理员
	 * @return List<String>
	 */
	List<String> getPermissions(Long userId, Long tenantId, Integer superAdmin);

}
