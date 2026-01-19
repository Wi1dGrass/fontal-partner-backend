package com.huixing.fontal.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务
 *
 * @author qimu
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param file    文件
     * @param biz     业务类型
     * @return 文件访问路径
     */
    String uploadFile(MultipartFile file, String biz);

    /**
     * 校验文件
     *
     * @param multipartFile 文件
     */
    void validFile(MultipartFile multipartFile);
}
