# fuckcg

这是一个用于刷创高体育跑步的项目

## 实现原理：

1. 使用原版创高的签名库 `libAMapSDK_Location_v6_6_0.so` 进行反编译并修改检测逻辑，达到绕过原版的安全检测。
2. 抓包得到上传运动的接口，分析请求参数和加密算法，使用 Java 重新实现加密逻辑，模拟上传运动数据。
3. 对原版创高进行脱壳与逆向，找到相关结构体的生成算法，用于伪造 HTTP 请求

## 致谢

1. 小米 13 Pro，小米最后一代不用高考的ROOT机，为逆向和调试提供了基础
2. [算法助手Pro](https://github.com/Xposed-Modules-Repo/com.junge.algorithmaide). 一个非常强大的 Xposed 模块，用于对应用运行逻辑 / 加密逻辑 / 签名逻辑 的追踪和动态调试
3. MT 论坛的 AcE7755 大佬，提供了对创高体育的脱壳方法和样本 [相关帖子](https://bbs.binmt.cc/thread-100448-1-1.html)
4. Gemini. 用于分析反编译后的混淆代码
5. Copilot. 伟大无须多言
6. 感谢 JokerCG 项目，虽然这个项目也是我写的

## 使用工具

- IDA Pro
- Android Studio
- Jadx
- MT 管理器

## 关于作者

哔哩哔哩：(谁家的小鉴)[https://space.bilibili.com/356620493]

投喂一下：(爱发电)[https://afdian.com/a/wzjer]