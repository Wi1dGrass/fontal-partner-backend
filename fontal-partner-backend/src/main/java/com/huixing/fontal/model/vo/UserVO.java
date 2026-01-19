package com.huixing.fontal.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户视图对象（脱敏后的用户信息）
 * 用于接口返回，只包含必要的公开字段
 *
 * @author fontal
 */
@Data
@ApiModel(description = "用户视图对象")
public class UserVO implements Serializable {

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
}
