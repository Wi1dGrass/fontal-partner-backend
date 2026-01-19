package com.huixing.fontal.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍加入申请/邀请VO（复用：支持申请和邀请）
 *
 * @author fontal
 */
@Data
public class TeamJoinApplyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    private Long id;

    /**
     * 队伍ID
     */
    private Long teamId;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 队伍头像
     */
    private String teamAvatar;

    /**
     * 申请用户ID（申请场景）/ 被邀请用户ID（邀请场景）
     */
    private Long userId;

    /**
     * 申请用户昵称（申请场景）/ 被邀请用户昵称（邀请场景）
     */
    private String userName;

    /**
     * 申请用户头像（申请场景）/ 被邀请用户头像（邀请场景）
     */
    private String userAvatar;

    /**
     * 队长ID（申请场景）/ 邀请人ID（邀请场景）
     */
    private Long leaderId;

    /**
     * 队长昵称（申请场景）/ 邀请人昵称（邀请场景）
     */
    private String leaderName;

    /**
     * 邀请人头像（邀请场景）
     */
    private String leaderAvatar;

    /**
     * 申请类型: 0-用户申请, 1-队伍邀请
     */
    private Integer applyType;

    /**
     * 申请类型描述
     */
    private String applyTypeDesc;

    /**
     * 申请/邀请留言
     */
    private String applyMessage;

    /**
     * 申请状态: 0-待审批, 1-已通过, 2-已拒绝, 3-已取消
     */
    private Integer applyStatus;

    /**
     * 申请状态描述
     */
    private String statusDesc;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 申请时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}