package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SistemaCrudApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void openApiDeveSerUmYamlValido() {
		try (InputStream input = getClass().getResourceAsStream("/static/openapi.yaml")) {
			assertThat(input).isNotNull();
			Map<String, Object> documento = new Yaml().load(input);
			assertThat(documento).containsEntry("openapi", "3.0.3");
			assertThat(documento).containsKeys("paths", "components");
		} catch (Exception ex) {
			throw new AssertionError("OpenAPI invalido", ex);
		}
	}

}
