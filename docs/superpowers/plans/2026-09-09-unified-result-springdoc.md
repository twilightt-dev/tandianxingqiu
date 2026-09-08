# Unified Result and SpringDoc Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the legacy pojo response wrapper with a single generic common-module `Result<T>`, migrate the Vue client to `code/msg/data`, and expose all existing controllers through SpringDoc without adding business endpoints.

**Architecture:** The common module owns the only response envelope. Server controllers and the two service chains that return envelopes use concrete generic types, while SpringDoc derives schemas from those signatures. The Vue Axios boundary consumes only the new envelope, making backend/frontend contract drift fail immediately.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Maven multi-module, Lombok, Spring Security, SpringDoc OpenAPI 2.x, Vue 3, TypeScript, Axios, Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-09-09-unified-result-springdoc-design.md`

## Global Constraints

- JSON response fields are exactly `code`, `msg`, and `data`; success is `code=1`, failure is `code=0`.
- Delete `pojo/src/main/java/com/dianping/dto/Result.java`; do not keep aliases or compatibility fields.
- Do not create or complete any controller endpoint or business logic.
- Every non-empty Controller return type must be a concrete `Result<T>`, not raw `Result`.
- Update the frontend atomically; it must not accept the legacy envelope.
- SpringDoc documents only existing endpoints and is directly available on backend port 8081.
- The workspace currently has no usable Git repository; do not fabricate commits or destructive rollback commands.

---

### Task 1: Implement the common generic Result and migrate the server response chain

**Files:**
- Modify: `dianping/common/src/main/java/com/dianping/result/Result.java`
- Create: `dianping/common/src/test/java/com/dianping/result/ResultTest.java`
- Delete: `dianping/pojo/src/main/java/com/dianping/dto/Result.java`
- Modify: `dianping/server/src/main/java/com/dianping/config/WebExceptionAdvice.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/BlogController.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/ShopController.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/ShopTypeController.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/UploadController.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/UserController.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/VoucherController.java`
- Modify: `dianping/server/src/main/java/com/dianping/controller/VoucherOrderController.java`
- Modify: `dianping/server/src/main/java/com/dianping/service/UserService.java`
- Modify: `dianping/server/src/main/java/com/dianping/service/IVoucherService.java`
- Modify: `dianping/server/src/main/java/com/dianping/service/impl/UserServiceImpl.java`
- Modify: `dianping/server/src/main/java/com/dianping/service/impl/VoucherServiceImpl.java`
- Modify: `dianping/server/src/test/java/com/dianping/controller/UploadControllerTest.java`
- Modify: `dianping/server/src/test/java/com/dianping/service/UserServiceImplTest.java`

**Interfaces:**
- Produces: `Result.success()`, `Result.success(T)`, `Result.error(String)` and getters for `code/msg/data`.
- Consumes: existing DTO/entity types and unchanged service logic.

- [ ] **Step 1: Write the failing common Result contract test**

```java
class ResultTest {
    @Test
    void createsTypedSuccessAndErrorEnvelopes() {
        Result<String> success = Result.success("token");
        assertThat(success.getCode()).isEqualTo(1);
        assertThat(success.getMsg()).isNull();
        assertThat(success.getData()).isEqualTo("token");

        Result<Void> error = Result.error("失败");
        assertThat(error.getCode()).isZero();
        assertThat(error.getMsg()).isEqualTo("失败");
        assertThat(error.getData()).isNull();
    }
}
```

- [ ] **Step 2: Run the common test and verify RED**

Run from `dianping`:

```text
mvn -q -pl common -am -Dtest=ResultTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation failure because the placeholder common Result has no generic type, factory methods, or getters.

- [ ] **Step 3: Implement the minimal common Result contract**

```java
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = success();
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
```

- [ ] **Step 4: Run the common test and verify GREEN**

Run the Step 2 command. Expected: `ResultTest` passes.

- [ ] **Step 5: Migrate server imports, factories, and concrete generic signatures**

Use `com.dianping.result.Result` everywhere. Required signatures include:

