# Web 工作台更新汇总

## 🎯 最新功能

### 1. 底部三标签页导航

底部固定导航栏包含 3 个标签页：

- **工作台** (`tabWork`) - 核心功能页面
- **关于** (`tabAbout`) - 项目信息与实现说明
- **高级设置** (`tabAdvanced`) - 凭据管理

### 2. 工作台改进

✅ **可编辑 JSON 编辑器**
- 生成的 `UploadJsonSports` JSON 直接在 textarea 中显示
- 用户可修改 JSON 各字段（格式、参数值等）
- 支持实时修改，修改后直接发送修改后的数据

✅ **工具按钮**
- **格式化 JSON** - 重新格式化 JSON（缩进和换行）
- **复制** - 将 JSON 复制到剪贴板（支持 Clipboard API 和降级方案）

✅ **发送修改后的数据**
- 点击"发起 HTTP 请求"时，发送的是 textarea 中当前的内容（已修改过）
- 服务器响应结果直接显示在同一编辑框中

### 3. 高级设置面板

🔐 **凭据管理**

用户可在"高级设置"标签页中：

1. **刷新凭据** - 获取当前登录用户的 JWT 和 Secret
2. **复制** - 将凭据复制到剪贴板（用于分享或备份）
3. **粘贴并应用** - 粘贴他人的凭据 JSON，立即切换到该用户
4. **清除** - 清空编辑框

**凭据格式**（JSON）：
```json
{
  "jwt": "eyJhbGc...",
  "secret": "xxx_secret_xxx"
}
```

---

## 📝 文件改动清单

### HTML (`work.html`)
- ✅ 新增 `#advancedPanel` 高级设置面板
- ✅ 将生成结果 `<pre>` 改为可编辑 `<textarea>`
- ✅ 新增"格式化"和"复制"按钮
- ✅ 将底部导航栏从 2 个标签扩展到 3 个

### CSS (`style.css`)
- ✅ 新增 `.credentials-textarea` 样式（可编辑凭据框）
- ✅ 新增 `.credentials-actions` 样式（按钮组布局）
- ✅ 新增 `.advanced-card` 和 `.credentials-container` 样式
- ✅ 增加 `.output-textarea` min-height 至 800px（更大的编辑区）
- ✅ 将 `.tab-bar` 从 2 列改为 3 列布局

### JavaScript (`app.js`)
- ✅ 新增 `initAdvancedPanel()` 函数处理凭据管理逻辑
- ✅ 新增"格式化 JSON"按钮处理器
- ✅ 新增"复制"按钮处理器（支持现代与兼容模式）
- ✅ 更新"粘贴并应用"按钮，调用 `Bridge.applyCredentials()`
- ✅ 更新"清除"按钮清空凭据框
- ✅ 修改数据提交逻辑，读取 textarea `.value` 而非 `.textContent`

---

## 🔧 原生端需要实现的 Bridge 方法

### 已有方法（保持不变）
- `buildUploadJsonSports(studentId, studentName)` - 生成数据
- `submitSportsData(sportsJsonString)` - 提交数据
- `startOAuthPage()` - 打开登录页
- `logout()` - 退出登录

### 新增方法

#### `getAuthState()` 
返回当前用户凭据（JWT + Secret）

```javascript
// 调用
const authStateJson = window.Bridge.getAuthState();
// 返回格式
{
  "isLoggedIn": true,
  "jwtValid": true,
  "jwt": "...",
  "secret": "..."
}
```

#### `applyCredentials(credentialsJson)`
应用新凭据，切换到指定用户

```javascript
// 调用
const result = window.Bridge.applyCredentials('{"jwt":"...","secret":"..."}');
// 返回格式（成功）
{ "success": true }
// 返回格式（失败）
{ "error": "JWT 格式不合法" }
```

详细说明见 `ADVANCED_SETTINGS_GUIDE.md`

---

## 🎨 UI/UX 改进

### 视觉风格
- 深色主题，蓝色主色调 (`#6ea8fe`)
- Glassmorphism 设计（毛玻璃效果）
- 暗色渐变背景

### 交互优化
- 标签页切换流畅，无刷新
- 底部固定导航，适配刘海屏 (`viewport-fit=cover`)
- 移动端响应式（底部导航改为单列，按钮全宽）
- 所有操作都有成功/错误提示

---

## 📱 移动端适配

- 底部导航考虑安全区 (`env(safe-area-inset-bottom)`)
- 小屏幕上 tab 按钮改为全宽显示
- textarea 最大高度限制为 40-56vh，避免超出屏幕

---

## ✅ 测试检查清单

- [ ] 三个标签页能正常切换
- [ ] 生成 JSON 后可在编辑框修改
- [ ] 修改后发送 HTTP 请求，服务器收到的是修改后的数据
- [ ] "格式化 JSON" 按钮能正确重新格式化
- [ ] "复制" 按钮能正确复制到剪贴板
- [ ] "刷新凭据" 能正确获取当前用户信息
- [ ] "复制凭据" 能复制到剪贴板
- [ ] 粘贴其他用户凭据后，点"粘贴并应用"能切换用户
- [ ] "清除" 按钮能清空凭据框
- [ ] 移动端底部导航显示正确
- [ ] 所有操作都有适当的成功/错误提示

---

## 🔐 安全建议

1. 用户 JWT 和 Secret 不应显示在日志中
2. 凭据应存储在加密的 SharedPreferences 中
3. 考虑在切换用户前要求设备认证（指纹/密码）
4. 应用退出时清空内存中的敏感数据

---

## 📚 相关文档

- 凭据管理详细指南：`ADVANCED_SETTINGS_GUIDE.md`
- GitHub Actions 构建配置：`.github/workflows/android-tag-build.yml`
- 项目脱敏说明：[`../README.md`](../README.md)
