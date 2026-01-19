# 消息系统API文档

## 概述

消息系统包含好友申请、队伍申请、队伍邀请等功能。本文档记录所有相关的API接口。

---

## 1. 好友申请相关接口

### 1.1 获取收到的好友申请列表

**接口地址**：`GET /api/friend/received`

**请求方式**：`GET`

**请求数据类型**：`application/x-www-form-urlencoded`

**响应数据类型**：`*/*`

**接口描述**：获取其他用户发送给我的好友申请

**请求参数**：无

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«List«FriendsRecordVO»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||

**响应参数**：
| 参数名称 | 参数说明 | 类型 | schema |
|---------|---------|------|--------|
|code||integer(int32)|integer(int32)|
|data||array|FriendsRecordVO|
|&emsp;&emsp;applyUser||User|User|
|&emsp;&emsp;&emsp;&emsp;contactInfo||string||
|&emsp;&emsp;&emsp;&emsp;createTime||string||
|&emsp;&emsp;&emsp;&emsp;email||string||
|&emsp;&emsp;&emsp;&emsp;gender||integer||
|&emsp;&emsp;&emsp;&emsp;id||integer||
|&emsp;&emsp;&emsp;&emsp;isDelete||integer||
|&emsp;&emsp;&emsp;&emsp;tags||string||
|&emsp;&emsp;&emsp;&emsp;teamIds||string||
|&emsp;&emsp;&emsp;&emsp;updateTime||string||
|&emsp;&emsp;&emsp;&emsp;userAccount||string||
|&emsp;&emsp;&emsp;&emsp;userAvatarUrl||string||
|&emsp;&emsp;&emsp;&emsp;userDesc||string||
|&emsp;&emsp;&emsp;&emsp;userIds||string||
|&emsp;&emsp;&emsp;&emsp;userPassword||string||
|&emsp;&emsp;&emsp;&emsp;userRole||integer||
|&emsp;&emsp;&emsp;&emsp;userStatus||integer||
|&emsp;&emsp;&emsp;&emsp;username||string||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;remark||string||
|&emsp;&emsp;status||integer(int32)||
|description||string||
|message||string||

**响应示例**：
```javascript
{
  "code": 0,
  "data": [
    {
      "applyUser": {
        "contactInfo": "",
        "createTime": "",
        "email": "",
        "gender": 0,
        "id": 0,
        "isDelete": 0,
        "tags": "",
        "teamIds": "",
        "updateTime": "",
        "userAccount": "",
        "userAvatarUrl": "",
        "userDesc": "",
        "userIds": "",
        "userPassword": "",
        "userRole": 0,
        "userStatus": 0,
        "username": ""
      },
      "id": 0,
      "remark": "申请备注",
      "status": 0
    }
  ],
  "description": "",
  "message": ""
}
```

**状态说明**：
- `status: 0` - 待处理
- `status: 1` - 已接受
- `status: 2` - 已拒绝
- `status: 3` - 已撤销

---

### 1.2 获取发送的好友申请列表

**接口地址**：`GET /api/friend/sent`

**请求方式**：`GET`

**请求数据类型**：`application/x-www-form-urlencoded`

**响应数据类型**：`*/*`

**接口描述**：获取我发送给其他用户的好友申请

**请求参数**：无

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«List«FriendsRecordVO»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||

**响应参数**：同 1.1

---

### 1.3 处理好友申请

**接口地址**：`POST /api/friend/handle`

**请求方式**：`POST`

**请求数据类型**：`application/json`

**响应数据类型**：`*/*`

**接口描述**：同意或拒绝好友申请

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|id|申请记录ID|body|true|integer(int64)||
|status|状态：1-接受，2-拒绝|body|true|integer(int32)||

**请求示例**：
```javascript
{
  "id": 123,
  "status": 1
}
```

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«integer»|
|401|Unauthorized||
|403|Forbidden||

**响应参数**：
| 参数名称 | 参数说明 | 类型 | schema |
|---------|---------|------|--------|
|code||integer(int32)|integer(int32)|
|data||integer(int32)||
|description||string||
|message||string||

---

### 1.4 撤销好友申请

**接口地址**：`DELETE /api/friend/revoke/{applicationId}`

**请求方式**：`DELETE`

**请求数据类型**：`application/x-www-form-urlencoded`

**响应数据类型**：`*/*`

**接口描述**：撤销我发送的好友申请

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|applicationId|申请记录ID|path|true|integer(int64)||

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«boolean»|
|204|No Content||
|401|Unauthorized||
|403|Forbidden||

**响应参数**：
| 参数名称 | 参数说明 | 类型 | schema |
|---------|---------|------|--------|
|code||integer(int32)|integer(int32)|
|data||boolean||
|description||string||
|message||string||

---

### 1.5 标记好友申请已读

**接口地址**：`POST /api/friend/mark-read`

**请求方式**：`POST`

**请求数据类型**：`application/json`

**响应数据类型**：`*/*`

