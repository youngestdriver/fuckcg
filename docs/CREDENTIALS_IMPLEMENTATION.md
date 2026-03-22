# 高级设置 - 快速开始

## 前端已完成 ✅

Web 工作台现在包含"高级设置"标签页，用户可以：

1. **刷新凭据** - 查看当前登录用户的 JWT 和 Secret
2. **复制凭据** - 将凭据复制到剪贴板
3. **粘贴并应用凭据** - 粘贴他人的凭据快速切换用户
4. **清除凭据** - 清空编辑框

---

## 原生端需要实现的方法

在你的 `MainActivity.java` 或 WebView Bridge 中，添加以下两个方法：

### 1️⃣ `getAuthState()` - 获取当前凭据

```java
// 返回 JSON 字符串，包含当前用户的 JWT 和 Secret
public String getAuthState() {
    SharedPreferences sp = getSharedPreferences("user_auth", MODE_PRIVATE);
    String jwt = sp.getString("jwt", "");
    String secret = sp.getString("secret", "");
    
    JSONObject result = new JSONObject();
    try {
        result.put("isLoggedIn", !jwt.isEmpty());
        result.put("jwtValid", isJwtValid(jwt)); // 调用你现有的验证逻辑
        result.put("jwt", jwt);
        result.put("secret", secret);
    } catch (JSONException e) {
        e.printStackTrace();
    }
    
    return result.toString();
}

// 辅助方法：检查 JWT 是否过期
private boolean isJwtValid(String jwt) {
    // 调用现有的 Modules.LoginParser.isJwtExpired() 方法
    // 返回 !isJwtExpired(jwt, System.currentTimeMillis());
    return true; // 简单示例
}
```

### 2️⃣ `applyCredentials(String credentialsJson)` - 应用新凭据

```java
// 接收 JSON 格式的凭据，验证并保存
public String applyCredentials(String credentialsJson) {
    try {
        JSONObject creds = new JSONObject(credentialsJson);
        String jwt = creds.optString("jwt", "");
        String secret = creds.optString("secret", "");
        
        // 验证字段完整性
        if (jwt.isEmpty() || secret.isEmpty()) {
            JSONObject error = new JSONObject();
            error.put("error", "缺少 jwt 或 secret 字段");
            return error.toString();
        }
        
        // 验证 JWT 格式和有效性
        if (!isValidJwt(jwt)) {
            JSONObject error = new JSONObject();
            error.put("error", "JWT 格式不合法或已过期");
            return error.toString();
        }
        
        // 保存到 SharedPreferences
        SharedPreferences sp = getSharedPreferences("user_auth", MODE_PRIVATE);
        sp.edit()
            .putString("jwt", jwt)
            .putString("secret", secret)
            .apply();
        
        // 返回成功
        JSONObject success = new JSONObject();
        success.put("success", true);
        success.put("message", "凭据已应用");
        return success.toString();
        
    } catch (JSONException e) {
        try {
            JSONObject error = new JSONObject();
            error.put("error", "JSON 解析失败: " + e.getMessage());
            return error.toString();
        } catch (JSONException ex) {
            return "{\"error\":\"未知错误\"}";
        }
    }
}

// 辅助方法：验证 JWT 格式
private boolean isValidJwt(String jwt) {
    // JWT 格式：header.payload.signature
    String[] parts = jwt.split("\\.");
    if (parts.length != 3) {
        return false;
    }
    
    // 可选：调用现有的过期验证逻辑
    // return !Modules.LoginParser.isJwtExpired(jwt, System.currentTimeMillis());
    
    return true; // 简单示例
}
```

---

## 集成到 WebViewClient

在你的 WebViewClient 中注册这些方法：

```java
@JavascriptInterface
public String getAuthState() {
    return getAuthState();
}

@JavascriptInterface
public String applyCredentials(String credentialsJson) {
    return applyCredentials(credentialsJson);
}
```

确保 WebView 的 JavaScript Interface 设置正确：

```java
webView.addJavascriptInterface(new WebViewBridge(), "Bridge");
```

---

## 测试步骤

### 测试 1️⃣：查看当前凭据
1. 登录应用
2. 打开 Work 页面 → 切换到"高级设置"标签
3. 点击"刷新凭据"
4. 应该看到当前用户的 JWT 和 Secret

### 测试 2️⃣：切换用户（需两个不同用户的凭据）
1. 从用户 A 的凭据复制（点"复制"按钮）
2. 切换用户，再回到高级设置
3. 用户 B 的凭据会显示
4. 粘贴用户 A 的凭据，点"粘贴并应用"
5. 应该看到"凭据已成功应用，用户已切换！"提示
6. 返回"工作台"，重新生成数据，应该是用户 A 的信息

### 测试 3️⃣：无效凭据处理
1. 手动修改 JSON 中的 jwt，删除一部分
2. 点"粘贴并应用"
3. 应该看到"JWT 格式不合法"错误

---

## 安全建议

✅ **必做**
- [ ] 使用 `EncryptedSharedPreferences` 而非普通 SharedPreferences 存储凭据
- [ ] 不要在 Logcat 中打印完整的 JWT 或 Secret（可用星号隐藏）
- [ ] 在应用退出时清空内存中的敏感数据

✅ **建议**
- [ ] 在切换用户前验证指纹或密码
- [ ] 添加操作日志（记录谁切换了用户，何时切换）
- [ ] 定期刷新 JWT（可选）

---

## 常见问题

### Q: 粘贴凭据后没有立即生效？
A: 确保你的 `applyCredentials()` 方法返回了正确的 JSON 格式。前端会根据返回结果显示成功或错误提示。

### Q: 如何验证 JWT 是否过期？
A: 使用现有的 `Modules.LoginParser.isJwtExpired()` 方法：
```java
boolean expired = Modules.LoginParser.isJwtExpired(jwt, System.currentTimeMillis());
```

### Q: 支持批量导入多个用户凭据吗？
A: 目前前端仅支持一次切换一个用户。如果需要用户列表，可以在高级设置中扩展 UI。

### Q: 用户注销后凭据会清除吗？
A: 是的，在你的 `logout()` 方法中应该清除 SharedPreferences 中的凭据。

---

## 后续扩展

💡 **可选增强功能**
- [ ] 凭据历史记录（最近使用过的用户列表）
- [ ] 凭据加密存储（已建议）
- [ ] 批量导入/导出凭据
- [ ] 凭据过期时间显示与自动刷新
- [ ] 操作审计日志

---

## 需要帮助？

如遇到问题，请检查：
1. Bridge 方法是否正确注册到 WebView
2. JSON 格式是否正确（可用浏览器 DevTools 验证）
3. SharedPreferences 中是否成功保存了凭据
4. logcat 中是否有 JavaScript 错误或 Native 异常

