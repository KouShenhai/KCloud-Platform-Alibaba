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
package org.laokou.common.core.utils;

import java.util.regex.Pattern;

import static org.laokou.common.i18n.common.NetworkConstants.IPV4_REGEX;

/**
 * @author laokou
 */
public class RegexUtil {

	private static final String MAIL_REGEX = "^[A-Za-z0-9-_\\u4e00-\\u9fa5]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

	private static final String MOBILE_REGEX = "^((13[0-9])|(14[5,7,9])|(15[0-3,5-9])|(166)|(17[0-9])|(18[0-9])|(19[1,8,9]))\\d{8}$";

	private static final String SOURCE_REGEX = "^[a-zA-Z]+_+([0-9]+)+$";

	private static final String NUMBER_REGEX = "^[0-9]*$";

	/**
	 * 邮箱验证.
	 * @param mail 邮箱
	 * @return 邮箱匹配结果
	 */
	public static boolean mailRegex(String mail) {
		return Pattern.matches(MAIL_REGEX, mail);
	}

	public static boolean ipRegex(String ip) {
		return Pattern.matches(IPV4_REGEX, ip);
	}

	/**
	 * 数字验证.
	 * @param number 数字
	 * @return 数字匹配结果
	 */
	public static boolean numberRegex(String number) {
		return Pattern.matches(NUMBER_REGEX, number);
	}

	/**
	 * 资源名验证.
	 * @param sourceName 自由名称
	 * @return 资源名匹配结果
	 */
	public static boolean sourceRegex(String sourceName) {
		return Pattern.matches(SOURCE_REGEX, sourceName);
	}

	/**
	 * 手机号验证.
	 * @param mobile 手机号
	 * @return 手机号匹配结果
	 */
	public static boolean mobileRegex(String mobile) {
		return Pattern.matches(MOBILE_REGEX, mobile);
	}

}
