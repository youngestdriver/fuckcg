# 🎯 快速参考卡

## 前端用户使用流程

### 场景 1：生成和修改数据

```
1. 登录应用 → 进入 Work 页面
2. 输入学号和姓名
3. 点"生成 UploadJsonSports"
   ↓
   {
     "odometer": 2.1,
     "stepCount": 3500,
     "avgSpeed": 2.5,
     ...
   }
4. [可选] 在编辑框修改某些字段
   例如改 "odometer": 2.1 → "odometer": 3.0
5. 点"格式化 JSON"（重新排版，可选）
6. 点"发起 HTTP 请求"（发送修改后的数据）
7. 查看响应结果
```

### 场景 2：切换用户

```
1. 登录用户 A
2. 进入"高级设置"标签
3. 点"刷新凭据"（获取A的JWT+Secret）
4. 点"复制"（复制到剪贴板）
   ↓
   {
     "jwt": "eyJhbGc...",
     "secret": "abc123..."
   }
5. 分享给用户 B（或保存备份）
6. 用户 B 登录自己的应用
7. 进入"高级设置"标签
8. 粘贴用户 A 的凭据（Ctrl+V 或手动粘贴）
9. 点"粘贴并应用"
   ↓
   "凭据已成功应用，用户已切换！"
10. 现在用户 B 可以以用户 A 的身份生成和提交数据
```

---

## 原生端实现清单

### 必须实现的两个方法

#### 方法 1️⃣：`getAuthState()`

```java
@JavascriptInterface
public String getAuthState() {
    // 1. 从存储读取 JWT 和 Secret
    // 2. 检查 JWT 是否过期
    // 3. 返回 JSON: {"isLoggedIn":true,"jwtValid":true,"jwt":"...","secret":"..."}
    return json;
}
```

**关键点**：
- 返回**必须是 JSON 字符串**（不是对象）
- 包含 `isLoggedIn`, `jwtValid`, `jwt`, `secret` 四个字段
- JWT 应来自 SharedPreferences 的保存凭据

#### 方法 2️⃣：`applyCredentials(String json)`

```java
@JavascriptInterface
public String applyCredentials(String credentialsJson) {
    // 1. 解析传入的 JSON
    // 2. 提取 jwt 和 secret
    // 3. 验证 JWT 格式（header.payload.signature）
    // 4. 保存到 SharedPreferences
    // 5. 返回成功或错误 JSON
    return resultJson;
}
```

**关键点**：
- 参数是 JSON 字符串：`{"jwt":"...","secret":"..."}`
- 返回成功时：`{"success":true}`
- 返回失败时：`{"error":"描述错误"}`

---

## HTML 结构变化

### 之前：仅一个生成器

```html
<textarea id="output">...</textarea>  ← 只读 <pre>
<button id="generateButton">生成</button>
<button id="submitButton">发送</button>
```

### 之后：三标签页结构

```html
<div class="tab-panel" id="workPanel">
  <!-- 工作台内容（生成和修改JSON）-->
  <textarea id="output">...</textarea>
  <button id="generateButton">生成</button>
  <button id="formatButton">格式化</button>
  <button id="copyButton">复制</button>
  <button id="submitButton">发送</button>
</div>

<div class="tab-panel" id="aboutPanel">
  <!-- 关于页面 -->
</div>

<div class="tab-panel" id="advancedPanel">
  <!-- 高级设置 -->
  <textarea id="credentialsOutput">...</textarea>
  <button id="refreshCredsButton">刷新凭据</button>
  <button id="copyCresButton">复制</button>
  <button id="pasteCredsButton">粘贴并应用</button>
  <button id="clearCredsButton">清除</button>
</div>

<nav class="tab-bar">
  <button data-tab-target="workPanel">工作台</button>
  <button data-tab-target="aboutPanel">关于</button>
  <button data-tab-target="advancedPanel">高级设置</button>
</nav>
```

---

## 调试技巧

### 浏览器调试（模拟 Bridge）

