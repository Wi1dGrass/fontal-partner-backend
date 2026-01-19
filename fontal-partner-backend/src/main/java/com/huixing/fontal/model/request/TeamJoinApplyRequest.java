package com.huixing.fontal.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 申请加入队伍请求（复用：支持申请和邀请）
 *
 * @author fontal
 */
@Data
public class TeamJoinApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍ID
     */
    @NotNull(message = "队伍ID不能为空")
    private Long teamId;

    /**
     * 被邀请用户ID（邀请场景使用）
     * 当该字段不为空时，表示是邀请场景
     */
    private Long inviteeId;

    /**
     * 申请/邀请留言（可选）
     */
    private String applyMessage;
}