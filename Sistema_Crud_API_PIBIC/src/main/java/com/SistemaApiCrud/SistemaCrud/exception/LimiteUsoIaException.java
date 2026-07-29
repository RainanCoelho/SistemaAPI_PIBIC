package com.SistemaApiCrud.SistemaCrud.exception;

public class LimiteUsoIaException extends RuntimeException {

    private final long segundosAteNovaTentativa;

    public LimiteUsoIaException(String mensagem, long segundosAteNovaTentativa) {
        super(mensagem);
        this.segundosAteNovaTentativa = Math.max(1, segundosAteNovaTentativa);
    }

    public long getSegundosAteNovaTentativa() {
        return segundosAteNovaTentativa;
    }
}
