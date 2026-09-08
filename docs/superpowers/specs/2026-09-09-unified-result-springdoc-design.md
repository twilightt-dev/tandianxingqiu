# 统一 Result 与 SpringDoc 接口文档设计

## 背景

项目当前同时存在两个同名响应类：

- `pojo/src/main/java/com/dianping/dto/Result.java`：实际被 Controller、Service、异常处理和测试使用，JSON 字段为 `success/errorMsg/data/total`，没有泛型。
- `common/src/main/java/com/dianping/result/Result.java`：空实现，尚未被使用。

新版 Vue 前端也按照旧的 `success/errorMsg/data` 契约解析响应。此次重构将公共响应模型彻底迁移到 common 模块，并通过 SpringDoc 自动生成现有接口文档。

## 目标

1. 全项目只保留 `com.dianping.result.Result<T>` 一个统一响应类型。
2. 固定 JSON 契约为 `code/msg/data`：`code=1` 表示成功，`code=0` 表示失败。
3. Controller 和必要的 Service 返回值使用准确泛型，使 OpenAPI 能推导 `data` 的具体结构。
4. SpringDoc 自动扫描已有接口，可直接访问 OpenAPI JSON 和 Swagger UI。
5. 同步更新 Vue 前端、单元测试和 E2E Mock，保证契约切换后应用仍能解析响应。

## 非目标

- 不新增当前不存在的 Controller 接口。
- 不补充博客详情、点赞列表、他人博客、关注等业务逻辑。
- 不修改数据库结构、实体字段或现有业务流程。
- 不引入另一套兼容响应或保留旧 JSON 字段。
- 不为生成文档而伪造接口、返回值或示例业务数据。

## 公共响应契约

`common/src/main/java/com/dianping/result/Result.java` 使用泛型并实现 `Serializable`：

```java
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success();
    public static <T> Result<T> success(T data);
    public static <T> Result<T> error(String msg);
}
```

约束：

| 场景 | code | msg | data |
|---|---:|---|---|
| 无数据成功 | 1 | `null` | `null` |
| 有数据成功 | 1 | `null` | 具体泛型数据 |
| 业务失败 | 0 | 错误信息 | `null` |

不保留 `ok/fail` 别名，不保留 `success/errorMsg/total` 字段，避免形成隐性双标准。分页信息需要时应作为分页 DTO 放在 `data` 中。

## 模块与类型迁移

common 模块继续依赖 Lombok，用于 `@Data`。pojo 中旧的 `com.dianping.dto.Result` 删除；pojo 只负责 DTO、Entity 等数据对象。

server 中以下层级全部切换到 `com.dianping.result.Result`：

- 所有包含接口方法的 Controller。
- `UserService`、`IVoucherService` 及其实现，因为 Controller 直接返回其结果。
- `WebExceptionAdvice`。
- 使用 Result 的测试。

返回类型按真实结果声明，例如：

| 接口类型 | 返回泛型示例 |
|---|---|
| 无响应数据 | `Result<Void>` |
| 登录 Token | `Result<String>` |
| 新增后的 ID | `Result<Long>` |
| 当前用户 | `Result<UserDTO>` |
| 单个门店 | `Result<Shop>` |
| 门店/博客/优惠券列表 | `Result<List<...>>` |

空的 `FollowController` 和 `BlogCommentsController` 不增加接口，仅允许添加文档分组注解。

## SpringDoc 设计

server 模块引入与 Spring Boot 3.5/Jakarta 兼容的 `springdoc-openapi-starter-webmvc-ui` 2.x 版本。接口文档入口保持 SpringDoc 默认值：

- OpenAPI JSON：`http://localhost:8081/v3/api-docs`
- Swagger UI：`http://localhost:8081/swagger-ui/index.html`

`SecurityConfig` 匿名放行：

- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

每个 Controller 使用 `@Tag` 分组；每个已存在的接口方法使用 `@Operation(summary = "...")`。不通过注解新增或隐藏业务能力。泛型返回值和请求 DTO/Entity 由 SpringDoc 自动形成 schema。

文档主要从后端 8081 端口直接查看，避免 Swagger UI 在 `/api` 反向代理前缀下产生资源和 OpenAPI 地址错配。现有 Nginx `/api/` 业务代理不需要为此改变。

## 前端契约迁移

`frontend/src/types/api.ts` 中的统一响应改为：

```ts
interface ApiResult<T> {
  code: number
  msg?: string | null
  data?: T | null
}
```

`unwrapApiResult` 仅在 `code === 1` 时返回 `data`；其它 code 使用 `msg` 构造 `ApiError`。HTTP 401 仍由 Axios 错误分支负责清理登录状态，不改变现有鉴权行为。

Playwright E2E 内的后端响应 Mock 同步改为 `{ code: 1, data }`。生产源码不保留旧响应兼容判断，使契约偏差能尽早暴露。

## 测试策略

严格采用 RED-GREEN-REFACTOR：

1. common 新增 `ResultTest`，先要求泛型 success/error 契约并观察空 Result 实现导致失败。
2. 前端先将 HTTP 测试期望改为 `code/msg/data`，观察旧解析器失败。
3. Security 测试先断言 SpringDoc 路径匿名可访问，观察当前配置返回未授权。
4. 实现公共 Result，再迁移 server 引用和泛型。
5. 添加 SpringDoc 依赖与 Controller 文档注解。
6. 更新前端解析和 E2E Mock。
7. 执行 common/server 定向测试、server 全量可运行测试、前端全量测试、类型检查、生产构建和后端打包。
8. 启动隔离验证实例或使用测试上下文验证 `/v3/api-docs` 与 Swagger UI；不得依赖补写业务接口。

## 删除与兼容策略

旧 `pojo.dto.Result` 在所有编译引用清零后删除。通过全文搜索保证以下内容不存在：

- `import com.dianping.dto.Result`
- `Result.ok(`
- `Result.fail(`
- 前端对 `success`、`errorMsg`、`total` 的统一响应读取

这是一次原子契约切换：新版后端和新版前端需要一起构建部署。旧前端备份仍使用旧契约，只适合与旧后端产物配套回滚，不能与新版后端混用。

## 风险与控制

| 风险 | 控制措施 |
|---|---|
| 泛型迁移遗漏导致编译失败 | 全模块编译和旧 import 全文扫描 |
| 前端仍读取旧字段 | HTTP 单测、E2E Mock 和生产构建共同验证 |
| 文档路径被 Security 拦截 | Spring Security MockMvc 回归测试 |
| OpenAPI 的 data 退化为 object | Controller 使用具体 `Result<T>`，不使用 raw Result |
| 重构意外补齐业务逻辑 | 文件审查只允许类型、包装方法、文档注解和必要配置变化 |
| 新旧前后端混用 | README/交付报告明确必须成对部署 |

## 验收标准

- common 的 `Result<T>` 与约定字段、工厂方法完全一致。
- pojo 中旧 Result 文件删除，全项目没有旧 import 或旧工厂方法。
- 所有现有 Controller 返回 `Result<T>` 或保持为空 Controller，不存在 raw Result。
- 前端仅解析 `code/msg/data`。
- `/v3/api-docs` 与 `/swagger-ui/index.html` 可访问。
- OpenAPI 中能看到所有现有 Controller 路径，且响应 schema 包含 `code/msg/data`。
- 后端测试、前端测试、类型检查、构建和打包通过；无法运行的外部集成测试必须单独说明原因。