**接口描述**：标记好友申请为已读状态

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|friendId|申请ID，为空则标记所有未读申请|body|false|integer(int64)||

**请求示例**：
```javascript
{
  "friendId": 123
}
```

或

```javascript
{}  // 标记所有未读申请
```

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«boolean»|
|401|Unauthorized||
|403|Forbidden||

---

## 2. 队伍申请相关接口

### 2.1 获取队伍的待审批申请列表

**接口地址**：`GET /api/team/applications/{teamId}`

**请求方式**：`GET`

**请求数据类型**：`application/x-www-form-urlencoded`

**响应数据类型**：`*/*`

**接口描述**：获取指定队伍的待审批申请列表（队长查看）

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|teamId|队伍ID|path|true|integer(int64)||

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«List«TeamJoinApplyVO»»|
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||

**响应参数**：
| 参数名称 | 参数说明 | 类型 | schema |
|---------|---------|------|--------|
|code||integer(int32)|integer(int32)|
|data||array|TeamJoinApplyVO|
|&emsp;&emsp;applyMessage||string||
|&emsp;&emsp;applyStatus||integer(int32)||
|&emsp;&emsp;applyType||integer(int32)||
|&emsp;&emsp;applyTypeDesc||string||
|&emsp;&emsp;createTime||string(date-time)||
|&emsp;&emsp;id||integer(int64)||
|&emsp;&emsp;leaderAvatar||string||
|&emsp;&emsp;leaderId||integer(int64)||
|&emsp;&emsp;leaderName||string||
|&emsp;&emsp;rejectReason||string||
|&emsp;&emsp;statusDesc||string||
|&emsp;&emsp;teamAvatar||string||
|&emsp;&emsp;teamId||integer(int64)||
|&emsp;&emsp;teamName||string||
|&emsp;&emsp;updateTime||string(date-time)||
|&emsp;&emsp;userAvatar||string||
|&emsp;&emsp;userId||integer(int64)||
|&emsp;&emsp;userName||string||
|description||string||
|message||string||

**字段说明**：
- `applyType`: 申请类型
  - `0` - 用户申请（用户主动申请加入队伍）
  - `1` - 队伍邀请（队长邀请用户加入队伍）
- `applyStatus`: 申请状态
  - `0` - 待处理
  - `1` - 已通过
  - `2` - 已拒绝

**响应示例**：
```javascript
{
  "code": 0,
  "data": [
    {
      "applyMessage": "请让我加入",
      "applyStatus": 0,
      "applyType": 0,
      "applyTypeDesc": "用户申请",
      "createTime": "2024-01-19 12:00:00",
      "id": 123,
      "leaderAvatar": "https://...",
      "leaderId": 10001,
      "leaderName": "队长名称",
      "rejectReason": "",
      "statusDesc": "待处理",
      "teamAvatar": "https://...",
      "teamId": 100004,
      "teamName": "我的队伍",
      "updateTime": "2024-01-19 12:00:00",
      "userAvatar": "https://...",
      "userId": 70571,
      "userName": "申请人"
    }
  ],
  "message": "ok",
  "description": ""
}
```

---

### 2.2 取消加入申请

**接口地址**：`POST /api/team/apply/cancel/{teamId}`

**请求方式**：`POST`

**请求数据类型**：`application/json`

**响应数据类型**：`*/*`

**接口描述**：取消我申请加入的队伍

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|teamId|队伍ID|path|true|integer(int64)||

**响应状态**：
| 状态码 | 说明 | schema |
|--------|------|--------|
|200|OK|BaseResponse«boolean»|
|201|Created||
|401|Unauthorized||
|403|Forbidden||
|404|Not Found||

**响应参数**：
| 参数名称 | 参数说明 | 类型 | schema |
|---------|---------|------|--------|
|code||integer(int32)|integer(int32)|
|data||boolean||
|description||string||
|message||string||

---

## 3. 待后端实现的接口

### 3.1 获取我发送的队伍申请列表

**接口地址**：`GET /api/team/my-applications`（建议）

**请求方式**：`GET`

**接口描述**：获取我申请加入的所有队伍列表及申请状态

**请求参数**：无

**预期响应**：
```javascript
{
  "code": 0,
  "data": [
    {
      "id": 123,
      "teamId": 100004,
      "teamName": "我的队伍",
      "teamAvatar": "https://...",
      "applyMessage": "请让我加入",
      "status": 0,
      "createTime": "2024-01-19 12:00:00"
    }
  ],
  "message": "ok"
}
```

**状态说明**：
- `status: 0` - 待处理
- `status: 1` - 已通过
- `status: 2` - 已拒绝
- `status: 3` - 已取消

---

### 3.2 获取我收到的队伍邀请列表

**接口地址**：`GET /api/team/invitations`（建议）

**请求方式**：`GET`

**接口描述**：获取队长邀请我加入的队伍列表（apply_type=1）

**请求参数**：无

