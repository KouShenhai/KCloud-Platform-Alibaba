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

package org.laokou.admin.api;

import org.laokou.admin.dto.index.IndexGetQry;
import org.laokou.admin.dto.index.IndexListQry;
import org.laokou.admin.dto.index.IndexTraceGetQry;
import org.laokou.admin.dto.index.IndexTraceListQry;
import org.laokou.admin.dto.index.clientobject.IndexCO;
import org.laokou.common.i18n.dto.Datas;
import org.laokou.common.i18n.dto.Result;

import java.util.Map;

/**
 * @author laokou
 */
public interface IndexsServiceI {

	Result<Datas<IndexCO>> list(IndexListQry qry);

	Result<Map<String, Object>> info(IndexGetQry qry);

	Result<Datas<Map<String, Object>>> traceList(IndexTraceListQry qry);

	Result<Map<String, Object>> getTraceById(IndexTraceGetQry qry);

}
