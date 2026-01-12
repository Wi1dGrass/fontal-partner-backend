package com.huixing.fontal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.UpdateTagRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @Author fontal
 * @Data 22025/12/31
 */
public interface UserService extends IService<User> {
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    long userRegistration(String username, String userAccount, String userPassword, String checkPassword);

    User getLoginUser(HttpServletRequest request);

    User getSafetyUser(User user);

    Integer loginOut(HttpServletRequest request);

    int updateUser(User user, User currentUser);

    boolean isAdmin(User currentUser);


    List<User> searchUserByTags(Set<String> tagNameList);

    boolean isAdmin(HttpServletRequest request);

    String redisFormat(Long id);

    int updateTageById(UpdateTagRequest tagRequest, User currentUser);

    List<User> computeMatchUsers(User loginUser);
}
