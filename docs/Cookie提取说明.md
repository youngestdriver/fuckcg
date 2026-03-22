# Cookie 提取功能说明

## 实现的功能

已成功实现 WebView 监听功能，当页面加载到 `http://ggtypt.njtech.edu.cn/cgapp-server/cas/appLogin?ticket=XXXX` 时，会自动提取并保存 Set-Cookie。

## 实现细节

### 1. 网络安全配置
为了允许 HTTP 访问，已在 `res/xml/network_security_config.xml` 中配置了网络安全策略：
- 允许 `u.njtech.edu.cn` 及其子域名的 HTTP 访问
- 允许 `ggtypt.njtech.edu.cn` 及其子域名的 HTTP 访问
- 在 `AndroidManifest.xml` 中引用了该配置文件

### 2. WebView 监听
在 `MainActivity.java` 的 `WebViewClient.onPageFinished()` 方法中添加了 URL 监听：
- 检测 URL 是否以 `http://ggtypt.njtech.edu.cn/cgapp-server/cas/appLogin` 开头
- 检测 URL 是否包含 `ticket=` 参数

### 3. Cookie 提取
使用 `CookieManager.getInstance().getCookie(url)` 获取该 URL 的所有 Cookie

### 4. Cookie 存储
- Cookie 保存到 SharedPreferences 中，键名为 `"cookies"`
- 同时设置登录状态 `isLoggedIn` 为 `true`
- 所有数据存储在 `"LoginPrefs"` SharedPreferences 中

### 5. 日志输出
添加了详细的日志输出，方便调试：
- 检测到回调 URL 时输出日志
- 提取到 Cookie 时输出 Cookie 内容
- 保存 Cookie 时输出确认信息

## Bridge API 更新

### 原有方法
- `Bridge.isLogin()` - 检查是否已登录
- `Bridge.setLogin(boolean)` - 设置登录状态

### 新增方法
- `Bridge.getCookies()` - 获取保存的 Cookie 字符串

## 使用示例

### 在 JavaScript 中调用

```javascript
// 检查是否已登录
var isLoggedIn = Bridge.isLogin();
console.log('登录状态：' + isLoggedIn);

// 获取保存的 Cookie
var cookies = Bridge.getCookies();
console.log('保存的 Cookie：' + cookies);
```

## 工作流程

1. 应用启动，加载统一认证登录页面
2. 用户完成登录
3. 页面重定向到 `http://ggtypt.njtech.edu.cn/cgapp-server/cas/appLogin?ticket=XXXX`
4. WebView 检测到该 URL
5. 自动提取 Set-Cookie 并保存
6. 设置登录状态为 true
7. JavaScript 可以通过 `Bridge.isLogin()` 和 `Bridge.getCookies()` 获取登录状态和 Cookie

## 测试方法

1. 运行应用
2. 在 Logcat 中过滤 `MainActivity` 标签
3. 完成登录流程
4. 观察日志输出：
   ```
   D/MainActivity: Detected callback URL: http://ggtypt.njtech.edu.cn/cgapp-server/cas/appLogin?ticket=...
   D/MainActivity: Extracted cookies: ...
   D/MainActivity: Cookies saved to SharedPreferences
   D/MainActivity: Login status set to true
   ```

## 注意事项

1. Cookie 会持久化保存，应用重启后仍然存在
2. 只有卸载应用才会清除保存的 Cookie
3. 如需清除 Cookie，可以调用 `Bridge.setLogin(false)` 并手动清除 SharedPreferences
4. Cookie 格式为字符串，多个 Cookie 之间用分号分隔（如：`cookie1=value1; cookie2=value2`）

