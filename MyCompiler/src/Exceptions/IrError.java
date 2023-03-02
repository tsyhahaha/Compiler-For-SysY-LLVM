package Exceptions;

public class IrError extends MyException{
    private String info;

    public IrError() { }

    public IrError(String info) {
        this.info = info;
        System.err.println(info);
    }

    public IrError(ErrorType type, int line) {
        super(type, line);
    }

    @Override
    public int getLine() {
        return super.getLine();
    }

    public IrError(ErrorType e, String info) {
        System.err.println("ErrorType: "+ e + ": "+info);
    }
}
