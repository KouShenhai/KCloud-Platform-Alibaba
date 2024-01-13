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

package org.laokou.gateway.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.laokou.gateway.repository.NacosRouteDefinitionRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author laokou
 */
@RestController
@Tag(name = "RoutersController", description = "路由管理")
@RequiredArgsConstructor
@RequestMapping("v1/routers")
public class RoutersController {

	private final NacosRouteDefinitionRepository nacosRouteDefinitionRepository;

	@PostMapping
	@Operation(summary = "路由管理", description = "同步路由")
	public Flux<Boolean> sync() {
		return nacosRouteDefinitionRepository.syncRouters();
	}

	@DeleteMapping
	@Operation(summary = "路由管理", description = "删除路由")
	public Mono<Boolean> delete() {
		return nacosRouteDefinitionRepository.deleteRouters();
	}

}
