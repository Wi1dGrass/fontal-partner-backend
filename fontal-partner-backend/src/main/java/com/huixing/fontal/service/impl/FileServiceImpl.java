package com.huixing.fontal.service.impl;

import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.contant.FileConstant;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.model.enums.FileUploadBizEnum;
import com.huixing.fontal.service.FileService;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传服务实现
 *
 * @author qimu
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Resource
    private COSClient cosClient;

    @Resource
    private com.huixing.fontal.config.CosClientConfig cosClientConfig;

    /**
     * 允许的文件后缀
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpeg", "jpg", "png", "gif", "bmp", "webp");

    /**
     * 最大文件大小：2MB
     */
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    @Override
    public String uploadFile(MultipartFile file, String biz) {
        // 1. 校验文件
        validFile(file);

        // 2. 校验业务类型
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "业务类型不存在");
        }

        // 3. 获取文件原始名称
        String originalFilename = file.getOriginalFilename();
        String extension = null;
        if (originalFilename != null) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        // 4. 生成唯一文件名
        String filename = generateFilename(extension);

        // 5. 上传到腾讯云 COS
        try {
            // 创建临时文件
            File tempFile = File.createTempFile("upload", "." + extension);
            file.transferTo(tempFile);

            // 构建上传路径
            String key = biz + "/" + filename;
            
            // 上传到 COS
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    cosClientConfig.getBucket(),
                    key,
                    tempFile
            );
            
            cosClient.putObject(putObjectRequest);
            
            // 删除临时文件
            tempFile.delete();

            // 6. 返回文件访问路径
            String fileUrl = FileConstant.COS_HOST + "/" + key;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
            
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public void validFile(MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 校验文件大小
        long size = multipartFile.getSize();
        if (size > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        }

        // 校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型，仅支持: " + ALLOWED_EXTENSIONS);
        }
    }

    /**
     * 生成唯一文件名
     *
     * @param extension 文件扩展名
     * @return 文件名
     */
    private String generateFilename(String extension) {
        // 使用时间戳 + 随机字符串生成唯一文件名
        long timestamp = System.currentTimeMillis();
        String randomString = RandomStringUtils.randomAlphanumeric(8);
        return timestamp + "_" + randomString + "." + extension;
    }
}
