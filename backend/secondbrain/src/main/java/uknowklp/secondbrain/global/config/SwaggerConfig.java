package uknowklp.secondbrain.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

	@Value("${swagger.url}")
	private String swaggerUri;
	
	@Bean
	public OpenAPI openAPI() {
		String jwtSchemeName = "JWT";
		SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

		return new OpenAPI()
			.info(new Info()
				.title("SecondBrain API")
				.version("1.0.0")
				.description("SecondBrain API 페이지"))
			.addSecurityItem(securityRequirement)
			.components(new Components()
				.addSecuritySchemes(jwtSchemeName, new SecurityScheme()
					.name(jwtSchemeName)
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("JWT 토큰을 입력하세요 (Bearer 제외)")
				)).addServersItem(new Server().url(swaggerUri));
	}
}
