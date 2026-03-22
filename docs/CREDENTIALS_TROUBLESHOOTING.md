# 🔧 凭据加载故障排查指南

## 问题：凭据没有显示

### 症状
- 进入"高级设置"标签页
- 点击"刷新凭据"按钮，但凭据编辑框仍然为空
- 显示错误信息或"当前环境不支持凭据获取"

---

## 🔍 诊断步骤

### 第一步：确认 Bridge 实现状态

#### 在浏览器中测试
1. 打开 Chrome DevTools（F12）
2. 进入 Console 标签
3. 运行以下代码：

```javascript
// 检查 Bridge 是否存在
console.log('Bridge exists:', typeof window.Bridge !== 'undefined');

// 检查 getAuthState 方法是否存在
console.log('getAuthState exists:', typeof window.Bridge?.getAuthState === 'function');

// 尝试调用 getAuthState
if (window.Bridge?.getAuthState) {
  const result = window.Bridge.getAuthState();
  console.log('getAuthState result:', result);
  console.log('Parsed:', JSON.parse(result));
}
```

### 第二步：检查返回值格式

如果上面的代码输出了结果，检查格式是否正确：

✅ **正确格式**：
```json
{
  "isLoggedIn": true,
  "jwtValid": true,
  "jwt": "eyJhbGc...",
  "secret": "xxx_secret_xxx"
}
```

❌ **常见错误格式**：
```javascript
// 错误 1：返回对象而非字符串
{isLoggedIn: true, jwt: "..."}  // ❌ 应该是 JSON 字符串

// 错误 2：缺少必要字段
{"isLoggedIn": true}  // ❌ 缺少 jwt 和 secret

// 错误 3：返回 null 或 undefined
null  // ❌ 应该返回 JSON 字符串
```

---

## 🛠️ 常见问题和解决方案

### 问题 1：Bridge 方法不存在
**表现**：Console 显示 `getAuthState exists: false`

**原因**：Android 端没有实现该方法

**解决方案**：
1. 查看 `CREDENTIALS_IMPLEMENTATION.md`
2. 在 Android 代码中添加以下方法：

```java
@JavascriptInterface
public String getAuthState() {
    SharedPreferences sp = getSharedPreferences("user_auth", MODE_PRIVATE);
    String jwt = sp.getString("jwt", "");
    String secret = sp.getString("secret", "");
    
    JSONObject result = new JSONObject();
    try {
        result.put("isLoggedIn", !jwt.isEmpty());
        result.put("jwtValid", !jwt.isEmpty());
        result.put("jwt", jwt);
        result.put("secret", secret);
    } catch (JSONException e) {
        e.printStackTrace();
    }
    
    return result.toString();
}
```

3. 在 WebView 中注册：
```java
webView.addJavascriptInterface(new WebViewBridge(), "Bridge");
```

### 问题 2：返回值不是 JSON 字符串
**表现**：Console 显示 `JSON.parse() error` 或返回值是 `[object Object]`

**原因**：返回了 JavaScript 对象而不是 JSON 字符串

**解决方案**：
确保在原生端返回的是 **JSON 字符串**：

```java
// ✅ 正确
return result.toString();  // 返回 JSON 字符串

// ❌ 错误
return result;  // 返回对象
```

### 问题 3：凭据为空
**表现**：凭据编辑框显示 `{"jwt":"","secret":""}`

**原因**：
1. 用户还未登录
2. SharedPreferences 中没有保存凭据
3. 登录后凭据未正确保存

**解决方案**：
1. 确保用户已完成登录（OAuth 流程）
2. 检查登录后是否保存了凭据到 SharedPreferences：

```java
// 在登录成功后
SharedPreferences sp = getSharedPreferences("user_auth", MODE_PRIVATE);
sp.edit()
    .putString("jwt", jwtToken)
    .putString("secret", secretKey)
    .apply();
```

### 问题 4：点击"刷新凭据"后仍无反应
**表现**：按钮点击无反应，编辑框不更新

**原因**：
1. HTML 元素 ID 不匹配
2. JavaScript 事件监听失败
3. Bridge 方法返回值格式不对

