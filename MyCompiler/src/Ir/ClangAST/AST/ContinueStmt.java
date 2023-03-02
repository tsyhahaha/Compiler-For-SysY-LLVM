package Ir.ClangAST.AST;

public class ContinueStmt extends Stmt{
    private int line;

    public ContinueStmt(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
