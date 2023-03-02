package Ir.ClangAST.AST;

import Ir.ClangAST.SymbolTable.SymbolTable;

import java.util.List;

public class CompoundStmt extends Stmt{
    private int endLine;
    private boolean lastReturn;
    private List<Stmt> stmts;
    private SymbolTable table;

    public CompoundStmt(List<Stmt> stmts, SymbolTable table, int endLine) {
        this.endLine = endLine;
        this.stmts = stmts;
        this.table = table;
    }

    public SymbolTable getTable() {
        return table;
    }

    public List<Stmt> getStmts() {
        return stmts;
    }

    public int getEndLine() {
        return endLine;
    }
}
