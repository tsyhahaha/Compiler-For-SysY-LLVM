package Ir.ClangAST.AST;

public class WhileStmt extends Stmt {
    private CondStmt cond;
    private Stmt body;

    public WhileStmt(CondStmt cond, Stmt body) {
        this.cond = cond;
        this.body = body;
    }

    public CondStmt getCond() {
        return cond;
    }

    public Stmt getBody() {
        return body;
    }
}
