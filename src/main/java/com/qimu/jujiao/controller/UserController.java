package com.qimu.jujiao.controller;

import com.alibaba.druid.sql.visitor.functions.If;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qimu.jujiao.common.BaseResponse;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.common.ResultUtil;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.UpdateTagRequest;
import com.qimu.jujiao.model.request.UserLoginRequest;
import com.qimu.jujiao.model.request.UserRegisterRequest;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
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
    private RedisTemplate<String,Object> redisTemplate;

    @PostMapping("/login")
    public BaseResponse<User> login(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {
        //进行参数校验
        if (loginRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();
        User user = userService.userLogin(userAccount,userPassword,request);
        return ResultUtil.success(user,"登入成功");
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
        if(StringUtils.isAllBlank(username,userAccount,userPassword,checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegistration(username,userAccount,userPassword,checkPassword);
        return ResultUtil.success(result,"注册成功");
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = userService.getLoginUser(request);
        Long userId = currentUser.getId();
        User user = userService.getById(userId);
        return  ResultUtil.success(userService.getSafetyUser(user));
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
    public BaseResponse<Boolean> deleteUser(Long id,HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH,"无权限");
        }
        if(id == null || id < 0) {
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
    public BaseResponse<Integer> updateTagById(@RequestBody UpdateTagRequest tagRequest,HttpServletRequest request) {
        if(tagRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User currentUser = userService.getLoginUser(request);
        int updateTag = userService.updateTageById(tagRequest,currentUser);
        redisTemplate.delete(userService.redisFormat(tagRequest.getId()));
        return ResultUtil.success(updateTag);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchList(HttpServletRequest request) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        //1.尝试获取用户
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception ignored) {}

        //2.确定缓存key
        String cacheKey = (loginUser == null) ? "fontal:user:guest" : userService.redisFormat(loginUser.getId());

        //3.访问缓存
        List<User> userList = (List<User>) valueOperations.get(cacheKey);

        //4.缓存没有命中，进行查询
        if (userList == null) {
            if (loginUser == null) {
                userList = userService.list(new QueryWrapper<User>().last("ORDER BY RAND() LIMIT 10"));
            } else {
                // 登录模式：计算标签相似度
                List<User> allCandidates = userService.list(new QueryWrapper<User>()
                        .ne("id", loginUser.getId()) // 排除自己
                        .eq("userStatus", 0));

                String loginUserTags = loginUser.getTags();

                // 核心算法逻辑：流式计算匹配分并排序,使用 Stream 处理并根据分值排序，不依赖实体类字段
                userList = allCandidates.stream()
                        .map(targetUser -> {
                            // 1. 计算分值
                            long score = calculateMatchScore(loginUserTags, targetUser.getTags());
                            // 2. 将用户和分值存入简单 Entry (Map.Entry)
                            return new AbstractMap.SimpleEntry<>(targetUser, score);
                        })
                        .filter(entry -> entry.getValue() > 0) // 过滤掉完全不匹配的
                        .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 降序排序
                        .limit(20) // 取前 20 名
                        .map(Map.Entry::getKey) // 还原回 User 对象
                        .collect(Collectors.toList());
            }

            // 如果查完还是空（比如库里没人），给个空列表防止后续 NPE
            if (userList == null) {
                userList = new ArrayList<>();
            }
        }
        //进行用户脱敏
        List<User> safetyUserList = new ArrayList<>();
        for (User safetyUser : userList) {
            safetyUser = userService.getSafetyUser(safetyUser);
            safetyUserList.add(safetyUser);
        }
        valueOperations.set(cacheKey,safetyUserList,1, TimeUnit.MINUTES);
        //5.刷新页面
        if (!CollectionUtils.isEmpty(safetyUserList)) {
            User firstUser = safetyUserList.get(0);
            List<User> others = new ArrayList<>(safetyUserList.subList(1,safetyUserList.size()));
            Collections.shuffle(others);

            List<User> finalResult = new ArrayList<>();
            finalResult.add(firstUser);
            finalResult.addAll(others);
            return ResultUtil.success(finalResult);
        }
        return ResultUtil.success(new ArrayList<>());
    }
    private long calculateMatchScore(String currentTagsStr, String targetTagsStr) {
        if (StringUtils.isAnyBlank(currentTagsStr, targetTagsStr)) {
            return 0;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<Set<String>>(){}.getType();

        // 1. 解析并标准化标签 (转大写确保匹配)
        Set<String> currentTags = toCapitalize(gson.fromJson(currentTagsStr, type));
        Set<String> targetTags = toCapitalize(gson.fromJson(targetTagsStr, type));

        // 2. 计算交集 (当前用户标签中 包含 目标用户标签 的数量)
        return currentTags.stream()
                .filter(targetTags::contains)
                .count();
    }

    private Set<String> toCapitalize(Set<String> oldSet) {
        return oldSet.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
    }
}
