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

package org.laokou.flowable.command.task;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.laokou.common.i18n.utils.LogUtil;
import org.laokou.common.i18n.utils.ObjectUtil;
import org.laokou.common.i18n.common.exception.FlowException;
import org.laokou.common.i18n.common.exception.SystemException;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.mybatisplus.utils.TransactionalUtil;
import org.laokou.common.security.utils.UserUtil;
import org.laokou.flowable.dto.task.TaskTransferCmd;
import org.springframework.stereotype.Component;

import static org.laokou.common.i18n.common.DatasourceConstants.FLOWABLE;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTransferCmdExe {

	private final TaskService taskService;

	private final TransactionalUtil transactionalUtil;

	public Result<Boolean> execute(TaskTransferCmd cmd) {
		try {
			log.info("转办流程分布式事务 XID：{}", RootContext.getXID());
			String taskId = cmd.getTaskId();
			String owner = cmd.getUserId().toString();
			String assignee = cmd.getToUserId().toString();
			DynamicDataSourceContextHolder.push(FLOWABLE);
			Task task = taskService.createTaskQuery()
				.taskTenantId(UserUtil.getTenantId().toString())
				.taskId(taskId)
				.singleResult();
			if (ObjectUtil.isNull(task)) {
				throw new FlowException("任务不存在");
			}
			if (!owner.equals(task.getAssignee())) {
				throw new FlowException("用户无权操作任务");
			}
			return Result.of(transfer(taskId, owner, assignee));
		}
		finally {
			DynamicDataSourceContextHolder.clear();
		}
	}

	private Boolean transfer(String taskId, String owner, String assignee) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				taskService.setOwner(taskId, owner);
				taskService.setAssignee(taskId, assignee);
				return true;
			}
			catch (Exception e) {
				log.error("错误信息：{}，详情见日志", LogUtil.result(e.getMessage()), e);
				r.setRollbackOnly();
				throw new SystemException(LogUtil.fail(e.getMessage()));
			}
		});
	}

}
