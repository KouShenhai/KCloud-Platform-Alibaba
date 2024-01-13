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
package org.laokou.gateway.config;

import lombok.SneakyThrows;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

import static org.laokou.common.i18n.common.NetworkConstants.HTTPS_SCHEME;

/**
 * API文档配置.
 *
 * @author laokou
 */
@Configuration
public class OpenApiDocConfig {

	@Bean
	@Lazy(false)
	@SneakyThrows
	public List<GroupedOpenApi> openApis(RouteDefinitionLocator locator, ServerProperties serverProperties) {
		List<GroupedOpenApi> groups = new ArrayList<>();
		locator.getRouteDefinitions().filter(routeDefinition -> {
			if (!serverProperties.getSsl().isEnabled() && HTTPS_SCHEME.equals(routeDefinition.getUri().getScheme())) {
				throw new RuntimeException(
						String.format("HTTP不允许开启SSL，请检查URL为%s的路由", routeDefinition.getUri().toString()));
			}
			return routeDefinition.getId().matches("laokou-.*");
		}).subscribeOn(Schedulers.boundedElastic()).subscribe(routeDefinition -> {
			String name = routeDefinition.getId().substring(7);
			GroupedOpenApi.builder().pathsToMatch("/".concat(name).concat("/**")).group(name).build();
		});
		return groups;
	}

}
