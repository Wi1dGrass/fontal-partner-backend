package com.huixing.fontal.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新请求
 * 只允许修改：昵称、头像、性别、联系方式、邮箱、个人简介
 *
 * @author fontal
 */
@Data
@ApiModel(description = "用户更新请求")
public class UserUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户昵称", example = "张三", notes = "最多256个字符")
    private String username;

    @ApiModelProperty(value = "用户头像URL", example = "https://example.com/avatar.jpg", notes = "最多1024个字符")
    private String userAvatarUrl;

    @ApiModelProperty(value = "性别", example = "1", notes = "0-保密，1-男，2-女")
    private Integer gender;

    @ApiModelProperty(value = "联系方式", example = "微信：xxx123", notes = "最多512个字符，不限制格式")
    private String contactInfo;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com", notes = "最多128个字符")
    private String email;

    @ApiModelProperty(value = "个人简介", example = "全栈工程师，热爱编程", notes = "最多512个字符")
    private String userDesc;
}
