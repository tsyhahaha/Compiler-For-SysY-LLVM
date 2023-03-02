package Ir.ClangAST.SymbolTable;

import Exceptions.ErrorType;
import Exceptions.IrError;

import java.util.HashMap;
import java.util.List;

public class FuncTable {
    private HashMap<String, FuncDecl> funcDecls;

    public FuncTable() {
        this.funcDecls = new HashMap<>();
    }

    private FuncTable(HashMap<String, FuncDecl> funcDecls) {
        this.funcDecls = funcDecls;
    }

    public FuncTable clone() {
        return new FuncTable(funcDecls);
    }

    public void put(String name, FuncDecl funcDecl) throws IrError {
        if(this.funcDecls.containsKey(name)) {
            throw new IrError();
        } else {
            this.funcDecls.put(name, funcDecl);
        }
    }

    public boolean containFunc(String name) {
        return this.funcDecls.containsKey(name);
    }

    public CType queryReturnType(String name) throws IrError {
        if(!this.funcDecls.containsKey(name)) {
            throw new IrError(ErrorType.c, "not find function "+name);
        } else {
            return this.funcDecls.get(name).getReturnType();
        }
    }

    public List<ParamPT> queryParams(String name) throws IrError {
        if(!this.funcDecls.containsKey(name)) {
            throw new IrError();
        } else {
            return this.funcDecls.get(name).getParams();
        }
    }

    public int queryParamNum(String name) throws IrError {
        if(!this.funcDecls.containsKey(name)) {
            throw new IrError();
        } else {
            return this.funcDecls.get(name).getParams().size();
        }
    }
}
