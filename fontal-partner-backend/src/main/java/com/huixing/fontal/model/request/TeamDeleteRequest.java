package com.huixing.fontal.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: QiMu
 * @Date: 2023年03月25日 16:35
 * @Version: 1.0
 * @Description: 删除队伍请求
 */
@Data
public class TeamDeleteRequest implements Serializable {
    private static final long serialVersionUID = -6119912852151581286L;
    private Long teamId;
}
