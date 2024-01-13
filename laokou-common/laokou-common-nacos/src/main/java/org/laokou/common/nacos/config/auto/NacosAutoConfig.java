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

package org.laokou.common.nacos.config.auto;

import com.alibaba.nacos.client.naming.remote.NamingClientProxy;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.laokou.common.nacos.proxy.ProtocolProxy;
import org.laokou.common.nacos.proxy.ProtocolProxyDelegate;
import org.laokou.common.nacos.utils.ServiceUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;

/**
 * @author laokou
 */
@AutoConfiguration
@Slf4j
@RequiredArgsConstructor
public class NacosAutoConfig {

	private final ServiceUtil serviceUtil;

	private final Registration registration;

	/**
	 * 协议代理. {@link NamingClientProxy}
	 * @param serverProperties server配置
	 * @return ProtocolProxy
	 */
	@Bean
	public ProtocolProxy protocolProxy(ServerProperties serverProperties) {
		return new ProtocolProxyDelegate(serverProperties.getSsl().isEnabled());
	}

	@PreDestroy
	public void close() {
		// 服务下线
		log.info("开始执行服务下线");
		serviceUtil.deregisterInstance(registration.getServiceId(), registration.getHost(), registration.getPort());
		log.info("服务下线执行成功");
		log.info("关闭服务执行完毕");
	}

}
