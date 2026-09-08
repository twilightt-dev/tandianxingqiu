package com.dianping.security;

import com.dianping.config.SecurityConfig;
import com.dianping.dto.UserDTO;
import com.dianping.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig
@WebAppConfiguration
@ContextConfiguration(classes = {
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtUtils.class,
        SecurityConfigTest.TestWebConfig.class
})
@TestPropertySource(properties = {
        "security.jwt.secret=dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LXdpdGgtMzItYnl0ZXM=",
        "security.jwt.ttl=PT30M"
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void codeEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/user/code"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEndpointIsPublicSoExpiredTokensCanBeRemovedLocally() throws Exception {
        mockMvc.perform(post("/user/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/upload/blog/delete").param("name", "/blogs/x.jpg"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void documentationEndpointsArePublic() throws Exception {
        int apiDocsStatus = mockMvc.perform(get("/v3/api-docs"))
                .andReturn().getResponse().getStatus();
        int swaggerUiStatus = mockMvc.perform(get("/swagger-ui/index.html"))
                .andReturn().getResponse().getStatus();

        assertThat(apiDocsStatus).isNotEqualTo(401);
        assertThat(swaggerUiStatus).isNotEqualTo(401);
    }

    @Test
    void protectedEndpointAcceptsValidBearerToken() throws Exception {
        UserDTO user = new UserDTO();
        user.setId(1010L);
        String token = jwtUtils.generateToken(user.getId().toString(), java.util.Map.of());

        mockMvc.perform(get("/private")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/user/code")
        String code() {
            return "ok";
        }

        @GetMapping("/private")
        String privateEndpoint() {
            return "ok";
        }

        @PostMapping("/user/logout")
        String logout() {
            return "ok";
        }
    }
}

