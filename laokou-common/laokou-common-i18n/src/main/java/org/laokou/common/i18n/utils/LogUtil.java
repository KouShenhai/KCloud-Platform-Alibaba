/*
 * Copyright (c) 2022-2024 KCloud-Platform-IOT Author or Authors. All Rights Reserved.
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

package org.laokou.common.i18n.utils;

import static org.laokou.common.i18n.common.StatusCode.INTERNAL_SERVER_ERROR;

/**
 * 日志工具类.
 *
 * @author laokou
 */
public class LogUtil {

	public static String record(String message) {
		return StringUtil.isEmpty(message) ? "暂无信息" : message;
	}

	public static String except(String message) {
		return StringUtil.isEmpty(message) ? MessageUtil.getMessage(INTERNAL_SERVER_ERROR) : message;
	}

}
