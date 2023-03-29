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

package org.laokou.test.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.laokou.test.entity.DeviceExportPO;
import org.laokou.test.mapper.DeviceExportMapper;
import org.laokou.test.service.DeviceExportService;
import org.springframework.stereotype.Service;

/**
 * @author laokou
 */
@Service
@RequiredArgsConstructor
public class DeviceExportServiceImpl extends ServiceImpl<DeviceExportMapper, DeviceExportPO> implements DeviceExportService {

    private final DeviceExportMapper deviceExportMapper;

    @Override
    public Integer getCount(String dateTime) {
        return deviceExportMapper.getCount(dateTime);
    }
}
