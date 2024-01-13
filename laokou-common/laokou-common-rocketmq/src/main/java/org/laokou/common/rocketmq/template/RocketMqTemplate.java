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
package org.laokou.common.rocketmq.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.laokou.common.i18n.common.RocketMqConstants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import static org.apache.rocketmq.client.producer.SendStatus.SEND_OK;
import static org.laokou.common.i18n.common.StringConstants.NULL;
import static org.laokou.common.i18n.common.TraceConstants.TRACE_ID;

/**
 * @author laokou
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RocketMqTemplate implements InitializingBean {

	private final RocketMQTemplate rocketMQTemplate;

	private final ThreadPoolTaskExecutor taskExecutor;

	/**
	 * 同步发送.
	 * @param topic 主题
	 * @param payload 消息
	 * @param timeout 超时时间
	 * @return 发送结果
	 */
	public <T> boolean sendSyncMessage(String topic, T payload, long timeout) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		return rocketMQTemplate.syncSend(topic, message, timeout).getSendStatus().equals(SEND_OK);
	}

	/**
	 * 同步发送.
	 * @param topic 主题
	 * @param payload 消息
	 * @param timeout 超时时间
	 * @return 发送结果
	 */
	public <T> boolean sendSyncMessage(String topic, T payload, long timeout, int delayLevel) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		return rocketMQTemplate.syncSend(topic, message, timeout, delayLevel).getSendStatus().equals(SEND_OK);
	}

	/**
	 * 同步发送消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> boolean sendSyncMessage(String topic, T payload) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		return rocketMQTemplate.syncSend(topic, message).getSendStatus().equals(SEND_OK);
	}

	/**
	 * 异步发送消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> void sendAsyncMessage(String topic, T payload) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
			@Override
			public void onSuccess(SendResult sendResult) {
				log.info("RocketMQ消息发送成功");
			}

			@Override
			public void onException(Throwable throwable) {
				log.error("RocketMQ消息发送失败，报错信息", throwable);
			}
		});
	}

	/**
	 * 异步发送消息.
	 * @param topic 主题
	 * @param tag 标签
	 * @param payload 消息
	 */
	public <T> void sendAsyncMessage(String topic, String tag, T payload) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		sendAsyncMessage(topic, tag, message);
	}

	/**
	 * 异步发送消息.
	 * @param topic 主题
	 * @param tag 标签
	 * @param payload 消息
	 * @param traceId 链路ID
	 */
	public <T> void sendAsyncMessage(String topic, String tag, T payload, String traceId) {
		Message<T> message = MessageBuilder.withPayload(payload).setHeader(TRACE_ID, traceId).build();
		sendAsyncMessage(topic, tag, message);
	}

	/**
	 * 异步发送消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> void sendAsyncMessage(String topic, T payload, long timeout) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		rocketMQTemplate.asyncSend(topic, message, new SendCallback() {
			@Override
			public void onSuccess(SendResult sendResult) {
				log.info("RocketMQ消息发送成功");
			}

			@Override
			public void onException(Throwable throwable) {
				log.error("RocketMQ消息发送失败，报错信息", throwable);
			}
		}, timeout);
	}

	/**
	 * 单向发送消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> void sendOneWayMessage(String topic, T payload) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		// 单向发送，只负责发送消息，不会触发回调函数，即发送消息请求不等待
		// 适用于耗时短，但对可靠性不高的场景，如日志收集
		rocketMQTemplate.sendOneWay(topic, message);
	}

	/**
	 * 延迟消息.
	 * @param topic 主题
	 * @param delay 延迟时间
	 * @param payload 消息
	 */
	public <T> boolean sendDelayMessage(String topic, long delay, T payload) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		return rocketMQTemplate.syncSendDelayTimeSeconds(topic, payload, delay).getSendStatus().equals(SEND_OK);
	}

	/**
	 * 同步发送顺序消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> boolean sendSyncOrderlyMessage(String topic, T payload, String id) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		return rocketMQTemplate.syncSendOrderly(topic, message, id).getSendStatus().equals(SEND_OK);
	}

	/**
	 * 异步发送顺序消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> void sendAsyncOrderlyMessage(String topic, T payload, String id) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		rocketMQTemplate.asyncSendOrderly(topic, message, id, new SendCallback() {
			@Override
			public void onSuccess(SendResult sendResult) {
				log.info("RocketMQ消息发送成功");
			}

			@Override
			public void onException(Throwable throwable) {
				log.error("RocketMQ消息发送失败，报错信息", throwable);
			}
		});
	}

	/**
	 * 单向发送顺序消息.
	 * @param topic 主题
	 * @param payload 消息
	 */
	public <T> void sendOneWayOrderlyMessage(String topic, T payload, String id) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		// 单向发送，只负责发送消息，不会触发回调函数，即发送消息请求不等待
		// 适用于耗时短，但对可靠性不高的场景，如日志收集
		rocketMQTemplate.sendOneWayOrderly(topic, message, id);
	}

	/**
	 * 事务消息.
	 * @param topic 主题
	 * @param payload 消息
	 * @param transactionId 事务ID
	 * @return 发送结果
	 */
	public <T> boolean sendTransactionMessage(String topic, T payload, Long transactionId) {
		Message<T> message = MessageBuilder.withPayload(payload)
			.setHeader(RocketMQHeaders.TRANSACTION_ID, transactionId)
			.build();
		return rocketMQTemplate.sendMessageInTransaction(topic, message, NULL).getSendStatus().equals(SEND_OK);
	}

	/**
	 * 转换并发送.
	 * @param topic 主题
	 * @param payload 消息内容
	 */
	public <T> void convertAndSendMessage(String topic, T payload) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		rocketMQTemplate.convertAndSend(topic, message);
	}

	/**
	 * 发送并接收.
	 */
	public <T> Object sendAndReceiveMessage(String topic, T payload, Class<?> clazz) {
		Message<T> message = MessageBuilder.withPayload(payload).build();
		return rocketMQTemplate.sendAndReceive(topic, message, clazz);
	}

	@Override
	public void afterPropertiesSet() {
		rocketMQTemplate.setAsyncSenderExecutor(taskExecutor.getThreadPoolExecutor());
	}

	private <T> void sendAsyncMessage(String topic, String tag, Message<T> message) {
		rocketMQTemplate.asyncSend(String.format(RocketMqConstants.TOPIC_TAG, topic, tag), message, new SendCallback() {
			@Override
			public void onSuccess(SendResult sendResult) {
				log.info("RocketMQ消息发送成功");
			}

			@Override
			public void onException(Throwable throwable) {
				log.error("RocketMQ消息失败，报错信息", throwable);
			}
		});
	}

}
