package com.huixing.fontal.config;

import com.huixing.fontal.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web MVC 配置类
 * 配置拦截器
 *
 * @author fontal
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    /**
     * 添加拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        // 用户注册
                        "/user/register",
                        // 用户登录
                        "/user/login",
                        // 用户搜索（允许游客访问）
                        "/user/search",
                        // 按标签搜索用户
                        "/user/search/tags",
                        // 获取队伍信息（允许游客访问）
                        "/team/team",
                        // 根据ID获取队伍信息
                        "/team/**",
                        // Swagger相关
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v2/api-docs/**",
                        "/favicon.ico",
                        // 错误页面
                        "/error"
                );
    }
}
