# WebView Bridge 使用说明

## 功能说明

这个应用实现了一个全屏的 WebView，并通过 JavaScript Bridge 提供了登录状态管理功能。

## Bridge API

在 WebView 中的 JavaScript 可以通过 `Bridge` 对象调用以下方法：

### 1. 检查登录状态

```javascript
var isLoggedIn = Bridge.isLogin();
// 返回 true 表示已登录，false 表示未登录
```

### 2. 设置登录状态

```javascript
// 设置为已登录
Bridge.setLogin(true);

// 设置为未登录
Bridge.setLogin(false);
```

## 主要功能

1. **全屏显示**：应用启动后会以全屏模式显示 WebView，隐藏状态栏和导航栏
2. **JavaScript 支持**：WebView 已启用 JavaScript
3. **返回键处理**：按返回键时，如果 WebView 有历史记录则返回上一页，否则退出应用
4. **持久化存储**：登录状态通过 SharedPreferences 存储，应用重启后仍然保留

## 修改 WebView 加载的页面

在 `MainActivity.java` 的 `loadDefaultPage()` 方法中：

### 加载本地 HTML（当前方式）
```java
webView.loadData(htmlData, "text/html; charset=UTF-8", null);
```

### 加载网络 URL
```java
webView.loadUrl("https://your-website.com");
```

## 测试页面

应用默认加载了一个测试页面，包含三个按钮：
- **检查登录状态**：调用 `Bridge.isLogin()` 并显示结果
- **登录**：调用 `Bridge.setLogin(true)` 设置为已登录
- **登出**：调用 `Bridge.setLogin(false)` 设置为未登录

## 注意事项

1. 所有通过 `@JavascriptInterface` 注解的方法都必须在主线程或有适当的线程处理
2. 登录状态存储在应用的 SharedPreferences 中，卸载应用会清除
3. 如果需要加载网络页面，确保 AndroidManifest.xml 中已添加 INTERNET 权限（已添加）

