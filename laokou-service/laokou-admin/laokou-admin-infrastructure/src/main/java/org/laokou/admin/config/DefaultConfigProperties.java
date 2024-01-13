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

package org.laokou.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static org.laokou.common.i18n.common.PropertiesConstants.DEFAULT_CONFIG_PREFIX;

/**
 * @author laokou
 */
@Data
@Component
@ConfigurationProperties(prefix = DEFAULT_CONFIG_PREFIX)
public class DefaultConfigProperties {

	private String definitionKey;

	private Set<String> tenantTables = new HashSet<>(0);

	private Set<String> removeParams = new HashSet<>(0);

	private Set<String> gracefulShutdownServices = new HashSet<>(0);

	private String tenantPrefix;

	private Set<String> domainNames = new HashSet<>(0);

}
