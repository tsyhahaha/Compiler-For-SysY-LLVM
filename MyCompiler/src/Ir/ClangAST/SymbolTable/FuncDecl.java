package Ir.ClangAST.SymbolTable;


import java.util.List;

public class FuncDecl {
    private String name;
    private List<ParamPT> params;
    private CType returnType;

    public FuncDecl(String name, List<ParamPT> params, CType returnType) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
    }

    public String getName() {
        return name;
    }

    public CType getReturnType() {
        return returnType;
    }

    public List<ParamPT> getParams() {
        return params;
    }
}
