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

package org.laokou.iot;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.laokou.common.i18n.common.NetworkConstants.IP;

/**
 * @author laokou
 */
@SpringBootApplication(scanBasePackages = "org.laokou")
public class IotApp {

	public static void main(String[] args) throws UnknownHostException {
		// System.setProperty(TlsSystemConfig.TLS_ENABLE, TRUE);
		// System.setProperty(TlsSystemConfig.CLIENT_AUTH, TRUE);
		// System.setProperty(TlsSystemConfig.CLIENT_TRUST_CERT, "tls/nacos.cer");
		System.setProperty(IP, InetAddress.getLocalHost().getHostAddress());
		new SpringApplicationBuilder(IotApp.class).web(WebApplicationType.SERVLET).run(args);
	}

}