```javascript
// 在浏览器控制台粘贴以下代码模拟 Bridge

window.Bridge = {
  getAuthState: function() {
    return '{"isLoggedIn":true,"jwtValid":true,"jwt":"test_jwt_12345","secret":"test_secret_67890"}';
  },
  applyCredentials: function(json) {
    console.log('Applied credentials:', json);
    try {
      const parsed = JSON.parse(json);
      if (parsed.jwt && parsed.secret) {
        return '{"success":true}';
      } else {
        return '{"error":"缺少字段"}';
      }
    } catch(e) {
      return '{"error":"JSON 格式错误"}';
    }
  },
  buildUploadJsonSports: function(sid, name) {
    return '{"odometer":2.1,"stepCount":3500}';
  },
  submitSportsData: function(json) {
    return '{"code":200,"message":"success"}';
  },
  startOAuthPage: function() {
    console.log('Start OAuth');
  },
  logout: function() {
    console.log('Logout');
  }
};
```

### logcat 查看原生错误

```bash
# 查看所有日志
adb logcat | grep -E "Bridge|WebView|JWT"

# 实时查看 JavaScript 错误
adb logcat | grep "Uncaught"
```

---

## 常用 JSON 格式

### 凭据 JSON
```json
{
  "jwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "secret": "your_secret_key_here"
}
```

### 运动数据 JSON（部分）
```json
{
  "xh": "学号",
  "name": "姓名",
  "odometer": 2.5,
  "stepCount": 3500,
  "avgSpeed": 2.4,
  "maxSpeedPerHour": 3.2,
  "minSpeedPerHour": 1.8,
  "calorie": 120.5,
  "activeTime": "00:10:30",
  "beginTime": "2024-03-22 14:30:00",
  "endTime": "2024-03-22 14:40:30"
}
```

---

## 常见错误排查

| 错误信息 | 原因 | 解决方案 |
|---------|------|--------|
| `Bridge is undefined` | Bridge 未注册 | 检查 WebView.addJavascriptInterface() |
| `JSON 格式不合法` | 返回值不是有效 JSON | 使用 JSONObject，ensure toString() 返回JSON字符串 |
| `缺少 jwt 或 secret 字段` | 凭据不完整 | 确保粘贴的JSON包含这两个字段 |
| `JWT 格式不合法` | JWT 格式错误 | JWT必须是 `xxx.yyy.zzz` 三段式 |
| `粘贴并应用` 后没反应 | Bridge 方法未调用成功 | 检查 logcat 中的异常 |

---

## 文件映射

| 文件 | 用途 |
|------|------|
| `work.html` | 前端页面结构（HTML） |
| `style.css` | 页面样式（CSS） |
| `app.js` | 交互逻辑（JavaScript） |
| `FEATURES_SUMMARY.md` | 功能总结 |
| `ADVANCED_SETTINGS_GUIDE.md` | 详细使用说明 |
| `CREDENTIALS_IMPLEMENTATION.md` | Java 实现代码示例 |

---

## 性能优化建议

✅ **已做好的优化**：
- 底部导航用 `position: fixed` 不影响内容滚动
- CSS 使用 `backdrop-filter: blur()` 实现毛玻璃效果
- textarea 限制最大高度（56vh），避免超出屏幕
- 支持 Clipboard API 和降级方案（兼容性好）

💡 **可进一步优化**：
- 缓存凭据在内存中，减少频繁调用 getAuthState()
- 压缩 JSON 再发送（若数据量大）
- WebView 启用硬件加速

---

## 安全建议清单

- [ ] 凭据存储使用 EncryptedSharedPreferences
- [ ] 不要在日志中打印 JWT 和 Secret
- [ ] 应用退出时清空敏感数据
- [ ] 切换用户前验证指纹识别（可选）
- [ ] 凭据过期时自动提示重新登录
- [ ] 添加操作审计日志（谁什么时候切换了用户）

---

## 快速开始（3分钟）

```
1. ✅ 前端已完成：HTML + CSS + JS 都已更新
2. 👨‍💻 你需要做：在原生代码中添加两个 Bridge 方法
   - getAuthState()
   - applyCredentials()
3. 🧪 测试：在浏览器或 WebView 中测试高级设置功能
4. 🚀 上线：提交代码到 GitHub
```

---

**需要更多帮助？** → 查阅相应的 `.md` 文档 📖

