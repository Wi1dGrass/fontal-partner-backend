package com.huixing.fontal.interceptor;

import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 * 拦截需要登录的接口
 *
 * @author fontal
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    /**
     * 前置拦截器，在请求处理之前进行调用
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param handler  处理器
     * @return true表示继续流程，false表示中断流程
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取当前登录用户
        try {
            User loginUser = userService.getLoginUser(request);
            if (loginUser == null) {
                throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
            }
            // 登录成功，放行
            return true;
        } catch (BusinessException e) {
            log.error("登录校验失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("登录校验异常", e);
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }
    }
}
