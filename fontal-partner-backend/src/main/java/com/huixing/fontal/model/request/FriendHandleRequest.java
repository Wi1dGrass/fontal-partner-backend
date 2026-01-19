package com.huixing.fontal.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 处理好友申请请求
 *
 * @author fontal
 */
@Data
public class FriendHandleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 好友申请id
     */
    @NotNull(message = "好友申请ID不能为空")
    private Long id;

    /**
     * 处理状态（1-同意 2-拒绝）
     */
    @NotNull(message = "处理状态不能为空")
    private Integer status;
}
