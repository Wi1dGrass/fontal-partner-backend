package com.huixing.fontal.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍加入申请
 *
 * @author fontal
 * @TableName team_join_apply
 */
@TableName(value = "team_join_apply")
@Data
public class TeamJoinApply implements Serializable {
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 队伍ID
     */
    private Long teamId;

    /**
     * 申请用户ID
     */
    private Long userId;

    /**
     * 队长ID
     */
    private Long leaderId;

    /**
     * 申请类型: 0-用户申请, 1-队伍邀请
     */
    private Integer applyType;

    /**
     * 申请状态: 0-待审批, 1-已通过, 2-已拒绝
     */
    private Integer applyStatus;

    /**
     * 申请留言
     */
    private String applyMessage;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 申请过期时间
     */
    private Date expireTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;
}