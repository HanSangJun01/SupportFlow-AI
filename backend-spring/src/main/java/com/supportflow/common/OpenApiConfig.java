package com.supportflow.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI supportFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SupportFlow AI Backend API")
                        .version("v1")
                        .description("Backend foundation APIs for tenant workspaces and tenant-scoped tickets"));
    }
}
