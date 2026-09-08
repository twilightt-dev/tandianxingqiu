# 探店星球前端现代化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将旧 Vue 2 多页面前端重写为功能等价、响应式且可测试的“探店星球”Vue 3 单页应用。

**Architecture:** 新建 `frontend/` Vite 工程，以领域 API、Pinia 登录状态、Vue Router 路由和响应式布局为边界。开发环境由 Vite 代理 `/api`，生产构建由 Nginx 提供，用户上传图片使用独立持久目录。

**Tech Stack:** Vue 3、TypeScript、Vite、Vue Router、Pinia、Element Plus、Axios、Vitest、Vue Test Utils、Playwright

**Spec:** `docs/superpowers/specs/2026-09-08-tandian-planet-frontend-design.md`

## Global Constraints

- 品牌名统一为“探店星球”。
- 保留现有业务功能、后端 URL、请求参数、JWT 格式和统一响应结构。
- 移动端优先；桌面端使用左导航、主内容和辅助栏。
- UI 采用珊瑚橙、暖白背景、圆角卡片的“温暖城市生活”方案。
- 不增加地图、消息、SSR、微前端或复杂全局状态。
- 用户上传文件不得写入或依赖可清空的 Vite 构建目录。

---

### Task 1: 工程基础、类型和纯函数

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig*.json`
- Create: `frontend/src/types/api.ts`
- Create: `frontend/src/utils/format.ts`
- Create: `frontend/src/utils/legacy-route.ts`
- Test: `frontend/src/utils/format.spec.ts`
- Test: `frontend/src/utils/legacy-route.spec.ts`

**Interfaces:**
- Produces: `formatPrice(value: number | string | null | undefined): string`
- Produces: `resolveLegacyLocation(pathname: string, search: string): string | null`
- Produces: `ApiResult<T>`, `PageQuery`, and shared domain types.

- [ ] 写价格格式化和旧 URL 转换的失败测试，覆盖分、空值、详情/列表/用户旧地址。
- [ ] 运行 `npm test -- --run src/utils`，确认因模块缺失而失败。
- [ ] 创建 Vite/TypeScript/Vitest 配置和最小纯函数实现。
- [ ] 再次运行测试，确认通过；运行 `npm run typecheck`。

### Task 2: HTTP 客户端、领域 API 与登录状态

**Files:**
- Create: `frontend/src/api/http.ts`
- Create: `frontend/src/api/user.ts`
- Create: `frontend/src/api/shop.ts`
- Create: `frontend/src/api/blog.ts`
- Create: `frontend/src/api/follow.ts`
- Create: `frontend/src/api/upload.ts`
- Create: `frontend/src/stores/auth.ts`
- Test: `frontend/src/api/http.spec.ts`
- Test: `frontend/src/stores/auth.spec.ts`

**Interfaces:**
- Produces: `unwrapApiResult<T>(result: ApiResult<T>): T`
- Produces: `createHttpClient(onUnauthorized: () => void): AxiosInstance`
- Produces: `useAuthStore()` with `token`, `user`, `setSession`, `restore`, and `logout`.

- [ ] 先写统一响应解包、业务失败和 token 恢复/清理测试。
- [ ] 运行目标测试，确认因实现缺失而失败。
- [ ] 实现 HTTP 客户端、领域 API 和最小 auth store。
- [ ] 运行目标测试及全量单元测试，确认通过。

### Task 3: 路由、应用外壳和设计系统

**Files:**
- Create: `frontend/src/main.ts`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/router/index.ts`
- Create: `frontend/src/layouts/AppShell.vue`
- Create: `frontend/src/components/navigation/AppHeader.vue`
- Create: `frontend/src/components/navigation/BottomNav.vue`
- Create: `frontend/src/components/navigation/DesktopSidebar.vue`
- Create: `frontend/src/styles/tokens.css`
- Create: `frontend/src/styles/base.css`
- Test: `frontend/src/router/router.spec.ts`

**Interfaces:**
- Consumes: `resolveLegacyLocation`, `useAuthStore`.
- Produces: named routes `home`, `login`, `password-login`, `shops`, `shop-detail`, `blog-detail`, `publish`, `me`, `edit-profile`, `user-profile`.

- [ ] 写路由表、受保护路由和未登录重定向测试。
- [ ] 运行路由测试，确认失败。
- [ ] 实现路由和应用外壳，建立已确认的响应式设计 token。
- [ ] 运行路由测试、类型检查和最小生产构建。

### Task 4: 登录、首页和共享内容卡片

**Files:**
- Create: `frontend/src/views/auth/LoginView.vue`
- Create: `frontend/src/views/auth/PasswordLoginView.vue`
- Create: `frontend/src/views/home/HomeView.vue`
- Create: `frontend/src/components/content/BlogCard.vue`
- Create: `frontend/src/components/common/PageState.vue`
- Test: `frontend/src/views/auth/LoginView.spec.ts`
- Test: `frontend/src/components/content/BlogCard.spec.ts`

