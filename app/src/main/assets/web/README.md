# assets/web

本目录提供应用内 `WebView` 使用的本地前端页面。

## 页面

- `login.html`：登录引导页，调用原生桥接打开统一认证。
- `work.html`：输入学号和姓名，调用原生生成 `UploadJsonSports` 并展示 JSON。
- `index.html`：本地路由页，可根据登录态跳转到登录页或工作页。

## 依赖的原生桥接

- `Bridge.getAuthState()`：读取登录态与 JWT 有效性。
- `Bridge.startOAuthPage()`：打开统一认证页面。
- `Bridge.buildUploadJsonSports(xh, name)`：生成 `UploadJsonSports` JSON。
- `Bridge.logout()`：清理登录态并回到登录页。

## 说明

- 工作页会把最近一次输入的学号和姓名保存在 `localStorage`，便于下次复用。
- 启动时真正的登录判定仍由原生侧完成；本地页面只负责展示与交互。
