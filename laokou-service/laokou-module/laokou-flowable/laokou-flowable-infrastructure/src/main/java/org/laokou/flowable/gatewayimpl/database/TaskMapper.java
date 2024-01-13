/*
 * Copyright (c) 2022 KCloud-Platform-Alibaba Author or Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.flowable.gatewayimpl.database;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.laokou.flowable.gatewayimpl.database.dataobject.TaskDO;
import org.springframework.stereotype.Repository;

import static org.laokou.common.i18n.common.MybatisConstants.TENANT_ID;
import static org.laokou.common.i18n.common.MybatisConstants.USER_ID;

/**
 * @author laokou
 */
@Mapper
@Repository
public interface TaskMapper {

	String getAssigneeByInstanceId(@Param("instanceId") String instanceId, @Param(TENANT_ID) Long tenantId);

	IPage<TaskDO> getTaskList(IPage<TaskDO> page, @Param("key") String key, @Param(USER_ID) Long userId,
			@Param("name") String name, @Param(TENANT_ID) Long tenantId);

}
