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
package org.laokou.test.elasticsearch;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import org.laokou.common.elasticsearch.annotation.ElasticsearchField;
import org.laokou.common.elasticsearch.annotation.Field;
import org.laokou.common.elasticsearch.constant.Constant;
import org.laokou.common.elasticsearch.enums.FieldTypeEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author laokou
 */
@Data
public class ResourceIndex implements Serializable {

	@Serial
	private static final long serialVersionUID = -3715061850731611381L;

	@Field(value = "id", type = FieldTypeEnum.LONG)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long id;

	@Field(value = "title", type = FieldTypeEnum.TEXT, searchAnalyzer = Constant.IK_SEARCH_ANALYZER,
			analyzer = Constant.IK_ANALYZER)
	private String title;

	@Field(value = "code")
	private String code;

	@Field(value = "remark", type = FieldTypeEnum.TEXT, searchAnalyzer = Constant.IK_SEARCH_ANALYZER,
			analyzer = Constant.IK_ANALYZER)
	private String remark;

	@Field(value = "ymd")
	private String ymd;

}