```java
Result<Void> sendCode(String phone);
Result<String> login(LoginFormDTO loginForm);
Result<List<Voucher>> queryVoucherOfShop(Long shopId);

public Result<Long> saveBlog(...);
public Result<Void> likeBlog(...);
public Result<List<Blog>> queryMyBlog(...);
public Result<List<Blog>> queryHotBlog(...);
public Result<Shop> queryShopById(...);
public Result<Long> saveShop(...);
public Result<Void> updateShop(...);
public Result<List<Shop>> queryShopByType(...);
public Result<List<Shop>> queryShopByName(...);
public Result<List<ShopType>> queryTypeList();
public Result<String> uploadImage(...);
public Result<Void> deleteBlogImg(...);
public Result<Void> sendCode(...);
public Result<String> login(...);
public Result<Void> logout();
public Result<UserDTO> me();
public Result<UserInfo> info(...);
public Result<Long> addVoucher(...);
public Result<List<Voucher>> queryVoucherOfShop(...);
public Result<Void> seckillVoucher(...); // remains the existing unimplemented error response
public Result<Void> handleRuntimeException(...);
```

Replace `Result.ok()` with `Result.success()`, `Result.ok(value)` with `Result.success(value)`, and `Result.fail(message)` with `Result.error(message)`. Do not edit the statements between request handling and response wrapping.

- [ ] **Step 6: Update server tests to the new getters and generic types**

Assertions use:

```java
assertThat(result.getCode()).isEqualTo(1);
assertThat(result.getData()).isEqualTo(expected);
assertThat(result.getMsg()).isEqualTo("手机号格式错误！");
```

Delete the old pojo Result only after all imports are migrated.

- [ ] **Step 7: Verify the migrated server chain**

Run:

```text
mvn -q -pl server -am -Dtest=ResultTest,UploadControllerTest,UserServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all named tests pass and compilation reports no raw Result return warnings introduced by this task.

---

### Task 2: Migrate the Vue HTTP boundary and E2E response fixtures

**Files:**
- Modify: `frontend/src/types/api.ts`
- Modify: `frontend/src/api/http.ts`
- Modify: `frontend/src/api/http.spec.ts`
- Modify: `frontend/e2e/app.spec.ts`

**Interfaces:**
- Consumes: backend `{ code: number, msg?: string | null, data?: T | null }`.
- Produces: unchanged domain API promises containing unwrapped `data`.

- [ ] **Step 1: Change the HTTP unit test expectation and verify RED**

```ts
expect(unwrapApiResult({ code: 1, data: { ok: 1 } })).toEqual({ ok: 1 })
expect(() => unwrapApiResult({ code: 0, msg: '失败' })).toThrow('失败')
```

Change the Axios test adapter response to `{ code: 1, data: 'ok' }`.

Run:

```text
npm test -- --run src/api/http.spec.ts
```

Expected: RED because the implementation still reads `success/errorMsg`.

- [ ] **Step 2: Implement the new TypeScript envelope and unwrap rule**

```ts
export interface ApiResult<T> {
  code: number
  msg?: string | null
  data?: T | null
}

export function unwrapApiResult<T>(result: ApiResult<T>): T {
  if (result.code !== 1) throw new ApiError(result.msg || '请求失败', result.code)
  return result.data as T
}
```

Keep HTTP status 401 handling unchanged.

- [ ] **Step 3: Update E2E fixtures and verify GREEN**

```ts
const result = <T>(data: T) => ({ code: 1, data })
```

Run the focused HTTP test again. Expected: pass.

- [ ] **Step 4: Run the full frontend unit suite and typecheck**

```text
npm test -- --pool=forks --maxWorkers=1
npm run typecheck
```

Expected: all unit tests pass and TypeScript reports no old response fields.

---

### Task 3: Add SpringDoc and document every existing controller

**Files:**
- Modify: `dianping/pom.xml`
- Modify: `dianping/server/pom.xml`
- Modify: `dianping/server/src/main/java/com/dianping/config/SecurityConfig.java`
- Modify: every file under `dianping/server/src/main/java/com/dianping/controller/`
- Modify: `dianping/server/src/test/java/com/dianping/security/SecurityConfigTest.java`
- Create: `dianping/server/src/test/java/com/dianping/docs/OpenApiDocumentationTest.java`

**Interfaces:**
- Produces: `/v3/api-docs`, `/swagger-ui/index.html`, controller tags and operation summaries.
- Consumes: concrete `Result<T>` signatures from Task 1.

- [ ] **Step 1: Add failing security expectations for documentation routes**

Extend `SecurityConfigTest` with anonymous GET requests:

```java
mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
```

The security-only test uses 404 rather than 401 as proof that requests passed authorization and reached the absent SpringDoc handler.

Run:

```text
mvn -q -pl server -am -Dtest=SecurityConfigTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: RED with 401 before the documentation matchers are permitted.

