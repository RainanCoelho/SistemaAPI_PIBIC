package com.SistemaApiCrud.SistemaCrud.exception;

public class CapacidadeIaEsgotadaException extends RuntimeException {

    private final long segundosAteNovaTentativa;

    public CapacidadeIaEsgotadaException(
            String mensagem,
            long segundosAteNovaTentativa,
            Throwable causa) {
        super(mensagem, causa);
        this.segundosAteNovaTentativa = Math.max(1, segundosAteNovaTentativa);
    }

    public long getSegundosAteNovaTentativa() {
        return segundosAteNovaTentativa;
    }
}
