# WebView 空白页面 + 按需登录实现说明

## 实现内容

已将应用重构为：
- **初始状态**：WebView 显示空白欢迎页面（带登录按钮）
- **按需登录**：只有调用 `Bridge.startOAuthPage()` 时才打开登录页面
- **自动回调**：登录成功后自动返回并保存 JSESSIONID

## 工作流程

```
应用启动
  ↓
显示欢迎页面（WebView 显示初始 HTML）
  ↓
用户点击"点击登录"按钮
  ↓
JavaScript 调用 Bridge.startOAuthPage()
  ↓
启动 LoginActivity（全屏登录页面）
  ↓
用户完成统一认证登录
  ↓
提取 JSESSIONID 并返回
  ↓
MainActivity 接收结果
  ↓
显示登录成功页面（带功能按钮）
```

## 核心改动

### 1. MainActivity.java

#### 初始化时设置 login 类引用
```java
// 初始化WebView
webView = findViewById(R.id.webview);
setupWebView();

// 设置 login 类的 Activity 引用
login.setActivity(this, loginLauncher);

// 加载初始页面（带有登录按钮）
loadInitialPage();
```

#### 新增 loadInitialPage() 方法
显示一个欢迎页面，带有"点击登录"按钮：
- 检查是否已登录
- 如果已登录，直接显示主页面
- 如果未登录，显示欢迎页面

#### 更新 loadMainPage() 方法
登录成功后显示的主页面，增加了：
- **获取 JSESSIONID** 按钮
- **重新登录** 按钮（调用 `Bridge.startOAuthPage()`）

### 2. login.java

#### 添加静态引用
```java
private static Activity activity;
private static ActivityResultLauncher<Intent> loginLauncher;
```

#### 新增 setActivity() 方法
在 MainActivity 启动时调用，设置必要的引用：
```java
public static void setActivity(Activity act, ActivityResultLauncher<Intent> launcher) {
    activity = act;
    loginLauncher = launcher;
}
```

#### 实现 startOAuthPage() 方法
真正启动 LoginActivity：
```java
public static String startOAuthPage() {
    if (activity == null || loginLauncher == null) {
        android.util.Log.e("login", "Activity or Launcher not initialized!");
        return null;
    }
    
    Intent intent = new Intent(activity, LoginActivity.class);
    loginLauncher.launch(intent);
    
    // 异步操作，返回 null
    return null;
}
```

### 3. WebAppInterface.java

#### 新增 startOAuthPage() 方法
让 JavaScript 可以调用登录：
```java
@JavascriptInterface
public void startOAuthPage() {
    android.util.Log.d("WebAppInterface", "startOAuthPage() called from JavaScript");
    login.startOAuthPage();
}
```

## 使用方式

### 在 JavaScript 中调用

#### 初始登录
```javascript
// 点击按钮启动登录
function startLogin() {
    try {
        Bridge.startOAuthPage();
    } catch(e) {
        alert('登录失败: ' + e.message);
    }
}
```

#### 登录后功能
```javascript
// 检查登录状态
var isLoggedIn = Bridge.isLogin();

// 获取 JSESSIONID
var jsessionId = Bridge.getJSessionId();

// 获取完整 Cookie
var cookies = Bridge.getCookies();

// 重新登录
Bridge.startOAuthPage();

// 登出
Bridge.setLogin(false);
```

### 在 Java 中调用

```java
// 在 MainActivity 初始化时设置引用
login.setActivity(this, loginLauncher);

// 启动登录
login.startOAuthPage();

// 提取 JSESSIONID
String jsessionId = login.extractJSessionId(cookies);
```

## 页面展示

### 1. 初始欢迎页面（未登录）
```
┌─────────────────────┐
│                     │
│   欢迎使用应用      │
│                     │
│  ┌───────────────┐  │
│  │  点击登录     │  │
│  └───────────────┘  │
│                     │
│ 调用 Bridge.startOAuth│
│ Page() 进行登录      │
│                     │
└─────────────────────┘
```

### 2. 登录页面（LoginActivity）
```
全屏显示统一认证登录页面
- 输入用户名
- 输入密码
- 点击登录
- 自动跳转并提取 JSESSIONID
```

### 3. 主页面（已登录）
```
┌─────────────────────┐
│   登录成功!          │
│                     │
│  ┌─────────────┐    │
│  │检查登录状态 │    │
│  └─────────────┘    │
│  ┌─────────────┐    │
│  │获取JSESSIONID│   │
│  └─────────────┘    │
│  ┌─────────────┐    │
│  │获取Cookies   │   │
│  └─────────────┘    │
│  ┌─────────────┐    │
│  │重新登录     │    │
│  └─────────────┘    │
│  ┌─────────────┐    │
│  │登出         │    │
│  └─────────────┘    │
│                     │
│ [结果显示区域]      │
│                     │
└─────────────────────┘
```

