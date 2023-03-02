package Exceptions;

public class LexerError extends MyException {

    public LexerError(ErrorType type, int line) {
        super(type, line);
    }

    @Override
    public int getLine() {
        return super.getLine();
    }

}
