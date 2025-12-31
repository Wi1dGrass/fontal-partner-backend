package com.qimu.jujiao.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.qimu.jujiao.common.ErrorCode;
import com.qimu.jujiao.exception.BusinessException;
import com.qimu.jujiao.mapper.UserMapper;
import com.qimu.jujiao.model.entity.User;
import com.qimu.jujiao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

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

    /**
     * 用户脱敏
     *
     * @param originUser 用户信息
     * @return 脱敏后的用户信息
     */

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
}
