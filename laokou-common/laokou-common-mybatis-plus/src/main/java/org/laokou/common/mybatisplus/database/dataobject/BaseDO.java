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

package org.laokou.common.mybatisplus.database.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.laokou.common.i18n.dto.AbstractDO;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.time.LocalDateTime;

import static org.laokou.common.i18n.common.MybatisConstants.*;
import static org.laokou.common.i18n.utils.DateUtil.Constant.DEFAULT_TIMEZONE;
import static org.laokou.common.i18n.utils.DateUtil.Constant.YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS;

/**
 * @author laokou
 */
@Data
@Schema(name = "BaseDO", description = "映射基类")
public class BaseDO extends AbstractDO {

	@Serial
	private static final long serialVersionUID = -5855413730985647400L;

	@TableId(type = IdType.INPUT)
	@Schema(name = ID, description = "ID")
	private Long id;

	@Schema(name = CREATOR, description = "创建人")
	@TableField(fill = FieldFill.INSERT)
	private Long creator;

	@Schema(name = EDITOR, description = "修改人")
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Long editor;

	@Schema(name = CREATE_DATE, description = "创建时间")
	@TableField(fill = FieldFill.INSERT)
	@JsonFormat(pattern = YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS, timezone = DEFAULT_TIMEZONE)
	@DateTimeFormat(pattern = YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS)
	private LocalDateTime createDate;

	@Schema(name = UPDATE_DATE, description = "修改时间")
	@TableField(fill = FieldFill.INSERT_UPDATE)
	@JsonFormat(pattern = YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS, timezone = DEFAULT_TIMEZONE)
	@DateTimeFormat(pattern = YYYY_ROD_MM_ROD_DD_SPACE_HH_RISK_HH_RISK_SS)
	private LocalDateTime updateDate;

	@Schema(name = DEL_FLAG, description = "删除标识 0未删除 1已删除")
	@TableField(fill = FieldFill.INSERT)
	@TableLogic
	private Integer delFlag;

	@Version
	@Schema(name = VERSION, description = "版本号")
	@TableField(fill = FieldFill.INSERT)
	private Integer version;

	@Schema(name = DEPT_ID, description = "部门ID")
	@TableField(fill = FieldFill.INSERT)
	private Long deptId;

	@Schema(name = DEPT_PATH, description = "部门PATH")
	@TableField(fill = FieldFill.INSERT)
	private String deptPath;

	@Schema(name = TENANT_ID, description = "租户ID")
	@TableField(fill = FieldFill.INSERT)
	private Long tenantId;

}
