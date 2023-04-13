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
package org.laokou.common.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author laokou
 */
public class MapUtil {

    public static Map<String,String> parseParamMap(String params) {
        String[] strings = params.split("&");
        int length = strings.length;
        if (length == 0) {
            return new HashMap<>(0);
        }
        Map<String,String> paramMap = new HashMap<>(strings.length);
        for (String string : strings) {
            int index = string.indexOf("=");
            if (index > -1) {
                String key = string.substring(0, index);
                String value = string.substring(index + 1);
                paramMap.put(key,value);
            }
        }
        return paramMap;
    }

    public static String parseParams(Map<String,String> paramMap) {
        Iterator<Map.Entry<String, String>> iterator = paramMap.entrySet().iterator();
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            stringBuilder.append(key).append("=").append(value).append("&");
        }
        return StringUtils.substringBeforeLast(stringBuilder.toString(),"&");
    }

    public static void main(String[] args) {
        String params = "k=1&v=2";
        Map<String, String> stringStringMap = parseParamMap(params);
        System.out.println(stringStringMap);
        System.out.println(parseParams(stringStringMap));
    }

    public static MultiValueMap<String, String> getParameters(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>(parameterMap.size());
        parameterMap.forEach((key, values) -> {
            if (values.length > 0) {
                for (String value : values) {
                    parameters.add(key, value);
                }
            }
        });
        return parameters;
    }

}
