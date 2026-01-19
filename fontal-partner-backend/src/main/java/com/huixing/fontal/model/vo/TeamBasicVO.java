package com.huixing.fontal.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍基础信息VO（非成员可访问）
 *
 * @author fontal
 */
@Data
public class TeamBasicVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 队伍ID
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 队伍头像
     */
    private String teamAvatarUrl;

    /**
     * 队伍描述
     */
    private String teamDesc;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 当前人数
     */
    private Integer currentNum;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 队伍状态: 0-公开, 1-私有, 2-加密
     */
    private Integer teamStatus;

    /**
     * 队伍状态描述
     */
    private String statusDesc;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 队伍公告
     */
    private String announce;

    /**
     * 队长信息
     */
    private CaptainInfo captain;

    /**
     * 队伍标签
     */
    private List<String> tags;

    /**
     * 队伍要求
     */
    private String requirements;

    /**
     * 队长信息
     */
    @Data
    public static class CaptainInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 用户ID
         */
        private Long id;

        /**
         * 用户昵称
         */
        private String username;

        /**
         * 用户账号
         */
        private String userAccount;

        /**
         * 用户头像
         */
        private String userAvatarUrl;

        /**
         * 用户描述
         */
        private String userDesc;
    }
}