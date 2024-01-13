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
package org.laokou.common.i18n.utils;

import org.laokou.common.i18n.common.exception.SystemException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * 日期处理.
 *
 * @author laokou
 */
public class DateUtil {

	/**
	 * yyyy-MM-dd HH:mm:ss.
	 */
	public static final int YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS = 0;

	/**
	 * yyyyMMddHHmmss.
	 */
	public static final int YYYYMMDDHHMMSS = 1;

	/**
	 * yyyyMM.
	 */
	public static final int YYYYMM = 2;

	/**
	 * yyyy-MM-dd.
	 */
	public static final int YYYY_ROD_MM_ROD_DD = 3;

	/**
	 * yyyy年MM月dd日.
	 */
	public static final int YYYY_TEXT_MM_TEXT_DD_TEXT = 4;

	/**
	 * yyyy.MM.dd.
	 */
	public static final int YYYY_DOT_MM_DOT_DD = 5;

	/**
	 * 星期一.
	 */
	public static final int MONDAY = 0;

	/**
	 * 时间格式.
	 */
	private static final String[] TIME_PATTERNS = { Constant.YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS,
			Constant.YYYYMMDDHHMMSS, Constant.YYYYMM, Constant.YYYY_ROD_MM_ROD_DD, Constant.YYYY_TEXT_MM_TEXT_DD_TEXT,
			Constant.YYYY_DOT_MM_DOT_DD };

	/**
	 * 星期数组.
	 */
	private static final DayOfWeek[] WEEK_PATTERNS = { DayOfWeek.MONDAY };

	public static String getTimePattern(int index) {
		if (index >= TIME_PATTERNS.length || index < 0) {
			throw new SystemException("时间格式不存在");
		}
		return TIME_PATTERNS[index];
	}

	public static DayOfWeek getWeekPattern(int index) {
		if (index >= WEEK_PATTERNS.length || index < 0) {
			throw new SystemException("星期格式不存在");
		}
		return WEEK_PATTERNS[index];
	}

	public static String format(LocalDateTime localDateTime, int index) {
		DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(index);
		return localDateTime.format(dateTimeFormatter);
	}

	public static String format(LocalDate localDate, int index) {
		DateTimeFormatter dateTimeFormatter = getDateTimeFormatter(index);
		return localDate.format(dateTimeFormatter);
	}

	public static DateTimeFormatter getDateTimeFormatter(int index) {
		String timePattern = getTimePattern(index);
		return DateTimeFormatter.ofPattern(timePattern);
	}

	public static boolean isAfter(LocalDateTime localDateTime1, LocalDateTime localDateTime2) {
		return localDateTime1.isAfter(localDateTime2);
	}

	public static boolean isBefore(LocalDateTime localDateTime1, LocalDateTime localDateTime2) {
		return localDateTime1.isBefore(localDateTime2);
	}

	public static boolean isBefore(LocalDate localDate1, LocalDate localDate2) {
		return localDate1.isBefore(localDate2);
	}

