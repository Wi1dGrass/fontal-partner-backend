# 惠星伙伴匹配系统

一个基于移动端 H5 的伙伴匹配与组队平台，帮助用户找到志同道合的伙伴进行合作。

## 项目简介

惠星伙伴匹配系统是一个全栈应用，支持用户管理、队伍创建、好友社交、实时聊天等核心功能。项目采用前后端分离架构，前端参考哔哩哔哩设计风格，提供简洁友好的用户体验。

## 技术栈

### 前端 (huixingpartner-frontend)
- **框架**: Vue 3 + TypeScript + Vite
- **UI 组件**: Vant 4 (移动端组件库)
- **状态管理**: Pinia + pinia-plugin-persistedstate
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **样式**: SCSS

### 后端 (fontal-partner-backend)
- **框架**: Spring Boot 2.7.6
- **数据库**: MySQL + MyBatis-Plus 3.5.2
- **API 文档**: Knife4j 3.0.3
- **工具库**: Apache Commons Lang, Gson

## 项目结构

```
HX-partner/
├── huixingpartner-frontend/    # 前端项目 (Vue 3)
│   ├── src/
│   │   ├── api/                # API 接口封装
│   │   ├── router/             # 路由配置
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── types/              # TypeScript 类型定义
│   │   ├── utils/              # 工具函数
│   │   ├── views/              # 页面组件
│   │   └── config/             # 配置文件
│   ├── package.json
│   └── vite.config.ts
│
└── fontal-partner-backend/      # 后端项目 (Spring Boot)
    ├── src/main/java/com/huixing/
    │   ├── controller/          # 控制器层
    │   ├── service/            # 服务层
    │   ├── mapper/             # 数据访问层
    │   └── model/              # 实体类
    ├── src/main/resources/
    │   └── application.yml     # 配置文件
    └── pom.xml
```

## 核心功能

### 1. 用户系统
- 用户注册/登录/登出
- 个人资料编辑（头像、昵称、简介、标签等）
- 用户搜索（文本搜索 + 标签搜索）
- 智能推荐用户

### 2. 队伍系统
- 队伍广场（推荐、热门、最新）
- 创建队伍（支持公开/私有/加密三种类型）
- 加入队伍（一键加入/密码加入/申请加入）
- 队伍管理（编辑信息、踢出成员、转让队长、解散队伍）
- 我的队伍列表

### 3. 好友系统
- 好友列表（实时搜索）
- 好友申请（发送/接收/同意/拒绝）
- 好友关系管理

### 4. 消息系统
- 消息中心（好友申请、队伍申请、队伍邀请等）
- 聊天功能（UI 已完成，待对接 WebSocket）

## 快速开始

### 前端启动

```bash
cd huixingpartner-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

### 后端启动

```bash
cd fontal-partner-backend

# 使用 Maven 启动
mvn spring-boot:run

# 或使用 IDE 运行主类
# src/main/java/com/huixing/xxxApplication.java
```

**注意**: 后端默认端口为 8080，启动前请确保：
1. MySQL 数据库已配置
2. `application.yml` 中的数据库连接信息已正确设置

## 开发进度

| 模块 | 进度 | 状态 |
|------|------|------|
| 项目初始化 | 100% | ✅ 已完成 |
| 登录/注册 | 100% | ✅ 已完成 |
| 个人资料 | 100% | ✅ 已完成 |
| 用户搜索 | 100% | ✅ 已完成 |
| 队伍功能 | 98% | ✅ UI已完成，待对接聊天API |
| 好友功能 | 90% | ✅ 列表/申请已完成，聊天待对接 |
| 消息功能 | 40% | 🚧 聊天详情UI已完成，待对接API |

## UI 设计风格

- **主色调**: 粉色系 (#FB7299)
- **背景**: 白色
- **布局**: 卡片式布局 + 圆角设计
- **参考**: 哔哩哔哩手机端设计风格

## API 文档

后端集成了 Knife4j，启动后访问:
```
http://localhost:8080/doc.html
```

## 数据库

项目使用 MySQL 数据库，具体表结构请参考后端 SQL 脚本。

## 开发文档

- [前端功能索引](huixingpartner-frontend/ai-docs/current/INDEX.md)
- [项目需求文档](huixingpartner-frontend/ai-docs/current/project-planning/project-planning.md)

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目仅供学习交流使用。

---

**最后更新**: 2026-01-19
