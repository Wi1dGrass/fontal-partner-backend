package com.huixing.fontal.controller;

import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.common.ErrorCode;
import com.huixing.fontal.common.ResultUtil;
import com.huixing.fontal.exception.BusinessException;
import com.huixing.fontal.model.vo.TeamVo;
import com.huixing.fontal.service.TeamService;
import com.huixing.fontal.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/team")
public class TeamController {
    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @GetMapping("/{teamId}")
    public BaseResponse<TeamVo> getUsersByTeamId(@PathVariable("teamId") Long teamId, HttpServletRequest request) {
        if(teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"未加入队伍");
        }
        TeamVo teamVo = teamService.getUsersByTeamId(teamId,request);
        return ResultUtil.success(teamVo);
    }

}
