# Sticky Notes Windows

Electron + React，Windows x64，透明浮窗与完整版共用账号、本机账本和随笔顺序。

## 构建

需要 Windows、Node.js 22.12+ 和 npm。仓库根目录先执行：

```powershell
npm ci
cd windows
npm ci
$env:VITE_SUPABASE_URL = "https://YOUR_PROJECT.supabase.co"
$env:VITE_SUPABASE_PUBLISHABLE_KEY = "YOUR_PUBLIC_CLIENT_KEY"
npm run build:renderer
npx electron-builder --win portable --x64
```

如果不准备云同步，可直接在根目录运行 Web 本地模式，或使用 Android 离线模式。Windows 发布构建要求显式提供自己的云端公开配置，避免生成看似可同步却缺配置的发行包。

生成文件在 `windows/dist/`，当前版本名为 `Sticky-Notes-Windows-v1.9.0.exe`。这份 EXE 带有你配置的云服务地址与客户端公开 key，分享前先完成 RLS 和双账号隔离测试。未经代码签名的个人构建可能收到 Windows 来源提示。

## 操作

- **Ctrl+Alt+G**：显示或隐藏两个浮窗。
- 托盘 → **打开完整版**：账号登录、待办、随笔、回收站。
- 托盘 / 浮窗右键 → **浮窗外观…**：背景色、字色、背景透明度。
- 圆点或图钉：按住拖动排序；不会将任务标为完成。
- 绿色勾 / 橙色图钉 / 红叉：完成、置顶切换、移入本机回收站。
- 窗口置顶独立于单条记录置顶。
- 同组随笔拖动顺序只存在本机；正文和置顶属性照常同步。

应用内部把 `https://sticky-notes.invalid/sticky-todo/` 映射为随包界面资源。
这是 Electron 内部地址，不是需要访问或部署的公网网站；云端请求单独发送给你配置的 Supabase。浏览器不会在外部访问该地址。

## 数据与更新

原地更新同一公开构建时，保留相同 Electron 应用名、应用 ID 和本地映射地址，才能复用 Chromium 用户目录。不清缓存、不删除用户档案；从托盘退出后运行新 EXE。不要同时运行多个旧版本。

维护者以前的私人构建使用不同的内部页面地址。开源构建不会自动读取那份私人缓存，请用原应用备份，再登录同一云账号确认数据完整后迁移；离线独有资料不能假设已经进云端。

## 测试

在仓库根目录运行：

```powershell
node --test windows/test/*.test.cjs
```

完整 Electron 界面测试需要另安装 Playwright，详见 [TESTING](../docs/TESTING.md)。测试使用隔离用户目录和虚构账号，不能改为读取真实用户目录。
