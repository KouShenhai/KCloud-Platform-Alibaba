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

package org.laokou.admin.command.cluster.query;

import lombok.RequiredArgsConstructor;
import org.laokou.admin.config.DefaultConfigProperties;
import org.laokou.admin.dto.cluster.ClusterServiceListQry;
import org.laokou.admin.dto.cluster.clientobject.ClusterServiceCO;
import org.laokou.common.i18n.dto.Datas;
import org.laokou.common.i18n.dto.Result;
import org.laokou.common.i18n.utils.StringUtil;
import org.laokou.common.nacos.utils.ServiceUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @author laokou
 */
@Component
@RequiredArgsConstructor
public class ClusterServiceListQryExe {

	private final ServiceUtil serviceUtil;

	private final DefaultConfigProperties defaultConfigProperties;

	public Result<Datas<ClusterServiceCO>> execute(ClusterServiceListQry qry) {
		return Result.of(getDatas(qry));
	}

	private Datas<ClusterServiceCO> getDatas(ClusterServiceListQry qry) {
		Integer pageNum = qry.getPageNum();
		Integer pageSize = qry.getPageSize();
		String id = qry.getId();
		Set<String> services = defaultConfigProperties.getGracefulShutdownServices();
		List<String> list = serviceUtil.getServices();
		list = list.stream().filter(services::contains).toList();
		if (StringUtil.isNotEmpty(id)) {
			list = list.stream().filter(n -> n.contains(id)).toList();
		}
		return new Datas<>(list.size(),
				list.stream()
					.map(ClusterServiceCO::new)
					.skip((long) (pageNum - 1) * pageSize)
					.limit(pageSize)
					.toList());
	}

}