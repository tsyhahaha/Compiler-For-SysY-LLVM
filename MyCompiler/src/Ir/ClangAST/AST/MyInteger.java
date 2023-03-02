package Ir.ClangAST.AST;

public class MyInteger implements LeafStmt{
    private int value;

    public MyInteger(int value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }
}
