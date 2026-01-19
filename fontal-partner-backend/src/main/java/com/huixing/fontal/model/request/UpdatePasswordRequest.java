package com.huixing.fontal.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 *
 * @author fontal
 */
@Data
@ApiModel(description = "修改密码请求")
public class UpdatePasswordRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "原密码", required = true, example = "12345678")
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true, example = "87654321", notes = "长度8-20个字符，必须包含字母和数字")
    private String newPassword;

    @ApiModelProperty(value = "确认新密码", required = true, example = "87654321", notes = "必须与新密码一致")
    private String checkPassword;
}
