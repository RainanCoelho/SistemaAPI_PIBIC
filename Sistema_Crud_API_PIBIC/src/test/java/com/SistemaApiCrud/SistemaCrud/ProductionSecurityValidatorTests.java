package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.SistemaApiCrud.SistemaCrud.config.ProductionSecurityValidator;

class ProductionSecurityValidatorTests {

    @Test
    void deveRejeitarSegredoJwtCurto() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(
                "segredo-curto",
                "https://app.exemplo.com");

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("64 caracteres");
    }

    @Test
    void deveRejeitarCorsHttpEmProducao() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(
                "segredo-seguro-com-mais-de-64-caracteres-para-assinatura-jwt-2026",
                "http://app.exemplo.com");

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void deveAceitarConfiguracaoSegura() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(
                "segredo-seguro-com-mais-de-64-caracteres-para-assinatura-jwt-2026",
                "https://app.exemplo.com");

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }
}
