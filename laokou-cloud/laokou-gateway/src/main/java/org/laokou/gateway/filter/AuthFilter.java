/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *   http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.laokou.gateway.filter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.http.HttpUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.laokou.common.core.constant.Constant;
import org.laokou.common.i18n.core.StatusCode;
import org.laokou.gateway.utils.PasswordUtil;
import org.laokou.common.core.utils.StringUtil;
import org.laokou.gateway.constant.GatewayConstant;
import org.laokou.gateway.utils.ResponseUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static org.laokou.gateway.constant.GatewayConstant.OAUTH2_AUTH_URI;
/**
 * ??????Filter
 * @author laokou
 */
@Component
@Slf4j
@RefreshScope
@Data
@ConfigurationProperties(prefix = "ignore")
public class AuthFilter implements GlobalFilter,Ordered {

    /**
     * ????????????urls
     */
    private List<String> uris;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // ??????request??????
        ServerHttpRequest request = exchange.getRequest();
        // ??????uri
        String requestUri = request.getPath().pathWithinApplication().value();
        log.info("uri???{}", requestUri);
        // ?????????????????????????????????
        if (ResponseUtil.pathMatcher(requestUri,uris)){
            return chain.filter(exchange);
        }
        // ????????????
        MediaType mediaType = request.getHeaders().getContentType();
        if (OAUTH2_AUTH_URI.contains(requestUri)
                && HttpMethod.POST.matches(request.getMethod().name())
                && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)) {
            return oauth2Decode(exchange,chain);
        }
        // ??????token
        String token = ResponseUtil.getToken(request);
        if (StringUtil.isEmpty(token)) {
            return ResponseUtil.response(exchange, ResponseUtil.error(StatusCode.UNAUTHORIZED));
        }
        ServerHttpRequest build = exchange.getRequest().mutate()
                .header(Constant.AUTHORIZATION_HEAD, token).build();
        return chain.filter(exchange.mutate().request(build).build());
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * OAuth2??????
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> oauth2Decode(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerRequest serverRequest = ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders());
        Mono modifiedBody = serverRequest.bodyToMono(String.class).flatMap(decrypt());
        BodyInserter<Mono, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
            ServerHttpRequest decorator = requestDecorator(exchange, headers, outputMessage);
            return chain.filter(exchange.mutate().request(decorator).build());
        }));
    }

    private Function decrypt() {
        return s -> {
            // ???????????????????????????
            Map<String, String> inParamsMap = HttpUtil.decodeParamMap((String) s, CharsetUtil.CHARSET_UTF_8);
            if (inParamsMap.containsKey(GatewayConstant.PASSWORD) && inParamsMap.containsKey(GatewayConstant.USERNAME)) {
                try {
                    String password = inParamsMap.get(GatewayConstant.PASSWORD);
                    String username = inParamsMap.get(GatewayConstant.USERNAME);
                    // ???????????????????????????
                    if (StringUtil.isNotEmpty(password)) {
                        inParamsMap.put(GatewayConstant.PASSWORD, PasswordUtil.decode(password));
                    }
                    if (StringUtil.isNotEmpty(username)) {
                        inParamsMap.put(GatewayConstant.USERNAME, PasswordUtil.decode(username));
                    }
                } catch (Exception e) {
                    log.error("???????????????{}",e.getMessage());
                }
            }
            else {
                log.error("??????????????????:{}", s);
            }
            return Mono.just(HttpUtil.toParams(inParamsMap, Charset.defaultCharset(), true));
        };
    }

    private ServerHttpRequestDecorator requestDecorator(ServerWebExchange exchange, HttpHeaders headers, CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }
}
