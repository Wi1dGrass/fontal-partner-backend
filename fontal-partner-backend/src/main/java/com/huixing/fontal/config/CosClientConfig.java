package com.huixing.fontal.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 客户端配置
 *
 */
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /**
     * 腾讯云 SecretId
     */
    private String secretId;

    /**
     * 腾讯云 SecretKey
     */
    private String secretKey;

    /**
     * 腾讯云 COS 区域
     */
    private String region;

    /**
     * 腾讯云 COS 存储桶名称
     */
    private String bucket;

    @Bean
    public COSClient cosClient() {
        // 1. 初始化用户身份信息
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2. 设置 bucket 的区域
        Region regionConfig = new Region(region);
        // 3. 生成 cos 客户端
        ClientConfig clientConfig = new ClientConfig(regionConfig);
        return new COSClient(cred, clientConfig);
    }
}
