package com.huixing.fontal.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 好友列表返回VO
 *
 * @author fontal
 */
@Data
@ApiModel(value = "好友列表返回VO", description = "用于返回好友列表的包装对象")
public class FriendListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "好友列表")
    private List<FriendVO> friends;

    @ApiModelProperty(value = "好友总数")
    private Integer total;
}
