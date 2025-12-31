package com.qimu.jujiao.service;

import com.qimu.jujiao.model.entity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author fontal
 * @Data 22025/12/31
 */
public interface UserService {
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    long userRegistration(String username, String userAccount, String userPassword, String checkPassword);
}
