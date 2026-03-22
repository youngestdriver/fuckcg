# 高级设置 - 凭据管理指南

## 功能说明

在 Web 工作台的"高级设置"标签页中，用户可以：

1. **刷新凭据** - 获取当前登录的 JWT 和 Secret
2. **复制** - 将凭据复制到剪贴板
3. **粘贴并应用** - 粘贴其他用户的凭据 JSON，切换到该用户
4. **清除** - 清空凭据输入框

## 前端工作流

```
用户打开"高级设置"标签
    ↓
刷新凭据（自动）→ 显示当前用户的 JWT 和 Secret
    ↓
用户可选择：
  A) 复制凭据分享给他人
  B) 粘贴来自他人的凭据，修改后点"粘贴并应用"切换用户
    ↓
后端返回成功 → "用户已切换！"
```

## 原生端 Bridge 实现要求

前端调用以下 Bridge 方法，请在 Android 端实现：

### 1. `getAuthState()` - 获取当前认证状态

**调用时机**：
- 页面初始化时检查登录状态
- 高级设置面板中"刷新凭据"时

**返回值**（JSON 字符串）：
```json
{
  "isLoggedIn": true,
  "jwtValid": true,
  "jwt": "eyJhbGc...",
  "secret": "your_secret_here"
}
```

**实现建议**：
- 从 SharedPreferences 或本地存储读取当前用户的 JWT 和 Secret
- 验证 JWT 是否过期（可参考现有 `Modules.LoginParser.isJwtExpired()` 方法）

---

### 2. `applyCredentials(credentialsJson)` - 应用新凭据切换用户

**调用参数**：
```javascript
credentialsJson = '{"jwt":"...", "secret":"..."}'
```

**返回值**（JSON 字符串）：
```json
{
  "success": true,
  "message": "用户切换成功"
}
```

或失败时：
```json
{
  "error": "JWT 格式不合法或已过期"
}
```

**实现建议**：
1. 解析传入的 JSON 字符串
2. 验证 `jwt` 和 `secret` 字段是否存在
3. 解析 JWT，检查格式和过期时间
4. 将凭据保存到 SharedPreferences
5. 返回成功或错误消息

---

### 3. `buildUploadJsonSports(studentId, studentName)` - 生成数据

**现有实现**：已实现，使用原生库生成签名

---

### 4. `submitSportsData(sportsJsonString)` - 提交数据

**现有实现**：已实现，使用当前凭据和 HTTP 请求提交

---

## 安全建议

⚠️ **重要**：凭据包含敏感信息（JWT、Secret），请注意以下几点：

1. **本地存储加密**：将 JWT 和 Secret 存储在加密的 SharedPreferences 中（使用 EncryptedSharedPreferences）
2. **内存清理**：在应用退出或用户注销时清空内存中的凭据
3. **日志安全**：不要在 Logcat 中打印完整的 JWT 或 Secret
4. **用户验证**：可选：在切换用户前要求输入设备密码或指纹认证
5. **过期检查**：自动检查 JWT 过期，提示用户重新登录

---

## 凭据格式示例

```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI...",
  "secret": "abc123def456ghi789jkl"
}
```

---

## 前端测试

在浏览器开发者工具中测试（模拟 Bridge 响应）：

```javascript
// 模拟 getAuthState()
window.Bridge = {
  getAuthState: function() {
    return '{"isLoggedIn":true,"jwtValid":true,"jwt":"test_jwt_123","secret":"test_secret_456"}';
  },
  applyCredentials: function(json) {
    console.log('Applied:', json);
    return '{"success":true,"message":"用户切换成功"}';
  }
};
```

---

## 常见问题

### Q: 如何验证 JWT 的有效性？

A: 参考现有代码中的 `Modules.LoginParser.isJwtExpired()` 方法。

### Q: 支持批量切换多个用户吗？

A: 目前仅支持一次切换一个用户。若需要用户列表管理，可以在"高级设置"中扩展 UI。

### Q: 粘贴凭据后需要重启应用吗？

A: 不需要。应用凭据后立即生效，用户可继续使用工作台生成数据。

