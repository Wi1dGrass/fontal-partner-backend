package com.huixing.fontal.model.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 好友申请标记已读请求
 *
 * @author fontal
 */
@Data
@ApiModel(description = "好友申请标记已读请求")
public class FriendMarkReadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请ID（可选，为空则标记所有未读申请为已读）
     */
    @ApiModelProperty(value = "申请ID，为空则标记所有未读申请", example = "123")
    private Long friendId;
}
