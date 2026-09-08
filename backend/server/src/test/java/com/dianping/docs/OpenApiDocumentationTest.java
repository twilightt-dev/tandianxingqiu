package com.dianping.docs;

import com.dianping.config.UploadProperties;
import com.dianping.controller.BlogCommentsController;
import com.dianping.controller.BlogController;
import com.dianping.controller.FollowController;
import com.dianping.controller.ShopController;
import com.dianping.controller.ShopTypeController;
import com.dianping.controller.UploadController;
import com.dianping.controller.UserController;
import com.dianping.controller.VoucherController;
import com.dianping.controller.VoucherOrderController;
import com.dianping.service.IBlogService;
import com.dianping.service.IShopService;
import com.dianping.service.IShopTypeService;
import com.dianping.service.IUserInfoService;
import com.dianping.service.IVoucherService;
import com.dianping.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OpenApiDocumentationTest.TestApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenApiDocumentationTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "delete", "patch", "head", "options", "trace");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IBlogService blogService;
    @MockBean
    private IShopService shopService;
    @MockBean
    private IShopTypeService shopTypeService;
    @MockBean
    private IUserInfoService userInfoService;
    @MockBean
    private IVoucherService voucherService;
    @MockBean
    private UserService userService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void exposesExistingOperationsWithTagsAndUnifiedResultSchemas() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode document = objectMapper.readTree(json);

        assertThat(document.at("/paths/~1user~1me").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1shop~1{id}").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1blog~1hot").isMissingNode()).isFalse();

        Set<String> operationTags = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> paths = document.path("paths").fields();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> path = paths.next();
            Iterator<Map.Entry<String, JsonNode>> operations = path.getValue().fields();
            while (operations.hasNext()) {
                Map.Entry<String, JsonNode> operation = operations.next();
                if (HTTP_METHODS.contains(operation.getKey())) {
                    operation.getValue().path("tags")
                            .forEach(tag -> operationTags.add(tag.asText()));
                    assertThat(operation.getValue().path("summary").asText())
                            .as("%s %s 应提供接口摘要", operation.getKey().toUpperCase(), path.getKey())
                            .isNotBlank();
                }
            }
        }
        assertThat(operationTags).containsExactlyInAnyOrder(
                "用户接口", "门店接口", "门店分类接口", "博客接口",
                "优惠券接口", "优惠券订单接口", "文件上传接口");

        assertThat(BlogCommentsController.class.getAnnotation(Tag.class).name())
                .isEqualTo("博客评论接口");
        assertThat(FollowController.class.getAnnotation(Tag.class).name())
                .isEqualTo("关注接口");

        JsonNode schemas = document.at("/components/schemas");
        boolean foundResultSchema = false;
        Iterator<Map.Entry<String, JsonNode>> schemaIterator = schemas.fields();
        while (schemaIterator.hasNext()) {
            Map.Entry<String, JsonNode> schema = schemaIterator.next();
            if (!schema.getKey().startsWith("Result")) {
                continue;
            }
            foundResultSchema = true;
            Set<String> properties = new HashSet<>();
            schema.getValue().path("properties").fieldNames().forEachRemaining(properties::add);
            assertThat(properties).containsExactlyInAnyOrder("code", "msg", "data");
        }
        assertThat(foundResultSchema).isTrue();
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class
    })
    @Import({
            BlogController.class,
            BlogCommentsController.class,
            FollowController.class,
            ShopController.class,
            ShopTypeController.class,
            UploadController.class,
            UserController.class,
            VoucherController.class,
            VoucherOrderController.class
    })
    static class TestApplication {

        @Bean
        UploadProperties uploadProperties() {
            UploadProperties properties = new UploadProperties();
            properties.setDirectory(Path.of(System.getProperty("java.io.tmpdir"), "tandian-openapi-test"));
            return properties;
        }
    }
}
