package com.example.uptstudenthub;

/** Falha de um pedido, já classificada por {@link ErrorType} (e código HTTP, se existir). */
public final class ExplorerException extends Exception {

    /** Tipo de erro. */
    public final ErrorType type;
    /** Código HTTP (só quando type == HTTP, senão 0). */
    public final int httpCode;

    /**
     * @param type     tipo de erro
     * @param httpCode código HTTP ou 0
     * @param cause    causa original (pode ser null)
     */
    public ExplorerException(ErrorType type, int httpCode, Throwable cause) {
        super(type.name(), cause);
        this.type = type;
        this.httpCode = httpCode;
    }
}
