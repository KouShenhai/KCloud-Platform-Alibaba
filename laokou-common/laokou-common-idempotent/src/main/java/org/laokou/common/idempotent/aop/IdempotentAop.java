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

package org.laokou.common.idempotent.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.laokou.common.core.utils.RequestUtil;
import org.laokou.common.core.utils.ResourceUtil;
import org.laokou.common.i18n.common.exception.ApiException;
import org.laokou.common.i18n.utils.StringUtil;
import org.laokou.common.idempotent.utils.IdempotentUtil;
import org.laokou.common.redis.utils.RedisKeyUtil;
import org.laokou.common.redis.utils.RedisUtil;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.laokou.common.i18n.common.TraceConstants.REQUEST_ID;

/**
 * @author laokou
 */
@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentAop {

	private final RedisUtil redisUtil;

	private static final DefaultRedisScript<Boolean> REDIS_SCRIPT;

	static {
		try (InputStream inputStream = ResourceUtil.getResource("META-INF/scripts/idempotent.lua").getInputStream()) {
			REDIS_SCRIPT = new DefaultRedisScript<>(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
					Boolean.class);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Before("@annotation(org.laokou.common.idempotent.annotation.Idempotent)")
	public void doBefore() {
		String requestId = getRequestId();
		if (StringUtil.isEmpty(requestId)) {
			throw new ApiException("提交失败，令牌不能为空");
		}
		String apiIdempotentKey = RedisKeyUtil.getApiIdempotentKey(requestId);
		Boolean result = redisUtil.execute(REDIS_SCRIPT, Collections.singletonList(apiIdempotentKey));
		if (!result) {
			throw new ApiException("不可重复提交请求");
		}
		IdempotentUtil.openIdempotent();
	}

	@After("@annotation(org.laokou.common.idempotent.annotation.Idempotent)")
	public void doAfter() {
		IdempotentUtil.cleanIdempotent();
	}

	private String getRequestId() {
		HttpServletRequest request = RequestUtil.getHttpServletRequest();
		String requestId = request.getHeader(REQUEST_ID);
		if (StringUtil.isEmpty(requestId)) {
			return request.getParameter(REQUEST_ID);
		}
		return requestId;
	}

}
