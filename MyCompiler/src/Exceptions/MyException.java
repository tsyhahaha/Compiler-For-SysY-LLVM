package Exceptions;

public class MyException extends Exception{
    private ErrorType type;
    private int line;

    public MyException() {}

    public MyException(ErrorType type, int line) {
        this.type = type;
        this.line = line;
    }
    public int getLine() { return line; }

    public ErrorType getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof MyException) {
            return this.line == ((MyException) obj).getLine() && this.type == ((MyException) obj).getType();
        }
        return false;
    }

    @Override
    public String toString() {
        return  line + " " + type;
    }

}
