package com.huixing.fontal.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 发送好友申请请求
 *
 * @author fontal
 */
@Data
public class FriendApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收申请的用户id
     */
    @NotNull(message = "接收用户ID不能为空")
    private Long receiveId;

    /**
     * 好友申请备注信息
     */
    private String remark;
}