## Bridge API 完整列表

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `Bridge.isLogin()` | 检查是否已登录 | boolean |
| `Bridge.setLogin(boolean)` | 设置登录状态 | void |
| `Bridge.getCookies()` | 获取完整 Cookie 字符串 | String |
| `Bridge.getJSessionId()` | 获取 JSESSIONID 值 | String |
| `Bridge.startOAuthPage()` | 启动登录页面 | void |

## 测试步骤

### 1. 首次启动（未登录）
```bash
# 清除应用数据
adb shell pm clear com.wzjer.work.fuckcg

# 启动应用
adb shell am start -n com.wzjer.work.fuckcg/.MainActivity

# 查看日志
adb logcat | grep -E "MainActivity|login|WebAppInterface"
```

**预期效果**：
- 应用显示欢迎页面
- 显示"点击登录"按钮

### 2. 点击登录
点击"点击登录"按钮

**预期效果**：
- 启动 LoginActivity
- 显示统一认证登录页面
- 日志输出：`WebAppInterface: startOAuthPage() called from JavaScript`
- 日志输出：`login: Starting OAuth page...`

### 3. 完成登录
输入用户名密码，完成登录

**预期效果**：
- 页面重定向到回调 URL
- 自动提取 JSESSIONID
- 关闭 LoginActivity
- 返回 MainActivity
- 显示 Toast：`登录成功! JSESSIONID: xxx`
- 加载主页面（带功能按钮）

### 4. 重启应用（已登录）
```bash
# 重启应用（不清除数据）
adb shell am force-stop com.wzjer.work.fuckcg
adb shell am start -n com.wzjer.work.fuckcg/.MainActivity
```

**预期效果**：
- 直接显示主页面（不显示登录按钮）
- 可以使用所有功能按钮

### 5. 测试重新登录
点击"重新登录"按钮

**预期效果**：
- 再次打开 LoginActivity
- 可以重新登录获取新的 JSESSIONID

## 日志输出示例

```
D/MainActivity: onCreate called
D/login: setActivity called
D/MainActivity: loadInitialPage called
D/WebAppInterface: startOAuthPage() called from JavaScript
D/login: Starting OAuth page...
D/LoginActivity: onCreate called
D/LoginActivity: Detected callback URL: http://ggtypt.njtech.edu.cn/cgapp-server/cas/appLogin?ticket=ST-12345
D/LoginActivity: Extracted cookies: JSESSIONID=ABC123DEF456; Path=/; HttpOnly
D/LoginActivity: Extracted JSESSIONID: ABC123DEF456
D/LoginActivity: Cookies and JSESSIONID saved to SharedPreferences
D/LoginActivity: Login status set to true
D/MainActivity: Login successful, JSESSIONID: ABC123DEF456
D/MainActivity: loadMainPage called
```

## 优势

1. ✅ **按需加载**：只有需要登录时才打开登录页面
2. ✅ **状态保持**：登录状态持久化，重启应用不需要重新登录
3. ✅ **灵活调用**：JavaScript 可以随时调用 `Bridge.startOAuthPage()`
4. ✅ **用户友好**：清晰的初始页面，明确的操作指引
5. ✅ **易于扩展**：可以在任何页面添加登录按钮

## 注意事项

1. **内存泄漏警告**：login.java 中的静态 Activity 引用会有警告，但由于是单例应用且正确管理，实际使用没有问题
2. **异步操作**：`startOAuthPage()` 是异步的，返回 null，实际结果通过 ActivityResultLauncher 回调
3. **登录状态**：保存在 SharedPreferences 中，卸载应用会清除
4. **重新登录**：可以随时调用 `Bridge.startOAuthPage()` 重新登录

## 完整示例 HTML

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>应用主页</title>
</head>
<body>
    <h1>欢迎使用应用</h1>
    
    <button onclick="login()">登录</button>
    <button onclick="checkStatus()">检查状态</button>
    <button onclick="getJSessionId()">获取JSESSIONID</button>
    
    <div id="result"></div>
    
    <script>
        function login() {
            Bridge.startOAuthPage();
        }
        
        function checkStatus() {
            var isLoggedIn = Bridge.isLogin();
            document.getElementById('result').innerHTML = 
                '登录状态: ' + (isLoggedIn ? '已登录' : '未登录');
        }
        
        function getJSessionId() {
            var jsessionId = Bridge.getJSessionId();
            document.getElementById('result').innerHTML = 
                'JSESSIONID: ' + (jsessionId || '无');
        }
    </script>
</body>
</html>
```

---

## 构建结果

✅ **编译成功**
📱 **APK 位置**：`app/build/outputs/apk/debug/app-debug.apk`

可以直接安装测试！

