# ✅ 凭据不显示 - 快速修复指南

## 问题现象
点击"刷新凭据"后，凭据编辑框没有显示 JWT 和 Secret

## 根本原因
Android 端还没有实现 `getAuthState()` Bridge 方法

---

## 🔧 快速修复（3 步）

### 第一步：检查是否在 Android App 中
- ✅ 在 **Android WebView** 中 - 需要实现 Bridge 方法
- ✅ 在 **浏览器** 中 - 会显示示例数据和提示信息

### 第二步：实现 Bridge 方法（Android 端）

在你的 `MainActivity.java` 或 `WebViewBridge.java` 中添加：

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
        Log.e("Bridge", "Error creating auth state", e);
    }
    
    return result.toString();
}
```

### 第三步：重新编译并测试

```bash
# 编译 Android 应用
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 运行应用，登录，进入"高级设置"，点击"刷新凭据"
# 应该看到你的凭据显示出来 ✅
```

---

## 🎯 期望效果

### 修复前
```
⚠️ 当前环境不支持凭据获取（Bridge 未实现）。
已显示示例数据，请在 Android App 中使用。
```

### 修复后
```
✅ 已刷新凭据。

{
  "jwt": "eyJhbGc...",
  "secret": "your_secret_key"
}
```

---

## 🔍 诊断步骤（如果仍未显示）

### 在浏览器 DevTools 中调试

```javascript
// 1. 检查 Bridge 是否存在
console.log(typeof window.Bridge);  // 应该是 'object'

// 2. 检查 getAuthState 方法是否存在
console.log(typeof window.Bridge?.getAuthState);  // 应该是 'function'

// 3. 尝试调用方法
const result = window.Bridge.getAuthState();
console.log(result);  // 应该是 JSON 字符串

// 4. 解析返回值
console.log(JSON.parse(result));  // 应该是包含 jwt 和 secret 的对象
```

### 常见问题排查

| 症状 | 原因 | 解决方案 |
|------|------|--------|
| `Bridge is undefined` | Bridge 未注册 | 检查 WebView.addJavascriptInterface() |
| `getAuthState is not a function` | 方法未实现 | 在 Java 中添加 @JavascriptInterface 方法 |
| `JSON parse error` | 返回值不是 JSON 字符串 | 确保返回的是 `result.toString()` 而不是对象 |
| 显示示例数据 | Bridge 方法不存在或返回 null | 实现 getAuthState 方法 |
| 显示空凭据 | 用户未登录或凭据未保存 | 确保登录后保存了凭据到 SharedPreferences |

---

## 📚 详细文档

如需完整的实现指南和故障排查，请查阅：

- **实现代码** → `CREDENTIALS_IMPLEMENTATION.md`
- **使用指南** → `QUICK_REFERENCE.md`
- **故障排查** → `CREDENTIALS_TROUBLESHOOTING.md`

---

## ✨ 好消息

前端已自动支持以下场景：

✅ **浏览器预览** - 显示示例数据和提示  
✅ **Bridge 不存在** - 显示友好错误信息  
✅ **返回值格式错误** - 显示原值和错误提示  
✅ **凭据为空** - 提示需要登录  

所以现在就算 Bridge 还没实现，你也能看到友好的提示信息！

---

## 🎯 下一步

1. ✅ 在 Android 端实现 `getAuthState()` 方法
2. ✅ 重新编译并运行应用
3. ✅ 完成登录
4. ✅ 进入"高级设置"标签页
5. ✅ 点击"刷新凭据"
6. ✅ 凭据应该立即显示！

---

**需要帮助？查阅 `CREDENTIALS_TROUBLESHOOTING.md` 获取详细诊断步骤。**

