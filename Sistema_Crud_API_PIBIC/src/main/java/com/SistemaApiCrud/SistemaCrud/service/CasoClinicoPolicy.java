package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;

public final class CasoClinicoPolicy {

    private CasoClinicoPolicy() {
    }

    public static void validarRascunho(CasoClinico caso) {
        if (caso.getStatus() != StatusCasoClinico.RASCUNHO) {
            throw new ConflitoEstadoException(
                    "O caso clinico so pode ser alterado enquanto estiver em rascunho");
        }
    }

    public static void validarArquivavel(CasoClinico caso) {
        if (caso.getStatus() == StatusCasoClinico.RASCUNHO) {
            throw new ConflitoEstadoException(
                    "Publique o caso clinico antes de arquiva-lo");
        }
    }
}
