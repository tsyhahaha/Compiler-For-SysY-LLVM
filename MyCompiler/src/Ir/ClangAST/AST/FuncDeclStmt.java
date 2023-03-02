package Ir.ClangAST.AST;

import Ir.ClangAST.SymbolTable.CType;
import Ir.LLVMIR.IRInstr.FuncDeclIR;

import java.util.List;

public class FuncDeclStmt extends Stmt{
    private boolean isMain;
    private String name;
    private List<Integer> paramsLevel=null;
    private int paramNum;
    private CType returnType;
    private CompoundStmt content;

    public FuncDeclStmt(CompoundStmt mainContent) {
        this.isMain = true;
        this.name = "main";
        this.paramNum = 0;
        this.returnType = CType.INT;
        this.content = mainContent;
    }

    public FuncDeclStmt(CType returnType, String name, int paramNum,CompoundStmt content) {
        this.isMain = false;
        this.name = name;
        this.returnType = returnType;
        this.paramNum = paramNum;
        this.content = content;
    }

    public FuncDeclStmt(CType returnType, String name, int paramNum,
                        CompoundStmt content, List<Integer> paramsLevel) {
        this.isMain = false;
        this.name = name;
        this.returnType = returnType;
        this.paramNum = paramNum;
        this.content = content;
        this.paramsLevel = paramsLevel;
    }

    public List<Integer> getParamsLevel() {
        return paramsLevel;
    }

    public boolean isMain() {
        return isMain;
    }

    public CompoundStmt getContent() {
        return content;
    }

    public FuncDeclIR genIr() {
        String type = returnType == CType.VOID ? "void" : "i32";
        return new FuncDeclIR(name, type, paramNum, paramsLevel);
    }

    public int getParamNum() {
        return paramNum;
    }

    public String getName() {
        return name;
    }
}
