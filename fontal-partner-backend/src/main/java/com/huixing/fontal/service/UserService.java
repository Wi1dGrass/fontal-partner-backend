package com.huixing.fontal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huixing.fontal.common.PageResult;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.UpdateTagRequest;
import com.huixing.fontal.model.request.UpdatePasswordRequest;
import com.huixing.fontal.model.request.UserUpdateRequest;
import com.huixing.fontal.model.vo.UserDetailVO;
import com.huixing.fontal.model.vo.UserVO;

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

    /**
     * 更新用户信息
     * 只允许修改：昵称、头像、性别、联系方式、邮箱、个人简介
     * @param updateUserRequest 更新请求对象
     * @param currentUser 当前登录用户
     * @return 是否更新成功
     */
    Boolean updateUser(UserUpdateRequest updateUserRequest, User currentUser);

    /**
     * 修改密码
     * @param updatePasswordRequest 修改密码请求
     * @param currentUser 当前登录用户
     * @return 是否修改成功
     */
    Boolean updatePassword(UpdatePasswordRequest updatePasswordRequest, User currentUser);

    boolean isAdmin(User currentUser);

    /**
     * 根据标签搜索用户（分页）
     * @param tagNameList 标签列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果（UserVO对象）
     */
    PageResult<UserVO> searchUserByTags(Set<String> tagNameList, int pageNum, int pageSize);

    /**
     * 根据标签搜索用户（旧版，已废弃）
     * @deprecated 请使用 {@link #searchUserByTags(Set, int, int)}
     */
    @Deprecated
    List<User> searchUserByTags(Set<String> tagNameList);

    boolean isAdmin(HttpServletRequest request);

    String redisFormat(Long id);

    int updateTageById(UpdateTagRequest tagRequest, User currentUser);

    List<User> computeMatchUsers(User loginUser);

    /**
     * 根据搜索文本查询用户（分页）
     * @param searchText 搜索文本
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页结果（UserVO对象）
     */
    PageResult<UserVO> searchUsersByText(String searchText, int pageNum, int pageSize);

    /**
     * 智能推荐用户列表
     * - 游客模式：随机推荐
     * - 登录模式：最匹配的用户固定首位，其余随机
     *
     * @param userId 用户ID（null表示游客）
     * @param request HTTP请求（用于Session存储固定用户）
     * @param limit 返回数量
     * @return 推荐用户列表（VO对象，已脱敏）
     */
    List<UserVO> getRecommendUsers(Long userId, HttpServletRequest request, int limit);

    /**
     * 根据用户ID获取用户详情
     * 包含基础信息和联系方式，用于用户详情页面
     *
     * @param userId 用户ID
     * @return 用户详情（UserDetailVO对象，已脱敏）
     */
    UserDetailVO getUserDetailById(Long userId);
}
