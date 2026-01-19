package com.huixing.fontal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.common.PageResult;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.mapper.UserMapper;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.UpdatePasswordRequest;
import com.huixing.fontal.model.request.UpdateTagRequest;
import com.huixing.fontal.model.request.UserUpdateRequest;
import com.huixing.fontal.model.vo.UserDetailVO;
import com.huixing.fontal.model.vo.UserVO;
import com.huixing.fontal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.huixing.fontal.contant.UserConstant.ADMIN_ROLE;
import static com.huixing.fontal.contant.UserConstant.LOGIN_USER_STATUS;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     *盐
     */
    private static final String SALT = "fontal";
    @Resource
    private UserMapper userMapper;

    private static final Gson GSON = new Gson();
    private static final Type TAG_SET_TYPE = new TypeToken<Set<String>>() {
    }.getType();

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //参数校验
        //1.非空
        if (StringUtils.isAnyBlank(userAccount,userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"输入不为空");
        }
        // 2. 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        // 2. 账户长度不大于16位
        if (userAccount.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能大于16位");
        }
        // 3. 密码就不小于6位吧
        if (userPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于6位 ");
        }
        //  5. 账户不包含特殊字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }
        //进行加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        //进行查数据库
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        userQueryWrapper.eq("userPassword",encryptPassword);
        User user = this.getOne(userQueryWrapper);
        //判断用户是否存在
        if(user == null){
            log.info("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名或者密码错误");
        }
        User safeUser = getSafetyUser(user);
        request.getSession().setAttribute(LOGIN_USER_STATUS,safeUser);
        return safeUser;
    }

    @Override
    public long  userRegistration(String username, String userAccount, String userPassword, String checkPassword) {
        //参数校验
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入不能为空");
        }
        // 2. 账户长度不小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        if (!StringUtils.isAnyBlank(username) && username.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能超过20个字符");
        }
        // 2. 账户长度不大于16位
        if (userAccount.length() > 16) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能能大于16位");
        }
        // 3. 密码就不小于8位吧
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于8位");
        }
        //  5. 账户不包含特殊字符
        // 匹配由数字、小写字母、大写字母组成的字符串,且字符串的长度至少为1个字符
        String pattern = "[0-9a-zA-Z]+";
        if (!userAccount.matches(pattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        // 6. 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入密码不一致");
        }
        //账号不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        long count = this.count(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号已存在");
        }
        //密码加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        //进行封装
        User user = new User();
        user.setUsername(username);
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setTags("[]");
        user.setTeamIds("[]");
        user.setUserIds("[]");
        //开始注册
        boolean saveResult = this.save(user);
        //校验
        if(!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"注册失败");
        }
        return user.getId();
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        //获取当前登入用户
        Object objUser = request.getSession().getAttribute(LOGIN_USER_STATUS);
        User currentUser = (User) objUser;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请先登入");
        }
        return currentUser;
    }



    /**
     * 用户脱敏
     *
     * @param originUser 用户信息
     * @return 脱敏后的用户信息
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            return null;
        }
        User safeUser = new User();
        safeUser.setId(originUser.getId());
        safeUser.setUsername(originUser.getUsername());
        safeUser.setUserAccount(originUser.getUserAccount());
        safeUser.setUserAvatarUrl(originUser.getUserAvatarUrl());
        safeUser.setGender(originUser.getGender());
        safeUser.setEmail(originUser.getEmail());
        safeUser.setContactInfo(originUser.getContactInfo());
        safeUser.setUserDesc(originUser.getUserDesc());
        safeUser.setUserStatus(originUser.getUserStatus());
        safeUser.setUserRole(originUser.getUserRole());
        safeUser.setUserIds(originUser.getUserIds());
        safeUser.setTags(originUser.getTags());
        safeUser.setTeamIds(originUser.getTeamIds());
        safeUser.setCreateTime(originUser.getCreateTime());
        return safeUser;
    }

    @Override
    public Integer loginOut(HttpServletRequest request) {
        request.getSession().removeAttribute(LOGIN_USER_STATUS);
        return 1;
    }

    @Override
    public Boolean updateUser(UserUpdateRequest updateUserRequest, User currentUser) {
        // 1. 从数据库重新获取用户信息，确保数据一致性
        User userFromDb = this.getById(currentUser.getId());
        if (userFromDb == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        // 2. 更新昵称
        if (StringUtils.isNotBlank(updateUserRequest.getUsername())) {
            if (updateUserRequest.getUsername().length() > 256) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度不能超过256个字符");
            }
            userFromDb.setUsername(updateUserRequest.getUsername());
        }

        // 3. 更新头像
        if (StringUtils.isNotBlank(updateUserRequest.getUserAvatarUrl())) {
            if (updateUserRequest.getUserAvatarUrl().length() > 1024) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像URL长度不能超过1024个字符");
            }
            userFromDb.setUserAvatarUrl(updateUserRequest.getUserAvatarUrl());
        }

        // 4. 更新性别（0-保密，1-男，2-女）
        if (updateUserRequest.getGender() != null) {
            if (updateUserRequest.getGender() < 0 || updateUserRequest.getGender() > 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "性别值不合法，应为0-保密，1-男，2-女");
            }
            userFromDb.setGender(updateUserRequest.getGender());
        }

        // 5. 更新联系方式（不校验格式）
        if (StringUtils.isNotBlank(updateUserRequest.getContactInfo())) {
            if (updateUserRequest.getContactInfo().length() > 512) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "联系方式长度不能超过512个字符");
            }
            userFromDb.setContactInfo(updateUserRequest.getContactInfo());
        }

        // 6. 更新邮箱
        if (StringUtils.isNotBlank(updateUserRequest.getEmail())) {
            if (updateUserRequest.getEmail().length() > 128) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱长度不能超过128个字符");
            }
            // 简单的邮箱格式校验
            if (!updateUserRequest.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            }
            userFromDb.setEmail(updateUserRequest.getEmail());
        }

        // 7. 更新个人简介
        if (StringUtils.isNotBlank(updateUserRequest.getUserDesc())) {
            if (updateUserRequest.getUserDesc().length() > 512) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介不能超过512个字符");
            }
            userFromDb.setUserDesc(updateUserRequest.getUserDesc());
        }

        // 8. 执行更新
        return this.updateById(userFromDb);
    }

    @Override
    public Boolean updatePassword(UpdatePasswordRequest updatePasswordRequest, User currentUser) {
        // 1. 参数校验
        if (updatePasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        String oldPassword = updatePasswordRequest.getOldPassword();
        String newPassword = updatePasswordRequest.getNewPassword();
        String checkPassword = updatePasswordRequest.getCheckPassword();

        // 2. 非空校验
        if (StringUtils.isBlank(oldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码不能为空");
        }
        if (StringUtils.isBlank(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能为空");
        }
        if (StringUtils.isBlank(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "确认密码不能为空");
        }

        // 3. 新密码和确认密码一致性校验
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }

        // 4. 新密码长度校验（6-20个字符）
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度必须在6-20个字符之间");
        }

        // 5. 新密码复杂度校验（必须包含字母和数字）
        if (!newPassword.matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码必须包含字母和数字");
        }

        // 6. 新密码不能与原密码相同
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与原密码相同");
        }

        // 7. 从数据库重新获取用户信息，确保数据一致性
        User userFromDb = this.getById(currentUser.getId());
        if (userFromDb == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }

        // 8. 验证原密码是否正确
        String encryptedOldPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes(StandardCharsets.UTF_8));
        if (!encryptedOldPassword.equals(userFromDb.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "原密码错误");
        }

        // 9. 加密新密码
        String encryptedNewPassword = DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes(StandardCharsets.UTF_8));

        // 10. 更新密码
        userFromDb.setUserPassword(encryptedNewPassword);

        // 11. 执行更新并记录日志
        boolean result = this.updateById(userFromDb);
        if (result) {
            log.info("用户 {} 修改密码成功", currentUser.getId());
        }

        return result;
    }

    @Override
    public boolean isAdmin(User currentUser) {
        return currentUser != null && currentUser.getUserRole() == ADMIN_ROLE;
    }

//    @Override
//    public List<User> searchBySQL(List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        long starTime = System.currentTimeMillis();
//        //直接更具sql查找
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        //拼接tag
//        for (String tags : tagNameList) {
//            queryWrapper = queryWrapper.like("tags",tags);
//        }
//        List<User> userList = userMapper.selectList(queryWrapper);
//        log.info("sql query time = " + (System.currentTimeMillis() - starTime));
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<User> searchCache(List<String> tagNameList) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        //先查找所有用户
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        long starTime = System.currentTimeMillis();
//        List<User> userList = userMapper.selectList(queryWrapper);
//        Gson gson = new Gson();
//        //判断内存是否包含要求的标签
//        userList.stream().filter(user -> {
//            String tagstr = user.getTags();
//            if (StringUtils.isBlank(tagstr)){
//                return false;
//            }
//            Set<String> tempTagNameSet =  gson.fromJson(tagstr,new TypeToken<Set<String>>(){}.getType());
//            for (String tagName : tagNameList){
//                if (!tempTagNameSet.contains(tagName)){
//                    return false;
//                }
//            }
//            return true;
//        }).map(this::getSafetyUser).collect(Collectors.toList());
//        log.info("memory query time = " + (System.currentTimeMillis() - starTime));
//        return  userList;
//    }

    @Override
    public List<User> searchUserByTags(Set<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询出所有的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        log.info("标签搜索：数据库共有 {} 个用户", userList.size());

        Gson gson = new Gson();
        // 在内存中查询符合要求的标签
        List<User> matchedUsers = userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)) {
                return false;
            }
            Set<String> tempTagNameStr = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            // 是否为空，为空返回HashSet的默认值，否则返回数值
            tempTagNameStr = Optional.ofNullable(tempTagNameStr).orElse(new HashSet<>());
            // tempTagNameStr集合中每一个元素首字母转换为大写
            tempTagNameStr = tempTagNameStr.stream().map(StringUtils::capitalize).collect(Collectors.toSet());

            // 返回false会过滤掉（AND 匹配：必须包含所有标签）
            for (String tagName : tagNameList) {
                tagName = StringUtils.capitalize(tagName);
                if (!tempTagNameStr.contains(tagName)) {
                    return false;
                }
            }
            log.debug("标签搜索：用户 {} 匹配成功，用户标签: {}", user.getId(), tempTagNameStr);
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());

        log.info("标签搜索：匹配到 {} 个用户，搜索条件: {}", matchedUsers.size(), tagNameList);
        return matchedUsers;
    }

    /**
     * 根据标签搜索用户（分页版本）
     */
    @Override
    public PageResult<UserVO> searchUserByTags(Set<String> tagNameList, int pageNum, int pageSize) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询出所有的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        log.info("标签搜索：数据库共有 {} 个用户", userList.size());

        Gson gson = new Gson();
        // 在内存中查询符合要求的标签
        List<User> matchedUsers = userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            if (StringUtils.isBlank(tagsStr)) {
                return false;
            }
            Set<String> tempTagNameStr = gson.fromJson(tagsStr, TAG_SET_TYPE);
            // 是否为空，为空返回HashSet的默认值，否则返回数值
            tempTagNameStr = Optional.ofNullable(tempTagNameStr).orElse(new HashSet<>());
            // tempTagNameStr集合中每一个元素首字母转换为大写
            tempTagNameStr = tempTagNameStr.stream().map(StringUtils::capitalize).collect(Collectors.toSet());

            // 返回false会过滤掉（AND 匹配：必须包含所有标签）
            for (String tagName : tagNameList) {
                tagName = StringUtils.capitalize(tagName);
                if (!tempTagNameStr.contains(tagName)) {
                    return false;
                }
            }
            log.debug("标签搜索：用户 {} 匹配成功，用户标签: {}", user.getId(), tempTagNameStr);
            return true;
        }).collect(Collectors.toList());

        long total = matchedUsers.size();
        log.info("标签搜索：匹配到 {} 个用户，搜索条件: {}", total, tagNameList);

        // 分页处理
        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, (int) total);

        List<UserVO> pageRecords = new ArrayList<>();
        if (fromIndex < total && fromIndex >= 0) {
            pageRecords = matchedUsers.subList(fromIndex, toIndex)
                    .stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        }

        return new PageResult<>(pageRecords, total, pageNum, pageSize);
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object objUser = request.getSession().getAttribute(LOGIN_USER_STATUS);
        User user = (User) objUser;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    @Override
    public String redisFormat(Long id) {
        return String.format("fontal:user:search:%s",id);
    }

    private Set<String> toCapitalize(Set<String> oldSet) {
        return oldSet.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
    }

    @Override
    public int updateTageById(UpdateTagRequest tagRequest, User currentUser) {
        long id = tagRequest.getId();
        if (id <= 0){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"该用户不存在");
        }
        Set<String> newTags = tagRequest.getTagList();
        if(newTags.size() > 12) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"最多设置12个标签");
        }
        if (!isAdmin(currentUser) && id != currentUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH,"无权限");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }
        Gson gson = new  Gson();
        String userTags = user.getTags();
        Set<String> oldTags = new HashSet<>();
        if (StringUtils.isNotBlank(userTags)) {
            oldTags = gson.fromJson(userTags, new TypeToken<Set<String>>(){}.getType());
            if (oldTags == null) {
                oldTags = new HashSet<>();
            }
        }
        Set<String> oldTagsCapitalize = toCapitalize(oldTags);
        Set<String> newTagsCapitalize = toCapitalize(newTags);
        // 添加 newTagsCapitalize 中 oldTagsCapitalize 中不存在的元素
        oldTagsCapitalize.addAll(newTagsCapitalize.stream().filter(tag->!oldTagsCapitalize.contains(tag)).collect(Collectors.toList()));
        // 移除 oldTagsCapitalize 中 newTagsCapitalize 中不存在的元素
        oldTagsCapitalize.removeAll(oldTagsCapitalize.stream().filter(tag -> !newTagsCapitalize.contains(tag)).collect(Collectors.toSet()));
        String tagsJson = gson.toJson(oldTagsCapitalize);
        user.setTags(tagsJson);
        return userMapper.updateById(user);
    }

    @Override
    public List<User> computeMatchUsers(User loginUser) {
        String loginUserTags = loginUser.getTags();
        if (StringUtils.isBlank(loginUserTags)) {
            return new ArrayList<>();
        }

        // 【优化】预解析并标准化登录用户标签，避免在循环中重复执行
        Set<String> loginTagSet = toCapitalize(GSON.fromJson(loginUserTags, TAG_SET_TYPE));

        // 【优化】SQL 粗筛：只查 ID 和 Tags，且根据标签关键词筛选，减少 90% 数据量
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags"); // 极简字段查询
        queryWrapper.ne("id", loginUser.getId());
        queryWrapper.eq("userStatus", 0);
        queryWrapper.and(qw -> {
            for (String tag : loginTagSet) {
                qw.or().like("tags", tag);
            }
        });

        List<User> allCandidates = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(allCandidates)) {
            return new ArrayList<>();
        }

        // 【优化】使用并行流处理 CPU 密集型计算（JSON 解析 & 交集计算）
        List<Long> topIds = allCandidates.parallelStream()
                .map(targetUser -> {
                    String tags = targetUser.getTags();
                    if (StringUtils.isBlank(tags)) {
                        return new AbstractMap.SimpleEntry<>(targetUser.getId(), 0L);
                    }
                    Set<String> targetTagSet = toCapitalize(GSON.fromJson(tags, TAG_SET_TYPE));
                    // 计算交集分数
                    long score = loginTagSet.stream().filter(targetTagSet::contains).count();
                    return new AbstractMap.SimpleEntry<>(targetUser.getId(), score);
                })
                .filter(entry -> entry.getValue() > 0)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 分数降序
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 【优化】批量回表补全完整用户信息，避免在脱敏循环中多次查询数据库
        if (CollectionUtils.isEmpty(topIds)) {
            return new ArrayList<>();
        }
        return this.listByIds(topIds);
    }

    /**
     * 根据搜索文本查询用户（分页版本）
     */
    @Override
    public PageResult<UserVO> searchUsersByText(String searchText, int pageNum, int pageSize) {
        // 参数校验
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页数量必须在1-100之间");
        }
        if (pageNum < 1) {
            pageNum = 1;
        }

        // 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(searchText)) {
            // 模糊匹配多个字段：用户名、标签、个人简介
            queryWrapper.and(qw -> qw.like("username", searchText)
                    .or().like("tags", searchText)
                    .or().like("userDesc", searchText));
        }

        // 查询总数
        long total = this.count(queryWrapper);

        // 分页查询
        int fromIndex = (pageNum - 1) * pageSize;
        queryWrapper.last("LIMIT " + pageSize + " OFFSET " + fromIndex);
        List<User> userList = this.list(queryWrapper);

        // 转换为UserVO
        List<UserVO> userVOList = userList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(userVOList, total, pageNum, pageSize);
    }

    @Override
    public List<UserVO> getRecommendUsers(Long userId, HttpServletRequest request, int limit) {
        long startTime = System.currentTimeMillis();

        // 1. 参数校验
        if (limit < 1 || limit > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "返回数量必须在1-50之间");
        }

        // 2. 游客模式：随机返回
        if (userId == null) {
            List<User> randomUsers = this.list(
                new QueryWrapper<User>()
                    .select("id", "username", "userAvatarUrl", "gender", "userDesc", "tags")
                    .eq("userStatus", 0)
                    .last("ORDER BY RAND() LIMIT " + limit)
            );

            log.info("游客模式推荐: 返回{}个用户, 耗时{}ms", randomUsers.size(),
                System.currentTimeMillis() - startTime);

            return randomUsers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        }

        // 3. 登录模式
        User loginUser = this.getById(userId);
        if (loginUser == null) {
            return new ArrayList<>();
        }

        // 4. 获取Session中的"固定用户"（第一次的最匹配用户）
        Long fixedUserId = (Long) request.getSession().getAttribute("FIXED_RECOMMEND_USER");

        // 5. 如果没有固定用户，计算并存储
        if (fixedUserId == null) {
            List<User> topMatches = computeMatchUsers(loginUser);
            if (!CollectionUtils.isEmpty(topMatches)) {
                fixedUserId = topMatches.get(0).getId();
                request.getSession().setAttribute("FIXED_RECOMMEND_USER", fixedUserId);
                request.getSession().setMaxInactiveInterval(30 * 60); // 30分钟过期
                log.info("首次推荐: 固定最匹配用户 userId={}", fixedUserId);
            }
        }

        // 6. 构建结果列表
        List<UserVO> result = new ArrayList<>();

        // 6.1 添加固定用户（第一位）
        if (fixedUserId != null) {
            User fixedUser = this.getById(fixedUserId);
            if (fixedUser != null && fixedUser.getUserStatus() == 0) {
                result.add(convertToVO(fixedUser));
            }
        }

        // 6.2 随机获取其他用户（排除自己和固定用户）
        QueryWrapper<User> randomWrapper = new QueryWrapper<>();
        randomWrapper.select("id", "username", "userAvatarUrl", "gender", "userDesc", "tags");
        randomWrapper.ne("id", userId); // 排除自己
        if (fixedUserId != null) {
            randomWrapper.ne("id", fixedUserId); // 排除固定用户
        }
        randomWrapper.eq("userStatus", 0);
        randomWrapper.last("ORDER BY RAND() LIMIT " + (limit - 1));

        List<User> randomUsers = this.list(randomWrapper);
        randomUsers.forEach(user -> result.add(convertToVO(user)));

        log.info("登录用户推荐: userId={}, 返回{}个用户, 固定userId={}, 耗时{}ms",
            userId, result.size(), fixedUserId, System.currentTimeMillis() - startTime);

        return result;
    }

    /**
     * 将User实体转换为UserVO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setUserAvatarUrl(user.getUserAvatarUrl());
        vo.setGender(user.getGender());
        vo.setProfile(user.getUserDesc());
        vo.setTags(user.getTags()); // 直接返回JSON字符串，不解析
        return vo;
    }

    /**
     * 将User实体转换为UserDetailVO
     */
    private UserDetailVO convertToDetailVO(User user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setUserAvatarUrl(user.getUserAvatarUrl());
        vo.setGender(user.getGender());
        vo.setProfile(user.getUserDesc());
        vo.setTags(user.getTags()); // 直接返回JSON字符串，不解析
        vo.setContactInfo(user.getContactInfo());
        vo.setEmail(user.getEmail());
        return vo;
    }

    @Override
    public UserDetailVO getUserDetailById(Long userId) {
        // 1. 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 2. 查询用户
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "用户不存在");
        }

        // 3. 转换为UserDetailVO
        return convertToDetailVO(user);
    }

}
