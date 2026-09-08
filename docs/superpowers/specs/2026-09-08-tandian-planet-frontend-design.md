# 探店星球前端现代化设计

## 目标

将 `nginx-1.18.0/html/hmdp` 下由 10 个 HTML 页面组成的 Vue 2 多页面应用，重写为独立的 Vue 3 单页应用。新版名称统一为“探店星球”，保留现有业务功能和后端接口，并采用移动端优先、桌面端宽屏多栏的现代化界面。

## 范围

- 技术栈：Vue 3、Vite、TypeScript、Vue Router、Pinia、Element Plus、Axios。
- 保留验证码登录、密码登录、门店列表和详情、优惠券秒杀、博客浏览和点赞、关注、发布博客、个人主页与资料编辑。
- 不实现旧界面中没有真实业务能力的地图、消息和搜索功能。
- 保持 Spring Boot 现有接口 URL、请求参数、JWT 格式和统一响应结构。
- 新源码放在 `frontend/`；旧静态站点备份为 `nginx-1.18.0/html/hmdp-legacy/`；生产构建输出到 `nginx-1.18.0/html/hmdp/`。

## 应用架构

```text
Vue views
  -> domain API modules
    -> shared Axios client
      -> /api
        -> Vite proxy (development) or Nginx proxy (production)
          -> Spring Boot :8081
```

`src/api/http.ts` 负责 JWT 请求头、`Result<T>` 解包、业务错误和 401 处理。各领域 API 文件只暴露具名业务函数。Pinia 只管理登录用户和 token；列表、详情、分页与表单状态保留在对应页面或组合式函数中。

## 路由

| 页面 | 新路由 | 登录要求 |
|---|---|---|
| 首页 | `/` | 否 |
| 验证码登录 | `/login` | 否 |
| 密码登录 | `/login/password` | 否 |
| 门店列表 | `/shops?type=&name=` | 否 |
| 门店详情 | `/shops/:id` | 否 |
| 博客详情 | `/blogs/:id` | 否 |
| 发布博客 | `/publish` | 是 |
| 我的主页 | `/me` | 是 |
| 编辑资料 | `/me/edit` | 是 |
| 他人主页 | `/users/:id` | 否 |

Vue Router 负责应用内跳转和鉴权。Nginx 使用 `try_files $uri $uri/ /index.html` 支持 history 模式。应用启动时将旧的 `.html` 地址及查询参数转换到新路由，保留外部旧链接兼容性。

## 视觉设计

采用已确认的 A 方案“温暖城市生活”：珊瑚橙为主色，暖白背景，圆角图片卡片，清晰的无衬线字体和轻量阴影。品牌文字和页面标题统一为“探店星球”。

- 小于 768px：单列内容，固定底部导航，触控优先。
- 768px 至 1199px：居中主内容，可折叠辅助导航。
- 1200px 及以上：左侧导航、中央信息流、右侧推荐或用户信息，最大宽度约 1280px。
- 内容和功能保持原样；增加加载、空数据、失败重试和清晰的登录提示状态。

## 组件边界

- `AppShell`：响应式页面框架和全局导航。
- `AppHeader`、`BottomNav`、`DesktopSidebar`：设备相关导航。
- `BlogCard`、`ShopCard`、`VoucherCard`：领域展示组件。
- `PageState`：加载、空数据和失败状态。
- `ImageUploader`：博客图片选择、上传、预览和删除。
- views：负责编排 API、路由参数和页面状态，不直接配置 Axios。

## 错误处理

- 后端 `success: false` 转换为带服务端消息的业务异常。
- HTTP 401 清理 Pinia/sessionStorage 登录状态并跳转登录页。
- 网络或 5xx 错误显示统一提示，同时为列表和详情保留重试入口。
- 上传失败不破坏已成功上传的图片列表；删除只在后端确认后更新界面。
- 所有异步操作都有 loading 状态，避免重复提交。

## 图片持久化

Vite 构建目录是可替换产物，不能兼作用户上传目录。后端上传路径改为配置项 `app.upload-dir`，默认指向项目外部可持久化目录；Nginx 将 `/imgs/` 映射到该目录。随版本发布的旧示例图片会复制到持久目录或继续作为构建静态资源提供。

## 测试与验收

- Vitest：响应解包、价格格式化、旧 URL 转换、分页合并。
- Vue Test Utils：登录校验、点赞/关注交互、组件状态。
- Pinia 测试：token 恢复、登录和退出。
- Router 测试：鉴权与旧链接转换。
- `vue-tsc`：严格类型检查。
- Vite 生产构建：验证资源路径与产物。
- Playwright 或浏览器自动化：覆盖登录、门店、博客、点赞和发布的核心流程；后端不可用时至少完成页面加载和路由回归。

## 迁移与回滚

在生成新产物前备份原 `hmdp` 目录。新版构建成功并完成检查后修改 Nginx 的 SPA 回退和图片映射。出现问题时可将 Nginx root 临时指回 `hmdp-legacy`，后端业务接口无需回滚。

