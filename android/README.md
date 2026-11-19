# Sticky Notes Android

Kotlin / Jetpack Compose / Room 原生应用；Android 8.0（API 26）及以上。
当前版本 0.6.0，应用 ID 为 `com.gray.sticky.nativeapp`，显示名为 **Sticky Notes**。

## 环境和配置

- JDK 17
- Android SDK Platform 34、对应 Build Tools（Gradle 可提示安装）
- Gradle Wrapper 8.11.1

在本目录创建未入库的 `local.properties`，例如：

```properties
sdk.dir=C:/Android/Sdk
```

也可通过 Android Studio 配置 SDK。需要云同步时，将
`app/src/main/assets/client.example.json` 复制为同目录 `client.json`，
填入自己的 Supabase URL 和客户端公开 key。没有配置时可离线使用本机笔记本，登录同步不可用。

## 构建和测试

Windows PowerShell：

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

macOS / Linux：

```sh
chmod +x gradlew
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

调试包：`app/build/outputs/apk/debug/app-debug.apk`。
连接并授权自己的测试手机后，可用 `adb install -r` 安装这个文件。

正式版本：

```powershell
.\gradlew.bat :app:assembleRelease
```

生成的 `app/build/outputs/apk/release/app-release-unsigned.apk` 必须先签名才可安装。
推荐通过 Android Studio 的 Generate Signed App Bundle / APK 创建并保存你自己的签名密钥。

仓库附带 Windows 用的 `sign-personal.ps1`：显式传入 SDK、JDK、仓库外 KeyDir、Unsigned 和 Output 参数。脚本要求 SDK 中已有 Build Tools 35.0.0，使用 Windows 当前用户 DPAPI 保护密码文件。跨电脑恢复需另行安全备份密钥和密码；不要只备份绑定当前 Windows 用户的密文。

## 升级

应用 ID 相同且签名相同才能覆盖安装保留数据。仓库没有维护者的签名；
你自己构建的 APK 不能直接覆盖维护者以前提供的私人 APK。建议在自用构建前
更改 applicationId 为自己唯一的 ID，或者先用测试设备验证迁移。不要为了安装而未经备份卸载旧应用。

## 使用

- 顶部标题下显示同步状态；右侧为刷新、账号、回收站入口。
- 回收站是本机删除记录，保留 30 天；恢复后重新同步正常记录。
- 点击待办/随笔文字行内编辑，点保存；左右滑出现操作按钮，反向滑或点空白收起。
- 长按条目排序，相邻条目在松手前让位；置顶和普通记录分别排序。
- 圆点不是完成按钮，通过绿色完成按钮完成。
- 账号登录后，同步与本机编辑分开处理；Android Keystore 加密保存会话令牌，密码不保存。
- 不能安全合并时到账号页查看“冲突备份”。

不要在 issue 或截图里包含验证码、恢复链接、密码或私人记录。
