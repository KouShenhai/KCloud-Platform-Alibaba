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
package org.laokou.common.security.config.auto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.laokou.common.core.config.OAuth2ResourceServerProperties;
import org.laokou.common.core.utils.MapUtil;
import org.laokou.common.security.config.GlobalOpaqueTokenIntrospector;
import org.laokou.common.security.handler.OAuth2ExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;

import static org.laokou.common.i18n.common.PropertiesConstants.OAUTH2_RESOURCE_SERVER_PREFIX;
import static org.laokou.common.i18n.common.PropertiesConstants.SPRING_APPLICATION_NAME;
import static org.laokou.common.i18n.common.StringConstants.TRUE;
import static org.laokou.common.i18n.common.SysConstants.ENABLED;

/**
 * 关闭OAuth2,请在yml配置spring.oauth2.resource-server.enabled=false
 * 关闭security，请排除SecurityAutoConfiguration、ManagementWebSecurityAutoConfiguration.
 *
 * @author laokou
 */
@Data
@EnableWebSecurity
@EnableMethodSecurity
@AutoConfiguration(after = { OAuth2AuthorizationAutoConfig.class })
@ConditionalOnProperty(havingValue = TRUE, matchIfMissing = true, prefix = OAUTH2_RESOURCE_SERVER_PREFIX,
		name = ENABLED)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class OAuth2ResourceServerAutoConfig {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 1000)
	@ConditionalOnMissingBean(SecurityFilterChain.class)
	SecurityFilterChain resourceFilterChain(GlobalOpaqueTokenIntrospector globalOpaqueTokenIntrospector,
			Environment env, OAuth2ResourceServerProperties oAuth2ResourceServerProperties, HttpSecurity http)
			throws Exception {
		return http
			.headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.httpStrictTransportSecurity(
					hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)))
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement(AbstractHttpConfigurer::disable)
			.securityContext(AbstractHttpConfigurer::disable)
			.csrf(AbstractHttpConfigurer::disable)
			.cors(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			// 基于token，关闭session
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(customizer(env, oAuth2ResourceServerProperties))
			// https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/opaque-token.html
			// 提供自定义OpaqueTokenIntrospector，否则回退到NimbusOpaqueTokenIntrospector
			.oauth2ResourceServer(
					resource -> resource.opaqueToken(token -> token.introspector(globalOpaqueTokenIntrospector))
						.accessDeniedHandler(OAuth2ExceptionHandler::handleAccessDenied)
						.authenticationEntryPoint(OAuth2ExceptionHandler::handleAuthentication))
			.build();
	}

	@NotNull
	public static Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> customizer(
			Environment env, OAuth2ResourceServerProperties oAuth2ResourceServerProperties) {
		Map<String, Set<String>> uriMap = Optional
			.of(MapUtil.toUriMap(oAuth2ResourceServerProperties.getRequestMatcher().getIgnorePatterns(),
					env.getProperty(SPRING_APPLICATION_NAME)))
			.orElseGet(HashMap::new);
		return request -> request.requestMatchers(HttpMethod.GET,
				Optional.ofNullable(uriMap.get(HttpMethod.GET.name())).orElseGet(HashSet::new).toArray(String[]::new))
			.permitAll()
			.requestMatchers(HttpMethod.POST,
					Optional.ofNullable(uriMap.get(HttpMethod.POST.name()))
						.orElseGet(HashSet::new)
						.toArray(String[]::new))
			.permitAll()
			.requestMatchers(HttpMethod.PUT,
					Optional.ofNullable(uriMap.get(HttpMethod.PUT.name()))
						.orElseGet(HashSet::new)
						.toArray(String[]::new))
			.permitAll()
			.requestMatchers(HttpMethod.DELETE,
					Optional.ofNullable(uriMap.get(HttpMethod.DELETE.name()))
						.orElseGet(HashSet::new)
						.toArray(String[]::new))
			.permitAll()
			.requestMatchers(HttpMethod.HEAD,
					Optional.ofNullable(uriMap.get(HttpMethod.HEAD.name()))
						.orElseGet(HashSet::new)
						.toArray(String[]::new))
			.permitAll()
			.requestMatchers(HttpMethod.PATCH,
					Optional.ofNullable(uriMap.get(HttpMethod.PATCH.name()))
						.orElseGet(HashSet::new)
						.toArray(String[]::new))
			.permitAll()
			.anyRequest()
			.authenticated();
	}

}
