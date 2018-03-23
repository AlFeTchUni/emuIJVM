package Assembler;

public class TranslationError extends RuntimeException {
    public TranslationError(String msg) {
        super(msg);
    }
}
