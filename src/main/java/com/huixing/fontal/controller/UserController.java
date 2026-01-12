package com.huixing.fontal.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.common.ResultUtil;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.UpdateTagRequest;
import com.huixing.fontal.model.request.UserLoginRequest;
import com.huixing.fontal.model.request.UserQueryRequest;
import com.huixing.fontal.model.request.UserRegisterRequest;
import com.huixing.fontal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author fontal
 * @Date 2025/12/31
 */
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
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchList(HttpServletRequest request) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 1. 尝试获取登录用户（不强制登录）
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception ignored) {}

        // 2. 确定缓存 Key
        String cacheKey = (loginUser == null) ? "fontal:user:guest" : userService.redisFormat(loginUser.getId());

        // 3. 优先访问缓存
        List<User> cachedUserList = (List<User>) valueOperations.get(cacheKey);
        if (cachedUserList != null) {
            return ResultUtil.success(doShuffle(cachedUserList));
        }

        // 4. 缓存未命中，开始计算
        List<User> finalUserList;
        if (loginUser == null) {
            // 游客模式：随机获取
            finalUserList = userService.list(new QueryWrapper<User>().last("ORDER BY RAND() LIMIT 10"));
        } else {
            // 登录模式：核心算法匹配
            finalUserList = userService.computeMatchUsers(loginUser);
        }

        // 5. 最终兜底：如果没算出来，随机给 10 个
        if (CollectionUtils.isEmpty(finalUserList)) {
            finalUserList = userService.list(new QueryWrapper<User>().last("ORDER BY RAND() LIMIT 10"));
        }

        // 6. 脱敏处理
        List<User> safetyUserList = finalUserList.stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toList());

        // 7. 写入缓存（设置 1 分钟过期，并加入随机扰动防止雪崩）
        int randomExpireTime = 60 + new Random().nextInt(30);
        valueOperations.set(cacheKey, safetyUserList, randomExpireTime, TimeUnit.SECONDS);

        // 8. 返回打乱后的结果
        return ResultUtil.success(doShuffle(safetyUserList));
    }

    /**
     * 辅助：标准化标签（首字母大写）
     */
    private Set<String> toCapitalize(Set<String> oldSet) {
        if (oldSet == null) return new HashSet<>();
        return oldSet.stream()
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::capitalize)
                .collect(Collectors.toSet());
    }

    /**
     * 辅助：固定首位并随机打乱其余用户
     */
    private List<User> doShuffle(List<User> userList) {
        if (CollectionUtils.isEmpty(userList) || userList.size() <= 1) {
            return userList;
        }
        List<User> result = new ArrayList<>(userList);
        User firstUser = result.get(0);
        List<User> others = new ArrayList<>(result.subList(1, result.size()));
        Collections.shuffle(others);

        List<User> finalResult = new ArrayList<>();
        finalResult.add(firstUser);
        finalResult.addAll(others);
        return finalResult;
    }

    @PostMapping("/search")
    public BaseResponse<List<User>> queryUser(@RequestBody UserQueryRequest userQueryRequest,HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请输入文本");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 模糊匹配多个字段：用户名、标签、个人简介
        queryWrapper.and(qw -> qw.like("username", userQueryRequest.getSearchText())
                .or().like("tags", userQueryRequest.getSearchText())
                .or().like("profile", userQueryRequest.getSearchText()));

        List<User> userList = userService.list(queryWrapper);

        // 脱敏处理
        List<User> safetyUserList = userList.stream()
                .map(userService::getSafetyUser)
                .collect(Collectors.toList());
        return ResultUtil.success(safetyUserList);
    }

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

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        Long userId = currentUser.getId();
        User user = userService.getById(userId);
        return ResultUtil.success(userService.getSafetyUser(user));
    }

    @PostMapping("/loginOut")
    public BaseResponse<Integer> loginOut(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtil.success(userService.loginOut(request));
    }

    @GetMapping("/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") Integer id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = this.userService.getById(id);
        return ResultUtil.success(user);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> getUpdateUserById(@RequestBody User user, HttpServletRequest request) {
        // 参数校验
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录校验
        User currentUser = userService.getLoginUser(request);
        // 权限校验
        int updateId = userService.updateUser(user, currentUser);
        //删除缓存
        redisTemplate.delete(userService.redisFormat(currentUser.getId()));
        // 返回结果
        return ResultUtil.success(updateId);
    }


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

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) Set<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标签参数错误");
        }
        List<User> userList = userService.searchUserByTags(tagNameList);
        return ResultUtil.success(userList);
    }

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