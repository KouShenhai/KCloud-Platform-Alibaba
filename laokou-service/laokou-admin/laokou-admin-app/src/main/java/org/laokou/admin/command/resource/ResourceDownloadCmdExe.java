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

package org.laokou.admin.command.resource;

import com.baomidou.dynamic.datasource.annotation.DS;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.laokou.admin.dto.resource.ResourceDownloadCmd;
import org.laokou.admin.gatewayimpl.database.ResourceMapper;
import org.laokou.admin.gatewayimpl.database.dataobject.ResourceDO;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.laokou.common.i18n.common.DatasourceConstants.TENANT;

/**
 * @author laokou
 */
@Component
@RequiredArgsConstructor
public class ResourceDownloadCmdExe {

	private final ResourceMapper resourceMapper;

	@SneakyThrows
	@DS(TENANT)
	public void executeVoid(ResourceDownloadCmd cmd) {
		ResourceDO resourceDO = resourceMapper.selectById(cmd.getId());
		HttpServletResponse response = cmd.getResponse();
		response.setContentType("application/octet-stream");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Content-disposition",
				"attachment;filename=" + StandardCharsets.UTF_8.encode(resourceDO.getTitle()));
		try (ServletOutputStream outputStream = response.getOutputStream()) {
			URL u = URI.create(resourceDO.getUrl()).toURL();
			HttpURLConnection conn = (HttpURLConnection) u.openConnection();
			conn.setDoInput(true);
			conn.setRequestMethod(HttpMethod.GET.name());
			conn.setConnectTimeout(6000);
			IOUtils.copy(conn.getInputStream(), outputStream);
			conn.disconnect();
		}
	}

}
