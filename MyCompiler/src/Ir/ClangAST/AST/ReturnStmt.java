package Ir.ClangAST.AST;

public class ReturnStmt extends Stmt{
    private int line;
    private ComputeStmt returnExpr;

    public ReturnStmt(int line) {
        this.returnExpr = null;
        this.line = line;
    }

    public ReturnStmt(ComputeStmt returnExpr, int line) {
        this.returnExpr = returnExpr;
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public ComputeStmt getReturnExpr() {
        return returnExpr;
    }
}