**预期响应**：
```javascript
{
  "code": 0,
  "data": [
    {
      "id": 456,
      "teamId": 100005,
      "teamName": "邀请的队伍",
      "teamAvatar": "https://...",
      "leaderId": 10002,
      "leaderName": "队长",
      "leaderAvatar": "https://...",
      "applyMessage": "诚邀加入",
      "status": 0,
      "createTime": "2024-01-19 12:00:00"
    }
  ],
  "message": "ok"
}
```

**说明**：
- `applyType = 1` 表示队伍邀请
- 队伍邀请的数据结构可能复用 `/api/team/apply` 接口，通过 `applyType` 字段区分

---

### 3.3 处理队伍申请/邀请

**接口地址**：`POST /api/team/application/handle`（建议）

**请求方式**：`POST`

**请求数据类型**：`application/json`

**接口描述**：同意或拒绝队伍申请/邀请

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|teamId|队伍ID|body|true|integer(int64)||
|applicationId|申请记录ID|body|true|integer(int64)||
|status|状态：1-通过，2-拒绝|body|true|integer(int32)||

**请求示例**：
```javascript
{
  "teamId": 100004,
  "applicationId": 123,
  "status": 1
}
```

**响应示例**：
```javascript
{
  "code": 0,
  "data": true,
  "message": "ok"
}
```

---

## 4. 聊天消息相关接口（待实现）

### 4.1 发送文本消息

**接口地址**：`POST /api/chat/send`

**请求方式**：`POST`

**请求数据类型**：`application/json`

**接口描述**：发送聊天消息

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|receiverId|接收者ID|body|true|integer(int64)||
|messageType|消息类型：1-文本，2-图片，3-语音，4-文件|body|true|integer(int32)||
|content|消息内容|body|true|string||

**请求示例**：
```javascript
{
  "receiverId": 70571,
  "messageType": 1,
  "content": "你好"
}
```

**响应示例**：
```javascript
{
  "code": 0,
  "data": {
    "id": 789,
    "senderId": 70572,
    "receiverId": 70571,
    "messageType": 1,
    "content": "你好",
    "sendTime": "2024-01-19 12:00:00"
  },
  "message": "发送成功"
}
```

---

### 4.2 获取聊天历史

**接口地址**：`GET /api/chat/history/{userId}`

**请求方式**：`GET`

**接口描述**：获取与指定用户的聊天历史记录

**请求参数**：
| 参数名称 | 参数说明 | 请求类型 | 是否必须 | 数据类型 | schema |
|---------|---------|---------|---------|---------|--------|
|userId|对方用户ID|path|true|integer(int64)||
|page|页码，从1开始|query|false|integer(int32)||
|size|每页大小，默认20|query|false|integer(int32)||

**响应示例**：
```javascript
{
  "code": 0,
  "data": {
    "total": 100,
    "list": [
      {
        "id": 789,
        "senderId": 70572,
        "receiverId": 70571,
        "messageType": 1,
        "content": "你好",
        "sendTime": "2024-01-19 12:00:00",
        "isRead": true
      }
    ]
  },
  "message": "ok"
}
```

---

### 4.3 获取未读消息数量

**接口地址**：`GET /api/chat/unread-count`

**请求方式**：`GET`

**接口描述**：获取当前用户的未读消息总数

**请求参数**：无

**响应示例**：
```javascript
{
  "code": 0,
  "data": 5,
  "message": "ok"
}
```

---

## 5. WebSocket 实时推送（待实现）

### 5.1 连接WebSocket

**接口地址**：`ws://localhost:8080/ws/chat`

**连接参数**：
| 参数名称 | 参数说明 | 类型 | 是否必须 |
|---------|---------|------|---------|
|token|用户认证token|string|true|

**连接示例**：
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/chat?token=xxx')

ws.onmessage = (event) => {
  const message = JSON.parse(event.data)
  // 处理接收到的消息
}
```

**消息推送格式**：
```javascript
{
  "type": "new_message",
  "data": {
    "id": 789,
    "senderId": 70572,
    "senderName": "发送者",
    "senderAvatar": "https://...",
    "receiverId": 70571,
    "messageType": 1,
    "content": "你好",
    "sendTime": "2024-01-19 12:00:00"
  }
}
```

**推送类型**：
- `new_message` - 新消息
- `friend_request` - 好友申请
- `team_invitation` - 队伍邀请
- `team_application` - 队伍申请通知
- `message_read` - 消息已读回执

---

## 附录：通用响应结构

所有接口都遵循以下响应结构：

```typescript
interface BaseResponse<T> {
  code: number        // 状态码：0-成功，其他-失败
  data: T            // 响应数据
  message?: string   // 提示信息
  description?: string // 详细描述
}
```

**状态码说明**：
- `0` - 成功
- `40001` - 参数错误
- `40101` - 未登录
- `40301` - 无权限
- `50001` - 服务器错误

---

**最后更新**: 2026-01-19
**版本**: v1.0.0
