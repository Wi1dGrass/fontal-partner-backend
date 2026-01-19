package com.huixing.fontal.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 队伍成员身份VO
 * 用于快速判断用户在队伍中的身份
 *
 * @author fontal
 */
@Data
public class TeamMembershipVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否是队伍成员
     */
    private Boolean isMember;

    /**
     * 角色类型
     * CAPTAIN - 队长
     * MEMBER - 普通成员
     * NON_MEMBER - 非成员
     */
    private String role;

    /**
     * 角色枚举
     */
    public enum Role {
        CAPTAIN("CAPTAIN"),
        MEMBER("MEMBER"),
        NON_MEMBER("NON_MEMBER");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}