**Interfaces:**
- Consumes: user/blog/shop-type API, auth store, named routes.
- Produces: reusable `BlogCard` events `open` and `like`; reusable `PageState` retry event.

- [ ] 写手机号/验证码校验、倒计时和博客卡片交互测试。
- [ ] 运行目标测试，确认失败。
- [ ] 实现两种登录和响应式首页。
- [ ] 运行目标测试、全量测试和类型检查。

### Task 5: 门店列表和详情

**Files:**
- Create: `frontend/src/views/shop/ShopListView.vue`
- Create: `frontend/src/views/shop/ShopDetailView.vue`
- Create: `frontend/src/components/content/ShopCard.vue`
- Create: `frontend/src/components/content/VoucherCard.vue`
- Test: `frontend/src/components/content/ShopCard.spec.ts`
- Test: `frontend/src/views/shop/ShopDetailView.spec.ts`

**Interfaces:**
- Consumes: shop/voucher API, auth store, named routes.
- Produces: shop filtering, infinite pagination, voucher display and seckill login handling.

- [ ] 写距离格式、卡片导航和未登录秒杀测试。
- [ ] 运行目标测试，确认失败。
- [ ] 实现门店页面和组件。
- [ ] 运行目标测试、全量测试和类型检查。

### Task 6: 博客详情、发布与图片上传

**Files:**
- Create: `frontend/src/views/blog/BlogDetailView.vue`
- Create: `frontend/src/views/blog/PublishBlogView.vue`
- Create: `frontend/src/components/content/ImageUploader.vue`
- Test: `frontend/src/views/blog/BlogDetailView.spec.ts`
- Test: `frontend/src/components/content/ImageUploader.spec.ts`

**Interfaces:**
- Consumes: blog/shop/follow/upload API, auth store, named routes.
- Produces: like/follow actions and `ImageUploader` model value `string[]`.

- [ ] 写点赞/关注与上传成功、失败、删除测试。
- [ ] 运行目标测试，确认失败。
- [ ] 实现博客详情、发布表单和图片上传器。
- [ ] 运行目标测试、全量测试和类型检查。

### Task 7: 用户主页和资料编辑

**Files:**
- Create: `frontend/src/views/user/MyProfileView.vue`
- Create: `frontend/src/views/user/UserProfileView.vue`
- Create: `frontend/src/views/user/EditProfileView.vue`
- Create: `frontend/src/components/profile/ProfileHeader.vue`
- Test: `frontend/src/components/profile/ProfileHeader.spec.ts`
- Test: `frontend/src/views/user/MyProfileView.spec.ts`

**Interfaces:**
- Consumes: user/blog/follow API, auth store, BlogCard.
- Produces: profile display, follow state, feed pagination, edit navigation and logout.

- [ ] 写关注切换、退出和个人博客呈现测试。
- [ ] 运行目标测试，确认失败。
- [ ] 实现用户相关页面和共享资料头部。
- [ ] 运行目标测试、全量测试和类型检查。

### Task 8: 上传目录、Nginx 与生产迁移

**Files:**
- Modify: `dianping/common/src/main/java/com/dianping/constant/SystemConstants.java`
- Modify: `dianping/server/src/main/resources/application.yaml`
- Modify: `dianping/server/src/main/java/com/dianping/controller/UploadController.java`
- Modify: `nginx-1.18.0/conf/nginx.conf`
- Create: `nginx-1.18.0/html/hmdp-legacy/` from current static application
- Build: `nginx-1.18.0/html/hmdp/`

**Interfaces:**
- Produces: configurable `app.upload-dir`; Nginx `/imgs/` persistent mapping and SPA fallback.

- [ ] 添加 Spring 配置绑定或属性注入测试，确认上传目录可从配置覆盖。
- [ ] 运行 Maven 目标测试，确认旧硬编码实现无法满足测试。
- [ ] 实现可配置上传目录并更新 Nginx 配置。
- [ ] 备份旧站点，执行前端生产构建到新静态目录。
- [ ] 运行 Maven 测试、前端测试、类型检查和生产构建。

### Task 9: 核心流程回归与交付检查

**Files:**
- Create: `frontend/e2e/app.spec.ts`
- Create: `frontend/playwright.config.ts`
- Create: `frontend/README.md`

**Interfaces:**
- Consumes: final Vite application and Nginx/Spring deployment contract.
- Produces: repeatable development, test and production build instructions.

- [ ] 写核心页面路由与品牌呈现的 E2E 测试，确认在未启动应用时失败。
- [ ] 启动 Vite 预览并运行 E2E。
- [ ] 完成可访问性和移动/桌面截图检查，修复发现的问题并保持测试绿色。
- [ ] 运行 `npm test -- --run`、`npm run typecheck`、`npm run build` 和后端测试。
- [ ] 核对构建产物不包含旧内联脚本，README 记录启动与回滚方法。

