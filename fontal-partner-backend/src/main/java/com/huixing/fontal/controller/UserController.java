package com.huixing.fontal.controller;

import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.common.PageResult;
import com.huixing.fontal.common.ResultUtil;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.*;
import com.huixing.fontal.model.vo.UserDetailVO;
import com.huixing.fontal.model.vo.UserVO;
import com.huixing.fontal.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author fontal
 * @Date 2025/12/31
 */
@Api(tags = "用户管理")
@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 智能搜索/推荐用户列表
     * - 游客：随机推荐
     * - 登录：最匹配的用户固定首位，其余随机
     */
    @ApiOperation("智能搜索/推荐用户列表")
    @GetMapping("/search")
    public BaseResponse<List<UserVO>> searchList(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        // 1. 参数校验
        if (limit < 1 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "返回数量必须在1-50之间");
        }

        // 2. 获取当前登录用户（不强制登录）
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception ignored) {}

        // 3. 调用Service层
        Long userId = (loginUser != null) ? loginUser.getId() : null;
        List<UserVO> users = userService.getRecommendUsers(userId, request, limit);

        // 4. 返回结果
        return ResultUtil.success(users);
    }

    @ApiOperation("按文本搜索用户（分页）")
    @PostMapping("/search")
    public BaseResponse<PageResult<UserVO>> queryUser(@RequestBody UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请输入文本");
        }

        // 参数校验和默认值设置
        Integer pageNum = userQueryRequest.getPageNum();
        Integer pageSize = userQueryRequest.getPageSize();
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        log.info("文本搜索用户：搜索文本 = {}, 页码 = {}, 每页大小 = {}", userQueryRequest.getSearchText(), pageNum, pageSize);

        try {
            PageResult<UserVO> pageResult = userService.searchUsersByText(userQueryRequest.getSearchText(), pageNum, pageSize);
            log.info("文本搜索用户：找到 {} 个匹配用户，返回第 {} 页，共 {} 条",
                    pageResult.getTotal(), pageNum, pageResult.getRecords().size());
            return ResultUtil.success(pageResult);
        } catch (Exception e) {
            log.error("按文本搜索用户失败，searchText: {}", userQueryRequest.getSearchText(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索失败，请稍后重试");
        }
    }

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<User> login(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        //进行参数校验
        if (loginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtil.success(user, "登入成功");
    }

    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest registerRequest) {
        if (registerRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String username = registerRequest.getUsername();
        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();
        if (StringUtils.isAllBlank(username, userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegistration(username, userAccount, userPassword, checkPassword);
        return ResultUtil.success(result, "注册成功");
    }

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        Long userId = currentUser.getId();
        User user = userService.getById(userId);
        return ResultUtil.success(userService.getSafetyUser(user));
    }

    @ApiOperation("用户退出登录")
    @PostMapping("/loginOut")
    public BaseResponse<Integer> loginOut(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtil.success(userService.loginOut(request));
    }

    @ApiOperation("根据用户ID获取用户信息")
    @GetMapping("/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = this.userService.getById(id);
        return ResultUtil.success(user);
    }

    /**
     * 获取用户详情
     * 包含基础信息和联系方式，用于用户详情页面
     */
    @ApiOperation("获取用户详情")
    @GetMapping("/{id}/detail")
    public BaseResponse<UserDetailVO> getUserDetail(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        UserDetailVO userDetail = userService.getUserDetailById(id);
        return ResultUtil.success(userDetail);
    }

    /**
     * 更新用户信息
     * 只允许修改：昵称、头像、性别、联系方式、邮箱、个人简介
     */
    @ApiOperation("更新用户信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest updateUserRequest,
                                             HttpServletRequest request) {
        // 1. 参数校验
        if (updateUserRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 2. 登录校验
        User currentUser = userService.getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 3. 调用更新方法
        boolean result = userService.updateUser(updateUserRequest, currentUser);

        // 4. 清除缓存
        redisTemplate.delete(userService.redisFormat(currentUser.getId()));

        // 5. 返回结果
        return ResultUtil.success(result, "更新成功");
    }

    /**
     * 修改密码
     * 需要验证原密码，新密码必须包含字母和数字，长度8-20个字符
     */
    @ApiOperation("修改用户密码")
    @PostMapping("/updatePassword")
    public BaseResponse<Boolean> updatePassword(@RequestBody UpdatePasswordRequest updatePasswordRequest,
                                                 HttpServletRequest request) {
        // 1. 参数校验
        if (updatePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 2. 登录校验
        User currentUser = userService.getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "请先登录");
        }

        // 3. 调用修改密码方法
        boolean result = userService.updatePassword(updatePasswordRequest, currentUser);

        // 4. 返回结果
        return ResultUtil.success(result, "密码修改成功");
    }

    @ApiOperation("删除用户（仅管理员）")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(Long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        if (id == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean remove = userService.removeById(id);
        if (remove) {
            redisTemplate.delete(userService.redisFormat(id));
        }
        return ResultUtil.success(remove);
    }

    @ApiOperation("按标签搜索用户（分页）")
    @GetMapping("/search/tags")
    public BaseResponse<PageResult<UserVO>> searchUsersByTags(
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 参数校验
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1-100之间");
        }
        if (pageNum < 1) {
            pageNum = 1;
        }

        // 参数校验：允许空标签，返回空列表而不是报错
        if (StringUtils.isBlank(tags)) {
            log.info("标签搜索：标签参数为空");
            PageResult<UserVO> emptyResult = new PageResult<>(new ArrayList<>(), 0, pageNum, pageSize);
            return ResultUtil.success(emptyResult);
        }

        // 解析逗号分隔的标签字符串
        Set<String> tagNameSet = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::capitalize)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(tagNameSet)) {
            log.info("标签搜索：解析后的标签为空，原始参数: {}", tags);
            PageResult<UserVO> emptyResult = new PageResult<>(new ArrayList<>(), 0, pageNum, pageSize);
            return ResultUtil.success(emptyResult);
        }

        log.info("标签搜索：搜索标签 = {}, 页码 = {}, 每页大小 = {}", tagNameSet, pageNum, pageSize);

        try {
            PageResult<UserVO> pageResult = userService.searchUserByTags(tagNameSet, pageNum, pageSize);
            log.info("标签搜索：找到 {} 个匹配用户，返回第 {} 页，共 {} 条",
                    pageResult.getTotal(), pageNum, pageResult.getRecords().size());
            return ResultUtil.success(pageResult);
        } catch (Exception e) {
            log.error("按标签搜索用户失败，tags: {}", tags, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "搜索失败，请稍后重试");
        }
    }

    @ApiOperation("更新用户标签")
    @PostMapping("/update/tags")
    public BaseResponse<Integer> updateTagById(@RequestBody UpdateTagRequest tagRequest, HttpServletRequest request) {
        if (tagRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getLoginUser(request);
        int updateTag = userService.updateTageById(tagRequest, currentUser);
        redisTemplate.delete(userService.redisFormat(tagRequest.getId()));
        return ResultUtil.success(updateTag);
    }

}