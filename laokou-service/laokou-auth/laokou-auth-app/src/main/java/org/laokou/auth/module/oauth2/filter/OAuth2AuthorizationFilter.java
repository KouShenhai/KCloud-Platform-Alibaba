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

package org.laokou.auth.module.oauth2.filter;

import io.micrometer.common.lang.NonNullApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.laokou.common.core.holder.ShutdownHolder;
import org.laokou.common.core.utils.ResponseUtil;
import org.laokou.common.i18n.utils.MessageUtil;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.laokou.common.i18n.common.StatusCodes.SERVICE_UNAVAILABLE;

/**
 * @author laokou
 */
@Slf4j
@NonNullApi
public class OAuth2AuthorizationFilter extends OncePerRequestFilter {

	@Override
	@SneakyThrows
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
		if (ShutdownHolder.status()) {
			ResponseUtil.response(response, SERVICE_UNAVAILABLE, MessageUtil.getMessage(SERVICE_UNAVAILABLE));
			return;
		}
		chain.doFilter(request, response);
	}

}
