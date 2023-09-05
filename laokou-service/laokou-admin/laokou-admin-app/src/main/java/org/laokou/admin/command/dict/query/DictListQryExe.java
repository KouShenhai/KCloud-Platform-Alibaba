/*
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
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

package org.laokou.admin.command.dict.query;

import lombok.RequiredArgsConstructor;
import org.laokou.admin.client.dto.dict.DictListQry;
import org.laokou.admin.client.dto.dict.clientobject.DictCO;
import org.laokou.admin.domain.common.DataPage;
import org.laokou.admin.domain.dict.Dict;
import org.laokou.admin.domain.gateway.DictGateway;
import org.laokou.common.core.utils.ConvertUtil;
import org.laokou.common.i18n.dto.Datas;
import org.laokou.common.i18n.dto.Result;
import org.springframework.stereotype.Component;

/**
 * @author laokou
 */
@Component
@RequiredArgsConstructor
public class DictListQryExe {

    private final DictGateway dictGateway;

    public Result<Datas<DictCO>> execute(DictListQry qry) {
        Dict dict = new Dict();
        dict.setType(qry.getType());
        dict.setLabel(qry.getLabel());
        Datas<Dict> datas = dictGateway.list(dict, new DataPage(qry.getPageNum(), qry.getPageSize()));
        Datas<DictCO> da = new Datas<>();
        da.setRecords(ConvertUtil.sourceToTarget(datas.getRecords(),DictCO.class));
        da.setTotal(datas.getTotal());
        return Result.of(da);
    }

}
