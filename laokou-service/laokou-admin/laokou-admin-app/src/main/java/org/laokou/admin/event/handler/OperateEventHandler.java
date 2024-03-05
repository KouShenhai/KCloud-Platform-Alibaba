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

package org.laokou.admin.event.handler;

import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import io.micrometer.common.lang.NonNullApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.laokou.admin.domain.gateway.LogGateway;
import org.laokou.common.core.common.event.OperateEvent;
import org.laokou.common.core.utils.JacksonUtil;
import org.laokou.common.domain.listener.AbstractDomainEventRocketMQListener;
import org.laokou.common.domain.service.DomainEventService;
import org.laokou.common.i18n.dto.DecorateDomainEvent;
import org.springframework.stereotype.Component;

import static org.laokou.common.i18n.common.RocketMqConstants.LAOKOU_OPERATE_EVENT_CONSUMER_GROUP;
import static org.laokou.common.i18n.common.RocketMqConstants.LAOKOU_OPERATE_EVENT_TOPIC;

/**
 * 操作日志处理.
 *
 * @author laokou
 */
@Slf4j
@Component
@NonNullApi
@RocketMQMessageListener(consumerGroup = LAOKOU_OPERATE_EVENT_CONSUMER_GROUP, topic = LAOKOU_OPERATE_EVENT_TOPIC)
public class OperateEventHandler extends AbstractDomainEventRocketMQListener {

	private final LogGateway logGateway;

	public OperateEventHandler(DomainEventService domainEventService, LogGateway logGateway) {
		super(domainEventService);
		this.logGateway = logGateway;
	}

	@Override
	protected void handleDomainEvent(DecorateDomainEvent evt, String attribute) {
		try {
			OperateEvent event = JacksonUtil.toBean(attribute, OperateEvent.class);
			DynamicDataSourceContextHolder.push(evt.getSourceName());
			logGateway.create(event, evt);
		}
		finally {
			DynamicDataSourceContextHolder.clear();
		}
	}

}
