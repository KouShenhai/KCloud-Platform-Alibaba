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

package org.laokou.admin.command.monitor.query;

import lombok.RequiredArgsConstructor;
import org.laokou.admin.dto.monitor.MonitorRedisCacheGetQry;
import org.laokou.admin.dto.monitor.clientobject.RedisCacheCO;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.redis.utils.RedisUtil;
import org.springframework.stereotype.Component;

/**
 * @author laokou
 */
@Component
@RequiredArgsConstructor
public class MonitorCacheGetQryExe {

	private final RedisUtil redisUtil;

	public Result<RedisCacheCO> execute(MonitorRedisCacheGetQry qry) {
		RedisCacheCO cacheCO = new RedisCacheCO();
		cacheCO.setCommandStats(redisUtil.getCommandStatus());
		cacheCO.setInfo(redisUtil.getInfo());
		cacheCO.setKeysSize(redisUtil.getKeysSize());
		return Result.of(cacheCO);
	}

}
