package com.huixing.fontal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huixing.fontal.common.BaseResponse;
import com.huixing.fontal.model.entity.User;
import com.huixing.fontal.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FileController 测试类
 * 测试文件上传接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    private static final String TEST_PICTURE_PATH = "C:/Users/11695/Desktop/tesPicturet/";

    @BeforeEach
    public void setUp() throws Exception {
        session = new MockHttpSession();
        
        // 创建测试用户并登录
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setUserAccount("testAccount");
        testUser.setUserPassword("password123");
        testUser.setUserAvatarUrl("https://example.com/default-avatar.jpg");
        
        // 将用户信息存入session
        session.setAttribute("user_login_state", testUser);
    }

    /**
     * 测试上传PNG格式图片
     */
    @Test
    public void testUploadPngImage() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        assertTrue(imageFile.exists(), "测试图片文件不存在: " + imageFile.getAbsolutePath());

        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test1.png",
                "image/png",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("上传PNG图片响应: " + response);
        
        // 验证响应包含成功状态
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "上传PNG图片应该成功");
        assertNotNull(baseResponse.getData(), "响应数据不应为空");
    }

    /**
     * 测试上传JPG格式图片
     */
    @Test
    public void testUploadJpgImage() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test2.jpg");
        assertTrue(imageFile.exists(), "测试图片文件不存在: " + imageFile.getAbsolutePath());

        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test2.jpg",
                "image/jpeg",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("上传JPG图片响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "上传JPG图片应该成功");
        assertNotNull(baseResponse.getData(), "响应数据不应为空");
    }

    /**
     * 测试上传GIF格式图片
     */
    @Test
    public void testUploadGifImage() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test3.gif");
        assertTrue(imageFile.exists(), "测试图片文件不存在: " + imageFile.getAbsolutePath());

        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test3.gif",
                "image/gif",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("上传GIF图片响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "上传GIF图片应该成功");
        assertNotNull(baseResponse.getData(), "响应数据不应为空");
    }

    /**
     * 测试上传BMP格式图片
     */
    @Test
    public void testUploadBmpImage() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test4.bmp");
        assertTrue(imageFile.exists(), "测试图片文件不存在: " + imageFile.getAbsolutePath());

        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test4.bmp",
                "image/bmp",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("上传BMP图片响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "上传BMP图片应该成功");
        assertNotNull(baseResponse.getData(), "响应数据不应为空");
    }

    /**
     * 测试上传队伍头像
     */
    @Test
    public void testUploadTeamAvatar() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        assertTrue(imageFile.exists(), "测试图片文件不存在");

        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "team_avatar.png",
                "image/png",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "team_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("上传队伍头像响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "上传队伍头像应该成功");
        assertNotNull(baseResponse.getData(), "响应数据不应为空");
        
        // 验证返回的URL包含team_avatar路径
        String fileUrl = (String) baseResponse.getData();
        assertTrue(fileUrl.contains("team_avatar"), "返回的URL应包含team_avatar路径");
    }

    /**
     * 测试业务类型为空的情况
     */
    @Test
    public void testUploadWithEmptyBiz() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test1.png",
                "image/png",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("业务类型为空响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertNotEquals(0, baseResponse.getCode(), "业务类型为空应该返回错误");
        assertNotNull(baseResponse.getDescription(), "错误描述不应为空");
    }

    /**
     * 测试业务类型为null的情况
     */
    @Test
    public void testUploadWithNullBiz() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test1.png",
                "image/png",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("业务类型为null响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertNotEquals(0, baseResponse.getCode(), "业务类型为null应该返回错误");
        assertNotNull(baseResponse.getDescription(), "错误描述不应为空");
    }

    /**
     * 测试上传不支持的文件类型（PDF）
     */
    @Test
    public void testUploadUnsupportedFileType() throws Exception {
        byte[] fileContent = "This is a PDF content".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("不支持的文件类型响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertNotEquals(0, baseResponse.getCode(), "不支持的文件类型应该返回错误");
    }

    /**
     * 测试上传超大文件（超过2MB）
     */
    @Test
    public void testUploadOversizedFile() throws Exception {
        // 创建超过2MB的文件
        byte[] fileContent = new byte[3 * 1024 * 1024]; // 3MB
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "oversized.jpg",
                "image/jpeg",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("超大文件响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertNotEquals(0, baseResponse.getCode(), "超大文件应该返回错误");
    }

    /**
     * 测试上传空文件
     */
    @Test
    public void testUploadEmptyFile() throws Exception {
        byte[] fileContent = new byte[0];
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("空文件响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        // 空文件大小为0，应该会通过大小校验，但可能在后续处理中失败
        // 或者如果服务端对空文件有特殊处理，可能会成功
        System.out.println("空文件测试的响应码: " + baseResponse.getCode());
    }

    /**
     * 测试上传没有扩展名的文件
     */
    @Test
    public void testUploadFileWithoutExtension() throws Exception {
        byte[] fileContent = "test content".getBytes();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "noextension",
                "image/jpeg",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("无扩展名文件响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        // 无扩展名的文件应该在校验时失败
        assertNotEquals(0, baseResponse.getCode(), "无扩展名的文件应该返回错误");
    }

    /**
     * 测试文件名包含多个点
     */
    @Test
    public void testUploadFileWithMultipleDots() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "my.avatar.file.png",
                "image/png",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("文件名包含多个点响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "文件名包含多个点应该也能成功上传");
    }

    /**
     * 测试大写扩展名
     */
    @Test
    public void testUploadFileWithUppercaseExtension() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test2.jpg");
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "avatar.JPG",
                "image/jpeg",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("大写扩展名响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertEquals(0, baseResponse.getCode(), "大写扩展名应该也能成功上传（会被转换为小写）");
    }

    /**
     * 综合测试：连续上传多个不同类型的头像
     */
    @Test
    public void testUploadMultipleAvatars() throws Exception {
        // 上传用户头像PNG
        File pngFile = new File(TEST_PICTURE_PATH + "test1.png");
        byte[] pngContent = Files.readAllBytes(pngFile.toPath());
        MockMultipartFile pngFilePart = new MockMultipartFile(
                "file",
                "user_avatar.png",
                "image/png",
                pngContent
        );

        MvcResult pngResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(pngFilePart)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String pngResponse = pngResult.getResponse().getContentAsString();
        System.out.println("步骤1 - 上传用户头像PNG响应: " + pngResponse);
        
        BaseResponse<?> pngBaseResponse = objectMapper.readValue(pngResponse, BaseResponse.class);
        assertEquals(0, pngBaseResponse.getCode(), "上传PNG头像应该成功");

        // 上传用户头像JPG
        File jpgFile = new File(TEST_PICTURE_PATH + "test2.jpg");
        byte[] jpgContent = Files.readAllBytes(jpgFile.toPath());
        MockMultipartFile jpgFilePart = new MockMultipartFile(
                "file",
                "user_avatar.jpg",
                "image/jpeg",
                jpgContent
        );

        MvcResult jpgResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(jpgFilePart)
                        .param("biz", "user_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String jpgResponse = jpgResult.getResponse().getContentAsString();
        System.out.println("步骤2 - 上传用户头像JPG响应: " + jpgResponse);
        
        BaseResponse<?> jpgBaseResponse = objectMapper.readValue(jpgResponse, BaseResponse.class);
        assertEquals(0, jpgBaseResponse.getCode(), "上传JPG头像应该成功");

        // 上传队伍头像GIF
        File gifFile = new File(TEST_PICTURE_PATH + "test3.gif");
        byte[] gifContent = Files.readAllBytes(gifFile.toPath());
        MockMultipartFile gifFilePart = new MockMultipartFile(
                "file",
                "team_avatar.gif",
                "image/gif",
                gifContent
        );

        MvcResult gifResult = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(gifFilePart)
                        .param("biz", "team_avatar")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String gifResponse = gifResult.getResponse().getContentAsString();
        System.out.println("步骤3 - 上传队伍头像GIF响应: " + gifResponse);
        
        BaseResponse<?> gifBaseResponse = objectMapper.readValue(gifResponse, BaseResponse.class);
        assertEquals(0, gifBaseResponse.getCode(), "上传GIF头像应该成功");
    }

    /**
     * 测试未登录用户上传文件
     */
    @Test
    public void testUploadWithoutLogin() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test1.png",
                "image/png",
                fileContent
        );

        // 不提供session信息
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "user_avatar"))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("未登录用户上传响应: " + response);
        
        // 未登录应该被拦截器拦截或返回错误
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertNotEquals(0, baseResponse.getCode(), "未登录用户上传应该失败");
    }

    /**
     * 测试无效的业务类型
     */
    @Test
    public void testUploadWithInvalidBiz() throws Exception {
        File imageFile = new File(TEST_PICTURE_PATH + "test1.png");
        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "test1.png",
                "image/png",
                fileContent
        );

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/file/upload")
                        .file(multipartFile)
                        .param("biz", "invalid_biz_type")
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("无效业务类型响应: " + response);
        
        BaseResponse<?> baseResponse = objectMapper.readValue(response, BaseResponse.class);
        assertNotEquals(0, baseResponse.getCode(), "无效的业务类型应该返回错误");
    }
}
