package com.example.uptstudenthub;

/** Indica que a resposta do servidor não tem a estrutura esperada (dados inválidos). */
public class DataException extends Exception {

    /** @param message descrição do problema */
    public DataException(String message) {
        super(message);
    }

    /**
     * @param message descrição do problema
     * @param cause   causa original
     */
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
