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

package org.laokou.gateway.support.ip;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.laokou.common.core.utils.IpUtil;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.nacos.utils.ResponseUtil;
import org.laokou.common.redis.utils.ReactiveRedisUtil;
import org.laokou.common.redis.utils.RedisKeyUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

import static org.laokou.common.i18n.common.BizCodes.IP_WHITE;

/**
 * @author laokou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhiteIp implements Ip {

	private final ReactiveRedisUtil reactiveRedisUtil;

	private final RemoteAddressResolver remoteAddressResolver;

	@Override
	public Mono<Void> validate(ServerWebExchange exchange, GatewayFilterChain chain) {
		InetSocketAddress remoteAddress = remoteAddressResolver.resolve(exchange);
		String hostAddress = remoteAddress.getAddress().getHostAddress();
		if (IpUtil.internalIp(hostAddress)) {
			return chain.filter(exchange);
		}
		String ipCacheHashKey = RedisKeyUtil.getIpCacheHashKey(Label.WHITE.getName());
		return reactiveRedisUtil.hasHashKey(ipCacheHashKey, hostAddress).flatMap(r -> {
			if (Boolean.FALSE.equals(r)) {
				log.error("IP为{}被限制", hostAddress);
				return ResponseUtil.response(exchange, Result.fail(IP_WHITE));
			}
			return chain.filter(exchange);
		});
	}

}
