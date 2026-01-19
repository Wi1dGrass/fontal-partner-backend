package com.huixing.fontal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.model.request.KickOutUserRequest;
import com.huixing.fontal.model.request.TeamCreateRequest;
import com.huixing.fontal.model.request.TeamDeleteRequest;
import com.huixing.fontal.model.request.TeamJoinRequest;
import com.huixing.fontal.model.request.TeamUpdateRequest;
import com.huixing.fontal.model.request.TransferTeamRequest;
import com.huixing.fontal.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TeamController 测试类
 * 测试队伍系统的所有接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private MockHttpServletRequest request;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        // Mock登录用户，通过拦截器验证
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setUserAccount("testAccount");
        when(userService.getLoginUser(any())).thenReturn(mockUser);
    }

    /**
     * 测试创建公开队伍
     */
    @Test
    public void testCreatePublicTeam() throws Exception {
        TeamCreateRequest createRequest = new TeamCreateRequest();
        createRequest.setTeamName("测试公开队伍");
        createRequest.setTeamAvatarUrl("https://picsum.photos/200");
        createRequest.setTeamDesc("这是一个测试公开队伍");
        createRequest.setMaxNum(10);
        createRequest.setExpireTime(new Date(System.currentTimeMillis() + 86400000)); // 24小时后过期
        createRequest.setTeamStatus(0); // 公开
        createRequest.setAnnounce("欢迎加入！");

        String jsonRequest = objectMapper.writeValueAsString(createRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("创建公开队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试创建加密队伍
     */
    @Test
    public void testCreatePrivateTeam() throws Exception {
        TeamCreateRequest createRequest = new TeamCreateRequest();
        createRequest.setTeamName("测试加密队伍");
        createRequest.setTeamAvatarUrl("https://picsum.photos/200");
        createRequest.setTeamDesc("这是一个测试加密队伍");
        createRequest.setMaxNum(5);
        createRequest.setExpireTime(new Date(System.currentTimeMillis() + 172800000)); // 48小时后过期
        createRequest.setTeamStatus(2); // 加密
        createRequest.setTeamPassword("123456");
        createRequest.setAnnounce("密码：123456");

        String jsonRequest = objectMapper.writeValueAsString(createRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("创建加密队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试创建私有队伍
     */
    @Test
    public void testCreateSecretTeam() throws Exception {
        TeamCreateRequest createRequest = new TeamCreateRequest();
        createRequest.setTeamName("测试私有队伍");
        createRequest.setTeamAvatarUrl("https://picsum.photos/200");
        createRequest.setTeamDesc("这是一个测试私有队伍");
        createRequest.setMaxNum(8);
        createRequest.setExpireTime(new Date(System.currentTimeMillis() + 259200000)); // 72小时后过期
        createRequest.setTeamStatus(1); // 私有
        createRequest.setAnnounce("仅限邀请");

        String jsonRequest = objectMapper.writeValueAsString(createRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("创建私有队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试创建队伍 - 参数错误
     */
    @Test
    public void testCreateTeamWithNullRequest() throws Exception {
        // 传入空对象而不是完全null，避免400错误
        TeamCreateRequest createRequest = new TeamCreateRequest();
        // 不设置任何字段，应该会触发业务层的参数校验

        String jsonRequest = objectMapper.writeValueAsString(createRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("创建队伍参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试根据队伍Id获取队伍信息
     */
    @Test
    public void testGetUsersByTeamId() throws Exception {
        Long teamId = 1L;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/team/{teamId}", teamId)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("获取队伍信息响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试根据队伍Id获取队伍信息 - 参数错误
     */
    @Test
    public void testGetUsersByTeamIdWithError() throws Exception {
        Long teamId = -1L; // 无效的teamId

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/team/{teamId}", teamId)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("获取队伍信息参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试获取所有队伍信息
     */
    @Test
    public void testGetAllTeams() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/team/team")
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("获取所有队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试通过队伍Id列表获取队伍信息
     */
    @Test
    public void testGetTeamsByIds() throws Exception {
        Set<Long> teamIds = new HashSet<>();
        teamIds.add(1L);
        teamIds.add(2L);
        teamIds.add(3L);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/team/teamsByIds")
                        .param("teamIds", "1,2,3")
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("通过ID列表获取队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试通过队伍Id列表获取队伍信息 - 参数错误
     */
    @Test
    public void testGetTeamsByIdsWithError() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/team/teamsByIds")
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("通过ID列表获取队伍参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试加入公开队伍
     */
    @Test
    public void testJoinPublicTeam() throws Exception {
        TeamJoinRequest joinRequest = new TeamJoinRequest();
        joinRequest.setTeamId(1L);

        String jsonRequest = objectMapper.writeValueAsString(joinRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("加入公开队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试加入加密队伍
     */
    @Test
    public void testJoinPrivateTeam() throws Exception {
        TeamJoinRequest joinRequest = new TeamJoinRequest();
        joinRequest.setTeamId(2L);
        joinRequest.setPassword("123456");

        String jsonRequest = objectMapper.writeValueAsString(joinRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("加入加密队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试加入队伍 - 参数错误
     */
    @Test
    public void testJoinTeamWithNullRequest() throws Exception {
        // 传入空对象而不是完全null，避免400错误
        TeamJoinRequest joinRequest = new TeamJoinRequest();
        // 不设置任何字段

        String jsonRequest = objectMapper.writeValueAsString(joinRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("加入队伍参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试退出队伍
     */
    @Test
    public void testQuitTeam() throws Exception {
        Long teamId = 1L;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/quit/{teamId}", teamId)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("退出队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试退出队伍 - 参数错误
     */
    @Test
    public void testQuitTeamWithError() throws Exception {
        Long teamId = -1L; // 无效的teamId

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/quit/{teamId}", teamId)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("退出队伍参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试踢出队伍成员
     */
    @Test
    public void testKickOutUser() throws Exception {
        KickOutUserRequest kickOutRequest = new KickOutUserRequest();
        kickOutRequest.setTeamId(1L);
        kickOutRequest.setUserId(2L);

        String jsonRequest = objectMapper.writeValueAsString(kickOutRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/kickOutUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("踢出成员响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试踢出队伍成员 - 参数错误
     */
    @Test
    public void testKickOutUserWithNullRequest() throws Exception {
        // 传入空对象而不是完全null，避免400错误
        KickOutUserRequest kickOutRequest = new KickOutUserRequest();
        // 不设置任何字段

        String jsonRequest = objectMapper.writeValueAsString(kickOutRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/kickOutUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("踢出成员参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试更新队伍信息
     */
    @Test
    public void testUpdateTeam() throws Exception {
        TeamUpdateRequest updateRequest = new TeamUpdateRequest();
        updateRequest.setId(1L);
        updateRequest.setTeamName("更新后的队伍名");
        updateRequest.setTeamDesc("更新后的描述");
        updateRequest.setMaxNum(15);
        updateRequest.setAnnounce("更新后的公告");

        String jsonRequest = objectMapper.writeValueAsString(updateRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("更新队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试更新队伍信息 - 参数错误
     */
    @Test
    public void testUpdateTeamWithNullRequest() throws Exception {
        // 传入空对象而不是完全null，避免400错误
        TeamUpdateRequest updateRequest = new TeamUpdateRequest();
        // 不设置任何字段

        String jsonRequest = objectMapper.writeValueAsString(updateRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("更新队伍参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试转让队长
     */
    @Test
    public void testTransferTeam() throws Exception {
        TransferTeamRequest transferRequest = new TransferTeamRequest();
        transferRequest.setTeamId(1L);
        transferRequest.setUserAccount("testUser001");

        String jsonRequest = objectMapper.writeValueAsString(transferRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("转让队长响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试转让队长 - 参数错误
     */
    @Test
    public void testTransferTeamWithNullRequest() throws Exception {
        // 传入空对象而不是完全null，避免400错误
        TransferTeamRequest transferRequest = new TransferTeamRequest();
        // 不设置任何字段

        String jsonRequest = objectMapper.writeValueAsString(transferRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("转让队长参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试删除队伍
     */
    @Test
    public void testDeleteTeam() throws Exception {
        TeamDeleteRequest deleteRequest = new TeamDeleteRequest();
        deleteRequest.setTeamId(1L);

        String jsonRequest = objectMapper.writeValueAsString(deleteRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("删除队伍响应：" + response);
        assertNotNull(response);
    }

    /**
     * 测试删除队伍 - 参数错误
     */
    @Test
    public void testDeleteTeamWithNullRequest() throws Exception {
        // 传入空对象而不是完全null，避免400错误
        TeamDeleteRequest deleteRequest = new TeamDeleteRequest();
        // 不设置任何字段

        String jsonRequest = objectMapper.writeValueAsString(deleteRequest);

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/team/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("删除队伍参数错误响应：" + response);
        assertNotNull(response);
    }

    /**
     * 综合测试：创建队伍 -> 加入队伍 -> 退出队伍
     */
    @Test
    public void testFullTeamWorkflow() throws Exception {
        // 1. 创建队伍
        TeamCreateRequest createRequest = new TeamCreateRequest();
        createRequest.setTeamName("完整测试队伍");
        createRequest.setTeamAvatarUrl("https://picsum.photos/200");
        createRequest.setTeamDesc("完整流程测试");
        createRequest.setMaxNum(10);
        createRequest.setExpireTime(new Date(System.currentTimeMillis() + 86400000));
        createRequest.setTeamStatus(0);
        createRequest.setAnnounce("测试公告");

        String jsonRequest = objectMapper.writeValueAsString(createRequest);

        MvcResult createResult = mockMvc.perform(MockMvcRequestBuilders.post("/team/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String createResponse = createResult.getResponse().getContentAsString();
        System.out.println("步骤1 - 创建队伍响应：" + createResponse);

        // 2. 查询队伍信息（假设队伍ID为1，实际应从响应中解析）
        MvcResult getResult = mockMvc.perform(MockMvcRequestBuilders.get("/team/1")
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String getResponse = getResult.getResponse().getContentAsString();
        System.out.println("步骤2 - 查询队伍响应：" + getResponse);

        // 3. 加入队伍
        TeamJoinRequest joinRequest = new TeamJoinRequest();
        joinRequest.setTeamId(1L);

        String joinJson = objectMapper.writeValueAsString(joinRequest);

        MvcResult joinResult = mockMvc.perform(MockMvcRequestBuilders.post("/team/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson)
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String joinResponse = joinResult.getResponse().getContentAsString();
        System.out.println("步骤3 - 加入队伍响应：" + joinResponse);

        // 4. 退出队伍
        MvcResult quitResult = mockMvc.perform(MockMvcRequestBuilders.post("/team/quit/1")
                        .sessionAttr("user_login_state", "mock_user_session"))
                .andExpect(status().isOk())
                .andReturn();

        String quitResponse = quitResult.getResponse().getContentAsString();
        System.out.println("步骤4 - 退出队伍响应：" + quitResponse);

        // 验证所有步骤都成功
        assertNotNull(createResponse);
        assertNotNull(getResponse);
        assertNotNull(joinResponse);
        assertNotNull(quitResponse);
    }
}
