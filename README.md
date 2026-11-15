# 小黄条 · 桌面待办

基于 React、TypeScript 和 Vite 的待办与便签应用，支持置顶、拖拽排序、完成归档和桌面浮窗视图。默认保存在当前浏览器的本地存储中，清除站点数据会删除本地记录。

## 本地运行

使用 Node.js 20.19+ 或 22.12+，在项目目录执行：

```sh
npm ci
npm run dev
```

打开终端显示的本地地址。运行 `npm run build` 可生成用于静态托管的 `dist/` 文件。

## 云同步与部署

公开源码不预设 Supabase 项目，也不包含个人待办。需要多设备同步时，请使用自己的 Supabase 项目，并为待办及便签设置按登录用户隔离的行级权限。在本机未纳入 Git 的 `.env.local` 中设置 `VITE_SUPABASE_URL` 和 `VITE_SUPABASE_PUBLISHABLE_KEY` 后重启开发服务或重新构建。请只使用可公开的客户端 key，切勿将服务端密钥放入浏览器代码。

仓库内附可手动运行的 GitHub Pages 工作流。上传源码不会自动部署网站。