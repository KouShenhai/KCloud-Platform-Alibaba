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

package org.laokou.flowable.command.definition;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.laokou.common.i18n.common.exception.FlowException;
import org.laokou.common.i18n.common.exception.SystemException;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.i18n.utils.LogUtil;
import org.laokou.common.mybatisplus.utils.TransactionalUtil;
import org.laokou.common.security.utils.UserUtil;
import org.laokou.flowable.dto.definition.DefinitionActivateCmd;
import org.springframework.stereotype.Component;

import static org.laokou.common.i18n.common.DatasourceConstants.FLOWABLE;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefinitionActiveCmdExe {

	private final RepositoryService repositoryService;

	private final TransactionalUtil transactionalUtil;

	public Result<Boolean> execute(DefinitionActivateCmd cmd) {
		try {
			String definitionId = cmd.getDefinitionId();
			DynamicDataSourceContextHolder.push(FLOWABLE);
			ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionTenantId(UserUtil.getTenantId().toString())
				.processDefinitionId(definitionId)
				.singleResult();
			if (definition.isSuspended()) {
				return Result.of(activate(definitionId));
			}
			else {
				throw new FlowException("激活失败，流程已激活");
			}
		}
		finally {
			DynamicDataSourceContextHolder.clear();
		}
	}

	private Boolean activate(String definitionId) {
		return transactionalUtil.defaultExecute(r -> {
			try {
				// 激活
				repositoryService.activateProcessDefinitionById(definitionId, true, null);
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
