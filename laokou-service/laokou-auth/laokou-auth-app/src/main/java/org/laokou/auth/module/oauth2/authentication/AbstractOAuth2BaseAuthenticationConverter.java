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
package org.laokou.auth.module.oauth2.authentication;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.laokou.auth.common.exception.handler.OAuth2ExceptionHandler;
import org.laokou.common.core.utils.MapUtil;
import org.laokou.common.i18n.utils.MessageUtil;
import org.laokou.common.i18n.utils.StringUtil;
import org.laokou.common.i18n.utils.ValidatorUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.laokou.common.i18n.common.ErrorCodes.INVALID_SCOPE;
import static org.laokou.common.i18n.common.StatusCodes.CUSTOM_SERVER_ERROR;
import static org.laokou.common.i18n.common.TenantConstants.TENANT_ID;
import static org.laokou.common.i18n.common.TraceConstants.TRACE_ID;
import static org.laokou.common.i18n.common.ValCodes.OAUTH2_TENANT_ID_REQUIRE;

/**
 * @author laokou
 */
@Slf4j
public abstract class AbstractOAuth2BaseAuthenticationConverter implements AuthenticationConverter {

	/**
	 * 类型.
	 * @return String
	 */
	abstract String getGrantType();

	/**
	 * 子类实现转换.
	 * @param clientPrincipal 认证参数
	 * @param additionalParameters 扩展参数
	 */
	abstract Authentication convert(Authentication clientPrincipal, Map<String, Object> additionalParameters);

	@Override
	public Authentication convert(HttpServletRequest request) {
		// 清空
		ThreadContext.clearMap();
		String traceId = request.getHeader(TRACE_ID);
		if (StringUtil.isNotEmpty(traceId)) {
			ThreadContext.put(TRACE_ID, traceId);
		}
		// 请求链 FilterOrderRegistration
		String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
		if (!getGrantType().equals(grantType)) {
			return null;
		}
		// 判断租户ID是否为空
		String tenantId = request.getParameter(TENANT_ID);
		// log.info("租户ID：{}", tenantId);
		if (StringUtil.isEmpty(tenantId)) {
			throw OAuth2ExceptionHandler.getException(CUSTOM_SERVER_ERROR,
					ValidatorUtil.getMessage(OAUTH2_TENANT_ID_REQUIRE));
		}
		// 构建请求参数集合
		MultiValueMap<String, String> parameters = MapUtil.getParameters(request);
		// 判断scope
		String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
		if (StringUtil.isNotEmpty(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
			throw OAuth2ExceptionHandler.getException(INVALID_SCOPE, MessageUtil.getMessage(INVALID_SCOPE));
		}
		// 获取上下文认证信息
		Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
		Map<String, Object> additionalParameters = new HashMap<>(parameters.size());
		parameters.forEach((key, value) -> {
			if (!key.equals(OAuth2ParameterNames.GRANT_TYPE) && !key.equals(OAuth2ParameterNames.CLIENT_ID)) {
				additionalParameters.put(key, value.getFirst());
			}
		});
		return convert(clientPrincipal, additionalParameters);
	}

}