**解决方案**：
1. 在 Console 中检查元素：
```javascript
console.log('refreshCredsButton:', document.getElementById('refreshCredsButton'));
console.log('credentialsOutput:', document.getElementById('credentialsOutput'));
console.log('advancedStatus:', document.getElementById('advancedStatus'));
```

2. 所有元素都应返回 DOM 元素对象，不是 `null`
3. 如果任何元素是 `null`，检查 HTML 中的 ID 是否拼写正确

---

## 📊 诊断流程图

```
打开高级设置
    ↓
点击"刷新凭据"
    ↓
┌─────────────────────────────────────┐
│ Bridge.getAuthState 存在吗？         │
├─────────────────────────────────────┤
│ 否 → 显示示例数据 + 警告信息
│       (需要在 Android 端实现)
│
│ 是 → 调用 Bridge.getAuthState()
│       ↓
│   返回值是有效 JSON 吗？
│   └─ 否 → 显示错误，显示返回原文
│   └─ 是 → 包含 jwt 和 secret 吗？
│       └─ 否 → 提示未检测到凭据
│       └─ 是 → 显示凭据 ✅
└─────────────────────────────────────┘
```

---

## 🧪 本地测试

### 方式 1：在浏览器中模拟 Bridge（快速测试）

```javascript
// 在 Console 中粘贴以下代码
window.Bridge = {
  getAuthState: function() {
    return JSON.stringify({
      "isLoggedIn": true,
      "jwtValid": true,
      "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test",
      "secret": "test_secret_12345"
    });
  },
  applyCredentials: function(json) {
    console.log('Applied:', json);
    return JSON.stringify({"success": true});
  }
};

// 然后点击"刷新凭据"按钮
```

### 方式 2：在 Android 中测试

1. 运行应用并登录
2. 打开高级设置标签页
3. 点击"刷新凭据"按钮
4. 查看 logcat 输出：

```bash
adb logcat | grep -E "Bridge|getAuthState|credentials"
```

---

## 📋 完整检查清单

- [ ] Bridge 方法已在 Android 端实现
- [ ] `getAuthState()` 返回有效的 JSON 字符串
- [ ] JSON 包含 `isLoggedIn`, `jwtValid`, `jwt`, `secret` 四个字段
- [ ] 用户已完成登录
- [ ] 登录后凭据已保存到 SharedPreferences
- [ ] WebView 已注册 JavaScript Bridge
- [ ] HTML 中的元素 ID 正确（`credentialsOutput`, `refreshCredsButton` 等）
- [ ] 前端 JavaScript 无错误（检查 Console）
- [ ] 点击"刷新凭据"后编辑框有内容

---

## 🆘 还是无法解决？

### 收集诊断信息

在 Console 中运行以下命令，记录输出：

```javascript
// 1. Bridge 状态
console.log('=== Bridge 诊断 ===');
console.log('Bridge 存在:', typeof window.Bridge !== 'undefined');
console.log('getAuthState 存在:', typeof window.Bridge?.getAuthState === 'function');

// 2. 调用结果
console.log('=== getAuthState 调用结果 ===');
try {
  const result = window.Bridge?.getAuthState?.();
  console.log('Raw result:', result);
  console.log('Parsed:', result ? JSON.parse(result) : null);
} catch (e) {
  console.error('Error:', e.message);
}

// 3. 页面元素
console.log('=== 页面元素 ===');
console.log('credentialsOutput:', document.getElementById('credentialsOutput'));
console.log('refreshCredsButton:', document.getElementById('refreshCredsButton'));
console.log('advancedStatus:', document.getElementById('advancedStatus'));
```

然后将输出信息整理后，提交给开发团队。

---

## 💡 改进建议

为了让凭据加载更稳定，可以：

1. **添加重试机制** - 失败后自动重试
2. **超时控制** - 如果 Bridge 调用超时，显示友好提示
3. **缓存凭据** - 第一次加载后缓存，减少调用
4. **加密存储** - 使用 EncryptedSharedPreferences 而非普通 SharedPreferences
5. **操作日志** - 记录凭据加载和应用的每一步

---

**最后更新**：2024-03-22  
**版本**：1.0.1 (含诊断功能)

