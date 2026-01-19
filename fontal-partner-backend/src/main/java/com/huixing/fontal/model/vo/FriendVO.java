package com.huixing.fontal.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友信息VO
 *
 * @author fontal
 */
@Data
@ApiModel(value = "好友信息VO", description = "用于返回好友列表中的单个好友信息")
public class FriendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "好友用户ID")
    private Long id;

    @ApiModelProperty(value = "好友昵称")
    private String name;

    @ApiModelProperty(value = "头像URL")
    private String avatar;

    @ApiModelProperty(value = "账号")
    private String userAccount;

    @ApiModelProperty(value = "个人简介")
    private String userDesc;

    @ApiModelProperty(value = "标签（JSON字符串数组）")
    private String tags;

    @ApiModelProperty(value = "最后一条消息内容")
    private String lastMessage;

    @ApiModelProperty(value = "最后消息时间（格式：HH:mm 或 MM-DD HH:mm）")
    private String lastMessageTime;
}
