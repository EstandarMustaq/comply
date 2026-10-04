package com.complyz.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

class ApiVersioningConfigurationTest {

    @Test
    void appliesVersionPrefixToControllerRoutesWithoutRepeatingItInAnnotations() throws Exception {
        try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestMvcConfiguration.class);
            context.refresh();
            MockMvc mockMvc = webAppContextSetup(context).build();

            mockMvc.perform(get(ApiVersioningConfiguration.API_BASE_PATH + "/probe"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("ok"));

            mockMvc.perform(get("/probe"))
                    .andExpect(status().isNotFound());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(ApiVersioningConfiguration.class)
    static class TestMvcConfiguration {

        @Bean
        VersionedRouteProbe versionedRouteProbe() {
            return new VersionedRouteProbe();
        }
    }

    @RestController
    static class VersionedRouteProbe {

        @GetMapping("/probe")
        String probe() {
            return "ok";
        }
    }
}
