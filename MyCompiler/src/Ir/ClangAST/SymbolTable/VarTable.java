package Ir.ClangAST.SymbolTable;

import Exceptions.ErrorType;
import Exceptions.IrError;
import utils.Pair;

import java.util.HashMap;

public class VarTable {
    private HashMap<String, VarDecl> varDecls;

    public VarTable() {
        this.varDecls = new HashMap<>();
    }

    public VarTable(HashMap<String, VarDecl> varDecls) {
        this.varDecls = varDecls;
    }

    public void put(String name, VarDecl varDecl) throws IrError {
        if (this.varDecls.containsKey(name)) {
            throw new IrError();
        } else {
            this.varDecls.put(name, varDecl);
        }
    }

    public VarTable clone() {
        return new VarTable(this.varDecls);
    }

    public boolean containVar(String name) {
        return this.varDecls.containsKey(name);
    }

    public void setPlace(String name, String place) {
        this.varDecls.get(name).setPlace(place);
    }

    public void setValue(String name, Integer value){
        this.varDecls.get(name).setValue(value);
    }

    public boolean isFuncParam(String name) {
        return this.varDecls.get(name).isFuncParam();
    }

    public boolean isConst(String name) {
        return this.varDecls.get(name).isConst();
    }

    public Pair<Integer, Integer> getDimension(String name) {
        return this.varDecls.get(name).getDimension();
    }

    public Integer queryValue(String name) throws IrError {
        return this.varDecls.get(name).getValue();  // 如果为null则说明没有值
    }

    public Integer queryValue(String name, int i, int j) throws IrError {
        return this.varDecls.get(name).getValue(i, j);  // 如果为null则说明没有值
    }

    public Integer queryLevel(String name) {
        return this.varDecls.get(name).getLevel();
    }

    public String queryPlace(String name) {
        return this.varDecls.get(name).getPlace();  // 如果为null则说明没有值
    }
}
