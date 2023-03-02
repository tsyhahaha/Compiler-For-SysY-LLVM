package Exceptions;

import FrontEnd.TokenType;

public class ParserError extends MyException{
    private TokenType tokenType;

    public ParserError(TokenType type) {
        this.tokenType = type;
    }

    public ParserError() { }

    public ParserError(ErrorType type, int line) {
        super(type, line);
    }

    public ParserError(String info) {
        System.err.println(info+" not founded!");
    }

    @Override
    public int getLine() {
        return super.getLine();
    }
}
