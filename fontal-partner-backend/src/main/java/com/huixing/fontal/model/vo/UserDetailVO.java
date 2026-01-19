package com.huixing.fontal.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户详情视图对象（脱敏后的用户完整信息）
 * 用于用户详情页面，包含基础信息和联系方式
 *
 * @author fontal
 */
@Data
@ApiModel(description = "用户详情视图对象")
public class UserDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "用户昵称", example = "张三")
    private String username;

    @ApiModelProperty(value = "用户头像URL", example = "https://example.com/avatar.jpg")
    private String userAvatarUrl;

    @ApiModelProperty(value = "性别", example = "1", notes = "0-保密，1-男，2-女")
    private Integer gender;

    @ApiModelProperty(value = "个人简介", example = "全栈工程师，热爱编程")
    private String profile;

    @ApiModelProperty(value = "用户标签（JSON字符串）", example = "[\"Java\", \"Python\"]", notes = "前端负责解析")
    private String tags;

    @ApiModelProperty(value = "联系方式", example = "微信：xxx123")
    private String contactInfo;

    @ApiModelProperty(value = "邮箱", example = "zhangsan@example.com")
    private String email;
}
