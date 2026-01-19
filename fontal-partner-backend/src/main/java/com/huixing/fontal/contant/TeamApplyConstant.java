package com.huixing.fontal.contant;

/**
 * 队伍加入申请常量
 *
 * @author fontal
 */
public interface TeamApplyConstant {

    /**
     * 申请状态：待审批
     */
    int APPLY_STATUS_PENDING = 0;

    /**
     * 申请状态：已通过
     */
    int APPLY_STATUS_APPROVED = 1;

    /**
     * 申请状态：已拒绝
     */
    int APPLY_STATUS_REJECTED = 2;

    /**
     * 申请状态：已取消
     */
    int APPLY_STATUS_CANCELLED = 3;

    /**
     * 申请过期时间（默认7天，单位：天）
     */
    int APPLY_EXPIRE_DAYS = 7;

    /**
     * 防止重复申请的时间间隔（60秒，单位：秒）
     */
    int REAPPLY_INTERVAL_SECONDS = 60;

    /**
     * 每个队伍最多保留的待审批申请数量
     */
    int MAX_PENDING_APPLIES_PER_TEAM = 100;
}