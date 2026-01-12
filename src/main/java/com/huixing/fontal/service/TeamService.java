package com.huixing.fontal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.huixing.fontal.model.entity.Team;
import com.huixing.fontal.model.vo.TeamVo;

import javax.servlet.http.HttpServletRequest;

public interface TeamService extends IService<Team> {
    TeamVo getUsersByTeamId(Long teamId, HttpServletRequest request);
}
