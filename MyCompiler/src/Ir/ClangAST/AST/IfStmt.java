package Ir.ClangAST.AST;

public class IfStmt extends Stmt{
    private CondStmt cond;
    private Stmt thenPart;
    private Stmt elsePart;

    public IfStmt(CondStmt cond, Stmt thenPart, Stmt elsePart) {
        this.cond = cond;
        this.thenPart = thenPart;
        this.elsePart = elsePart;
    }

    public CondStmt getCond() {
        return cond;
    }

    public Stmt getElsePart() {
        return elsePart;
    }

    public Stmt getThenPart() {
        return thenPart;
    }
}
