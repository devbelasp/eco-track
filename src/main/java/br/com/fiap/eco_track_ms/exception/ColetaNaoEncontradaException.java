package br.com.fiap.eco_track_ms.exception;

public class ColetaNaoEncontradaException extends RuntimeException {
    public ColetaNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}