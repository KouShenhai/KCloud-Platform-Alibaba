/**
 * Copyright (c) 2022 KCloud-Platform-Alibaba Authors. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.laokou.tenant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.laokou.tenant.entity.SysTenantSourceDO;
import org.laokou.tenant.mapper.SysTenantSourceMapper;
import org.laokou.tenant.qo.SysTenantSourceQo;
import org.laokou.tenant.service.SysTenantSourceService;
import org.laokou.tenant.vo.SysTenantSourceVO;
import org.springframework.stereotype.Service;

/**
 * @author laokou
 */
@Service
public class SysTenantSourceServiceImpl extends ServiceImpl<SysTenantSourceMapper, SysTenantSourceDO> implements SysTenantSourceService {

    @Override
    public IPage<SysTenantSourceVO> queryTenantSourcePage(IPage<SysTenantSourceVO> page, SysTenantSourceQo qo) {
        return this.baseMapper.queryTenantSourcePage(page,qo);
    }

    @Override
    public Integer getVersion(Long id) {
        return this.baseMapper.getVersion(id);
    }

    @Override
    public void deleteTenantSource(Long id) {
        this.baseMapper.deleteById(id);
    }

    @Override
    public String queryTenantSourceName(Long tenantId) {
        return this.baseMapper.queryTenantSourceName(tenantId);
    }

    @Override
    public SysTenantSourceVO queryTenantSource(String sourceName) {
        return this.baseMapper.queryTenantSource(sourceName);
    }

}