	/**
	 * 转换.
	 * @param dateTime 时间
	 * @param index 索引
	 * @return LocalDateTime
	 */
	public static LocalDateTime parseTime(String dateTime, int index) {
		String timePattern = getTimePattern(index);
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timePattern);
		return LocalDateTime.parse(dateTime, dateTimeFormatter);
	}

	/**
	 * 转换.
	 * @param dateTime 日期
	 * @param index 索引
	 * @return LocalDateTime
	 */
	public static LocalDate parseDate(String dateTime, int index) {
		String timePattern = getTimePattern(index);
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timePattern);
		return LocalDate.parse(dateTime, dateTimeFormatter);
	}

	/**
	 * 获取 前/后 x天 的时间.
	 * @param localDateTime 时间
	 * @param days 天
	 * @return LocalDateTime
	 */
	public static LocalDateTime plusDays(LocalDateTime localDateTime, long days) {
		return localDateTime.plusDays(days);
	}

	public static LocalDate plusDays(LocalDate localDate, long days) {
		return localDate.plusDays(days);
	}

	/**
	 * 获取 前/后 x秒 的时间.
	 * @param localDateTime 时间
	 * @param seconds 秒
	 * @return LocalDateTime
	 */
	public static LocalDateTime plusSeconds(LocalDateTime localDateTime, long seconds) {
		return localDateTime.plusSeconds(seconds);
	}

	/**
	 * 获取 前/后 x月 的时间.
	 * @param localDateTime 时间
	 * @param months 月
	 * @return LocalDateTime
	 */
	public static LocalDateTime plusMonths(LocalDateTime localDateTime, long months) {
		return localDateTime.plusMonths(months);
	}

	public static LocalDate plusMonths(LocalDate localDate, long months) {
		return localDate.plusMonths(months);
	}

	/**
	 * 获取 前/后 x年 的时间.
	 * @param localDateTime 时间
	 * @param years 年
	 * @return LocalDateTime
	 */
	public static LocalDateTime plusYears(LocalDateTime localDateTime, long years) {
		return localDateTime.plusYears(years);
	}

	public static LocalDateTime now() {
		return LocalDateTime.now();
	}

	public static LocalDate nowDate() {
		return LocalDate.now();
	}

	public static LocalDateTime getLocalDateTimeOfTimestamp(long timestamp) {
		Instant instant = Instant.ofEpochMilli(timestamp);
		ZoneId zoneId = ZoneId.systemDefault();
		return LocalDateTime.ofInstant(instant, zoneId);
	}

	public static long getTimestampOfLocalDateTime(LocalDateTime localDateTime) {
		ZoneId zoneId = ZoneId.systemDefault();
		return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
	}

	public static long getDays(LocalDateTime start, LocalDateTime end) {
		return Duration.between(start, end).toDays();
	}

	public static long getDays(LocalDate start, LocalDate end) {
		return Period.between(start, end).getDays();
	}

	public static long getHours(LocalDateTime start, LocalDateTime end) {
		return Duration.between(start, end).toHours();
	}

	public static long getMonths(LocalDate start, LocalDate end) {
		return Period.between(start, end).getMonths();
	}

	public static long getYears(LocalDate start, LocalDate end) {
		return Period.between(start, end).getYears();
	}

	public static LocalDate getDayOfWeek(LocalDate localDate, int index) {
		return localDate.with(TemporalAdjusters.nextOrSame(getWeekPattern(index)));
	}

	public static long getMinutes(LocalDateTime start, LocalDateTime end) {
		return Duration.between(start, end).toMinutes();
	}

	public static long getSeconds(LocalDateTime start, LocalDateTime end) {
		return Duration.between(start, end).toSeconds();
	}

	public static long getMillis(LocalDateTime start, LocalDateTime end) {
		return Duration.between(start, end).toMillis();
	}

	public static LocalDate getFirstDayOfMonth(LocalDate localDate) {
		return localDate.with(TemporalAdjusters.firstDayOfMonth());
	}

	public static LocalDate getLastDayOfMonth(LocalDate localDate) {
		return localDate.with(TemporalAdjusters.lastDayOfMonth());
	}

	public static String getDayOfWeekText(LocalDate localDate) {
		return localDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
	}

	public interface Constant {

		/**
		 * yyyy-MM-dd HH:mm:ss.
		 */
		String YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS = "yyyy-MM-dd HH:mm:ss";

		/**
		 * yyyyMMddHHmmss.
		 */
		String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

		/**
		 * yyyyMM.
		 */
		String YYYYMM = "yyyyMM";

		/**
		 * yyyy-MM-dd.
		 */
		String YYYY_ROD_MM_ROD_DD = "yyyy-MM-dd";

		/**
		 * yyyy年MM月dd日.
		 */
		String YYYY_TEXT_MM_TEXT_DD_TEXT = "yyyy年MM月dd日";

		/**
		 * yyyy.MM.dd.
		 */
		String YYYY_DOT_MM_DOT_DD = "yyyy.MM.dd";

		/**
		 * GMT+8.
		 */
		String DEFAULT_TIMEZONE = "GMT+8";

	}

}