- [ ] **Step 2: Add SpringDoc dependency management and server dependency**

Define a tested SpringDoc 2.x version in the root properties and add:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

to the server module. Permit `/v3/api-docs/**`, `/swagger-ui/**`, and `/swagger-ui.html` in `SecurityConfig`.

- [ ] **Step 3: Verify the security test is GREEN**

Run the Step 1 Maven command. Expected: documentation paths are not rejected with 401.

- [ ] **Step 4: Add documentation annotations without changing controller behavior**

Each controller receives a Chinese `@Tag`, including empty controllers. Every existing mapped method receives an `@Operation(summary = "...")`, for example:

```java
@Tag(name = "用户接口")
@RestController
@RequestMapping("/user")
public class UserController {
    @Operation(summary = "查询当前登录用户")
    @GetMapping("/me")
    public Result<UserDTO> me() { ... }
}
```

Tags are: 用户、门店、门店分类、博客、博客评论、关注、优惠券、优惠券订单、文件上传. Operation summaries must describe only methods already present.

- [ ] **Step 5: Add an OpenAPI model test**

Use SpringDoc's OpenAPI model or MockMvc test support to assert that generated documentation contains representative existing paths and the new envelope schema:

```java
assertThat(openApiJson).contains("/user/me", "/shop/{id}", "/blog/hot");
assertThat(openApiJson).contains("code", "msg", "data");
```

The test must use mocks or an isolated web context and must not connect to MySQL or Redis.

- [ ] **Step 6: Run SpringDoc and server regression tests**

```text
mvn -q -pl server -am -Dtest=ResultTest,SecurityConfigTest,OpenApiDocumentationTest,UploadControllerTest,UserServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all named tests pass without external database dependencies.

---

### Task 4: Remove legacy traces, build both applications, and verify live documentation

**Files:**
- Modify: `frontend/README.md`
- Update: `.superpowers/sdd/2026-09-09-unified-result-springdoc/report.md`
- Mechanically rebuild: `nginx-1.18.0/html/hmdp/`
- Mechanically build: `dianping/server/target/dianping-server.jar`

**Interfaces:**
- Consumes: Tasks 1-3 complete code.
- Produces: deployable matching frontend/backend artifacts and reproducible documentation instructions.

- [ ] **Step 1: Scan for forbidden legacy references and raw controller Result types**

Run searches for:

```text
com.dianping.dto.Result
Result.ok(
Result.fail(
success:
errorMsg
```

Expected: no production response-contract hits. Entity/domain fields coincidentally named `success` are evaluated separately rather than blindly changed.

- [ ] **Step 2: Update README with the atomic deployment contract**

Document:

```text
OpenAPI JSON: http://localhost:8081/v3/api-docs
Swagger UI:   http://localhost:8081/swagger-ui/index.html
Response:     { "code": 1, "msg": null, "data": ... }
```

State that new frontend and backend must be deployed together and that missing business endpoints remain intentionally unimplemented.

- [ ] **Step 3: Run fresh final verification**

Backend:

```text
mvn -q -pl server -am test
mvn -q -pl server -am package -DskipTests
```

Frontend:

```text
npm test -- --pool=forks --maxWorkers=1
npm run typecheck
npm run build
```

If the full backend suite waits on external MySQL/Redis, record that result and retain the complete isolated test command from Task 3 as the authoritative code-contract verification.

- [ ] **Step 4: Verify SpringDoc in an isolated runtime**

Start the built jar on a non-production port with an explicit test JWT secret and upload directory, without replacing the user's running 8081 process. Request `/v3/api-docs` and `/swagger-ui/index.html`, assert HTTP 200, verify representative paths and `Result` fields, then stop only that exact temporary process.

- [ ] **Step 5: Rebuild and deploy the matching frontend artifact**

Build the frontend to `nginx-1.18.0/html/hmdp` only after verifying that path is inside the workspace and the persistent `uploads/` directory is outside it. Remove only stale, unreferenced fingerprint assets by exact path; never recursively delete the site, legacy backup, or uploads.

- [ ] **Step 6: Write the delivery report**

Record RED/GREEN evidence, changed contract, SpringDoc URLs, test counts, package/build outputs, deployment pairing requirement, and confirmation that no controller functionality was added.
