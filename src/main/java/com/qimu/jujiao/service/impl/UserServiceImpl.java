package com.qimu.jujiao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.mapper.UserMapper;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.model.request.UpdateTagRequest;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.qimu.jujiao.contant.UserConstant.ADMIN_ROLE;
import static com.qimu.jujiao.contant.UserConstant.LOGIN_USER_STATUS;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     *盐
     */
    private static final String SALT = "fontal";
    @Resource
    private UserMapper userMapper;

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
        // 3. 密码就不小于8位吧
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码小于8位 ");
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
    public int updateUser(User user, User currentUser) {
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!StringUtils.isAnyBlank(user.getUserDesc()) && user.getUserDesc().length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介不能超过30个字符");
        }
        if (!StringUtils.isAnyBlank(user.getUsername()) && user.getUsername().length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称不能超过20个字符");
        }
        if (!StringUtils.isAnyBlank(user.getContactInfo()) && user.getContactInfo().length() > 18) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "联系方式不能超过18个字符");
        }
        //不能更新账号
        if(user.getUserAccount() != null && user.getUserAccount().equals(currentUser.getUserAccount())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号无法修改");
        }
        //如果修改密码，密码不为空
        if (user.getUserPassword() != null && user.getUserPassword().length() >10){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 如果是管理员，允许更新任意用户
        // 如果不是管理员，只允许更新当前（自己的）信息
        if (!isAdmin(currentUser) && userId != currentUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限");
        }
        // 用户必须不为空
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return userMapper.updateById(user);
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
        Gson gson = new Gson();
        // 在内存中查询符合要求的标签
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagNameStr = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            // 是否为空，为空返回HashSet的默认值，否则返回数值
            tempTagNameStr = Optional.ofNullable(tempTagNameStr).orElse(new HashSet<>());
            // tempTagNameStr集合中每一个元素首字母转换为大写
            tempTagNameStr = tempTagNameStr.stream().map(StringUtils::capitalize).collect(Collectors.toSet());
            // 返回false会过滤掉
            for (String tagName : tagNameList) {
                tagName = StringUtils.capitalize(tagName);
                if (!tempTagNameStr.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
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
        Gson gson = new  Gson();
        Set<String> oldTags = gson.fromJson(user.getTags(), new TypeToken<Set<String>>(){}.getType());
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

}
