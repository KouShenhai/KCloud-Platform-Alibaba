/*
 * Copyright (c) 2022-2024 KCloud-Platform-Alibaba Author or Authors. All Rights Reserved.
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

package org.laokou.common.prometheus.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import static org.laokou.common.i18n.common.PropertiesConstants.SPRING_APPLICATION_NAME;
import static org.laokou.common.i18n.common.SysConstants.APPLICATION;

/**
 * @author laokou
 */
@AutoConfiguration
@RequiredArgsConstructor
public class PrometheusAutoConfig {

	private final Environment environment;

	@Bean
	MeterRegistryCustomizer<MeterRegistry> configurer() {
		return (registry) -> registry.config()
			.commonTags(APPLICATION, environment.getProperty(SPRING_APPLICATION_NAME));
	}

}