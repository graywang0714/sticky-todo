# Sticky Notes：自行配置 Supabase

此文档用于你自己的新项目。本仓库不连接维护者的数据库，不包含服务端凭据。先用测试账号和虚构记录验证；不要直接重建已有业务库。

## 1. 建立数据表

应用使用 public schema 下的两张表，字段名称必须保持一致：

| todos | 类型 | 含义 |
| --- | --- | --- |
| id | text，主键 | 客户端生成的记录 ID |
| user_id | uuid，非空 | 归属用户，关联 auth.users(id) |
| text | text，非空 | 待办正文 |
| done | boolean，非空，默认 false | 完成标记 |
| pinned | boolean，非空，默认 false | 置顶标记 |
| position | double precision，非空，默认 0 | 排序值 |
| created_at | bigint，非空 | Unix 毫秒时间戳 |
| completed_at | bigint，可空 | 完成时间 |

| notes | 类型 | 含义 |
| --- | --- | --- |
| id | text，主键 | 客户端生成的记录 ID |
| user_id | uuid，非空 | 归属用户，关联 auth.users(id) |
| title | text，非空 | 标题 |
| content | text，非空 | 正文 |
| pinned | boolean，非空，默认 false | 置顶标记 |
| created_at | bigint，非空 | 创建时间，Unix 毫秒 |
| updated_at | bigint，非空 | 修改时间，Unix 毫秒 |

用项目的 Table Editor 建表即可。应用通过已登录用户访问两表；可以给 user_id 配置默认值 `auth.uid()`，但权限不能只依赖默认值。

### 必须配置 Row Level Security

两表都开启 RLS。对 `authenticated` 角色分别建立策略：

| 操作 | USING 条件 | WITH CHECK 条件 |
| --- | --- | --- |
| SELECT | `auth.uid() = user_id` | — |
| INSERT | — | `auth.uid() = user_id` |
| UPDATE | `auth.uid() = user_id` | `auth.uid() = user_id` |
| DELETE | `auth.uid() = user_id` | — |

不要创建无条件 `true` 的访问策略。匿名用户不应能够读取、插入、修改或删除记录。在 Data API 设置中确保 public schema 可访问，authenticated 有所需表操作权限。

启用两表的 Realtime / Postgres Changes 发布，以便客户端接收已提交的记录变更。客户端还会在重连或前台时重新核对数据。

## 2. 配置邮箱注册与验证码

在 Authentication 的 Email provider 下开启 Email 登录及 **Confirm email**，保持允许新用户注册。这样旧版本和直接 signup API 也受验证要求约束。

配置自定义 SMTP 发信服务后，在 Emails → Templates 分别修改：
- Confirm sign up
- Magic link / OTP
- Reset password

示例正文：

```html
<h2>Sticky Notes 邮箱验证</h2>
<p>回到应用输入以下验证码：</p>
<p style="font-size:32px;font-weight:bold;letter-spacing:5px">{{ .Token }}</p>
<p>不是本人发起的请求，请忽略本邮件。</p>
```

重置密码邮件可以把标题改为“Sticky Notes 重置密码验证码”。每份模板都单独保存。不要同时要求用户点击链接，应用内直接使用数字验证码即可。

SMTP 主机、端口、用户名和应用密码由你自己的发信服务提供。SMTP 凭据只放在 Supabase 后台，不能写进客户端或仓库。默认发信服务不适合给外部用户长期使用，邮箱送达和额度需自行确认。

URL Configuration 中不能把 localhost 当成生产重置网页。本项目没有部署点击邮件后进入的公网密码重置网站；建议使用上述验证码流程。

## 3. 配置客户端

- Web：复制根目录 `.env.example` 为 `.env.local`。
- Windows：构建 renderer 前设置两个同名环境变量，见 Windows 文档。
- Android：复制 `android/app/src/main/assets/client.example.json` 为同目录 `client.json`。

填自己的 Supabase 项目 URL 和 **publishable key / anon key**。公开客户端 key 会随应用分发，它不是服务器密钥，安全边界是 RLS 和用户会话。绝不能把 secret key、service_role key 或数据库密码装进客户端。

## 4. 上线前验证

使用两个测试用户 A、B：
1. A 注册、验证、登录，建立测试待办和随笔。
2. B 登录，看不到 A 的记录；B 即使知道记录 ID 也不能修改 A 的数据。
3. Windows 和 Android 用 A 登录，核对新增、修改、完成、删除。
4. 断网编辑，退出再登录 A，确认待同步修改继续；切 B 不显示 A 草稿。
5. 找回密码：验证码只在正确用途下有效，旧密码不能继续登录。
6. 用超过 200 条假记录检查分页；检查回收站和本地排序的边界。

本地自动化测试不替代你项目的 RLS、SMTP 和真实设备验收。

参考：[Supabase RLS](https://supabase.com/docs/guides/database/postgres/row-level-security)、[Postgres Changes](https://supabase.com/docs/guides/realtime/postgres-changes)、[邮箱模板](https://supabase.com/docs/guides/auth/auth-email-templates)。
