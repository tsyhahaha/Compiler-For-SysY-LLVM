package Ir.ClangAST.AST;

import java.util.List;

public class Root {
    private List<VarDeclStmt> globalVars;
    private List<FuncDeclStmt> functions;
    private FuncDeclStmt main;

    public Root(List<VarDeclStmt> globalVars, List<FuncDeclStmt> functions, FuncDeclStmt main) {
        this.globalVars = globalVars;
        this.functions = functions;
        this.main = main;
    }

    public FuncDeclStmt getMain() {
        return main;
    }

    public List<FuncDeclStmt> getFunctions() {
        return functions;
    }

    public List<VarDeclStmt> getGlobalVars() {
        return globalVars;
    }
}
