package Ir.ClangAST.AST;

import java.util.List;

public class CallExpr extends Expr implements LeafStmt {
    private int line;
    private String funcName;
    private List<Expr> params;
    private String format;

    public CallExpr(String funcName, List<Expr> params, int line) {
        this.line = line;
        this.funcName = funcName;
        this.params = params;
    }

    public CallExpr(boolean isPrintf, String format, List<Expr> params, int line) {
        this.line = line;
        this.funcName = "printf";
        this.format = format;
        this.params = params;
    }

    @Override
    public Integer getValue() {
        // 可能优化的地方，通过 getRoot 可以查看函数信息, 常值函数可直接getValue
        return null;
    }

    public int getLine() {
        return line;
    }

    public String getFuncName() {
        return funcName;
    }

    public List<Expr> getParams() {
        return params;
    }

    public boolean isPrintf() {
        return this.funcName.equals("printf");
    }

    public String getFormat() {
        return format;
    }
}
