package com.huixing.fontal.controller;

import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.common.ResultUtil;
import com.huixing.fontal.model.file.UploadFileRequest;
import com.huixing.fontal.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

/**
 * 文件上传控制器
 *
 * @author qimu
 */
@RestController
@Slf4j
@RequestMapping("/file")
@Api(tags = "文件上传管理")
public class FileController {

    @Resource
    private FileService fileService;

    /**
     * 上传文件
     *
     * @param file 文件
     * @param biz  业务类型（user_avatar: 用户头像, team_avatar: 队伍头像）
     * @return 文件访问路径
     */
    @PostMapping("/upload")
    @ApiOperation(value = "上传文件")
    public BaseResponse<String> uploadFile(
            @RequestPart("file") MultipartFile file,
            String biz) {
        
        if (biz == null || biz.isEmpty()) {
            return ResultUtil.error(ErrorCode.PARAMS_ERROR, "业务类型不能为空");
        }
        
        String fileUrl = fileService.uploadFile(file, biz);
        return ResultUtil.success(fileUrl);
    }
}
