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

package org.laokou.mqtt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

import static org.laokou.common.i18n.common.PropertiesConstants.MQTT_PREFIX;

/**
 * @author laokou
 */
@Component
@Data
@ConfigurationProperties(prefix = MQTT_PREFIX)
public class SpringMqttProperties {

	private String username;

	private String password;

	private String host;

	private boolean clearStart = true;

	private int receiveMaximum = 5;

	private long maximumPacketSize = 1024;

	private int connectionTimeout = 10;

	private int keepAliveInterval = 15;

	private boolean automaticReconnect = true;

	private boolean manualAcks = true;

	private Set<String> topics;

}
