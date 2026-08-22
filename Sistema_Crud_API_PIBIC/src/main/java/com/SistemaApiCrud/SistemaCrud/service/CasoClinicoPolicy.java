package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;

public final class CasoClinicoPolicy {

    private CasoClinicoPolicy() {
    }

    public static void validarRascunho(casos_clinicos caso) {
        if (caso.getStatus() != StatusCasoClinico.RASCUNHO) {
            throw new ConflitoEstadoException(
                    "O caso clinico so pode ser alterado enquanto estiver em rascunho");
        }
    }

    public static void validarArquivavel(casos_clinicos caso) {
        if (caso.getStatus() == StatusCasoClinico.RASCUNHO) {
            throw new ConflitoEstadoException(
                    "Publique o caso clinico antes de arquiva-lo");
        }
    }
}
