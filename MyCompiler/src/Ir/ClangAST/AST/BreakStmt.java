package Ir.ClangAST.AST;

public class BreakStmt extends Stmt{
    private int line;
    public BreakStmt(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
