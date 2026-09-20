# xnotes 简体中文版

## 原项目

本仓库是 [xnotes-android](https://github.com/shardulvs/xnotes-android) 的简体中文汉化 fork。原作者：[shardulvs](https://github.com/shardulvs)。保留原项目的 [MIT 许可证](LICENSE)。

## 本 fork 的改动

- 使用 Android 语言资源提供简体中文，修正滑块漏译及部分术语，保留用户文档名称。
- 增加“S Pen 第三方笔双按钮兼容”开关：默认关闭；开启后显示主、副按钮配置，支持 Wacom One 等以橡皮擦输入上报副按钮的兼容笔。
- 主、副按钮可分别选择功能及“按住临时使用／按一下切换”；例如主按钮按住擦除、副按钮切换平移，临时擦除结束后恢复平移。切换模式只在触屏后操作画布。
- 优化按钮切换：松开最后一个有效按钮后保留选择或擦除结果，等待抬笔再恢复画笔，减少误画；补充失焦、取消输入和切换文档的状态清理及回归测试。
- 补齐上游默认画布背景在文件浏览器新建入口中的应用。
- 使用 GitHub Actions 构建并发布签名 APK：[下载 Release](https://github.com/Ruo1024/xnotes-android/releases/latest)。

## 构建方式

需要 JDK 17、Android SDK 36、NDK 27.0.12077973 和 CMake 3.22.1，设置好 `ANDROID_HOME`。

```bash
git clone https://github.com/Ruo1024/xnotes-android.git
cd xnotes-android
JAVA_HOME=/path/to/jdk-17 ./gradlew testDebugUnitTest assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`。自行构建的 Debug 签名与 Release 不同。

正式发布使用 GitHub Actions 的 **Build and publish Chinese APK** 工作流。发布前递增 [.github/release.properties](.github/release.properties) 中的 `versionCode`，再填写版本标签运行；替换已有 Release 时勾选 `replace_existing`。流程会检查新包与最新正式版（以及被替换版）的包名、签名一致，且内部版本号更高。签名从仓库 Secrets 的 `XNOTES_KEYSTORE_BASE64`、`XNOTES_STORE_PASSWORD` 读取；更换签名会影响覆盖安装。
