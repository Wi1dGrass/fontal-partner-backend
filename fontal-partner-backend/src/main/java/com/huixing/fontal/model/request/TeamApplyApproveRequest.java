package com.huixing.fontal.model.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 审批队伍加入申请请求
 *
 * @author fontal
 */
@Data
public class TeamApplyApproveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    /**
     * 审批状态: 1-通过, 2-拒绝
     */
    @NotNull(message = "审批状态不能为空")
    private Integer status;

    /**
     * 拒绝原因（可选，拒绝时建议填写）
     */
    private String rejectReason;
}