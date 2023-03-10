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
package org.laokou.oss.server.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.laokou.common.i18n.core.CustomException;
import org.laokou.common.i18n.core.HttpResult;
import org.laokou.oss.client.vo.UploadVO;
import org.laokou.oss.server.support.OssTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
/**
 * 对象存储控制器
 * @author laokou
 */
@RestController
@Tag(name = "Oss API",description = "对象存储API")
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class OssApiController {

    private final OssTemplate ossTemplate;

    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "对象存储>上传",description = "对象存储>上传")
    public HttpResult<UploadVO> upload(@RequestPart("file") MultipartFile file,@RequestParam("md5")String md5) throws Exception {
        if (file.isEmpty()) {
            throw new CustomException("上传的文件不能为空");
        }
        return new HttpResult<UploadVO>().ok(ossTemplate.upload(file.getSize(),md5,file.getOriginalFilename(),file.getContentType(),file.getInputStream()));
    }

}
