package Ir.ClangAST.AST;

public class AssignStmt extends Stmt{
    private DeclRefExpr left;
    private Expr right;

    public AssignStmt(DeclRefExpr left, Expr right) {
        this.left = left;
        this.right = right;
    }

    public Expr getRight() {
        return right;
    }

    public DeclRefExpr getLeft() {
        return left;
    }
}
