# 测试和已知范围

## Windows / Web

先在仓库根执行 `npm ci`，在 `windows/` 执行 `npm ci`。
单元回归（从根目录运行）：

```powershell
node --test windows/test/*.test.cjs
```

覆盖账号账本隔离、旧缓存归属、断网重试、241 条记录分页、CRUD 归属过滤、30 天回收站及恢复、样式参数校验。

完整成品界面验收需要 Playwright：

```powershell
npm install --no-save --package-lock=false @playwright/test
$env:VITE_SUPABASE_URL = "https://fixture.supabase.co"
$env:VITE_SUPABASE_PUBLISHABLE_KEY = "sb_publishable_fixture_only_not_a_real_key"
node windows/build-renderer.cjs
$env:STICKY_ELECTRON = (Resolve-Path windows/node_modules/electron/dist/electron.exe).Path
node windows/test/account-e2e.cjs
```

这里的 URL 和 key 均为虚构值。测试通过 Electron 的独立临时用户档案拦截认证及数据请求、阻断 WebSocket，不使用真实服务。可设置 `STICKY_TEST_BUNDLE` 为待验收成品中的 app.asar，以核验实际打包资源。需要真实 GUI/可用显示环境，部分无显示服务器环境的动画计时不同。

外观设置窗口验收：

```powershell
$env:STICKY_QA_OUT = Join-Path $env:TEMP "sticky-settings-results"
& $env:STICKY_ELECTRON windows/test/ui-smoke.cjs
```

更完整的 GUI 验收包含：注册验证码错误/正确、忘记密码、相同密码提示、两窗口账号切换、离线待办/随笔保留、整进程重启、拖动时先让位再保存、随笔排序跨同机窗口一致、完整版与浮窗按钮样式一致。

## Android

```powershell
cd android
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

JUnit、Robolectric、MockWebServer 和 Compose 测试覆盖输入规则、账号空间隔离、冲突备份、恢复链接校验、REST 所有者过滤、分页、离线持久化、卡片内编辑、回收站、回滑关闭、动态换序与窄屏标题。

模拟环境不等于所有品牌真机验证。发布前在目标手机核对实际手势、键盘、字体缩放、后台恢复、签名更新和你自己的邮件投递。不要在生产数据上跑自动写入测试。

## 当前边界

- 无 iOS 客户端；Windows 是 Electron，Android 是原生 Compose。
- 回收站、随笔手动顺序、Android 冲突备份是设备本地内容。
- 回收站不是云端备份；卸载、清空应用数据会删除本机独有内容。
- 历史私人版本不同的内部地址/签名不会自动迁移到此公开构建。
- 本仓库的自动化测试不证明使用者的 RLS、SMTP、跨公网延迟或服务端配置正确。
