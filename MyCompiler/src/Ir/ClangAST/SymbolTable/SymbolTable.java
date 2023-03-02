package Ir.ClangAST.SymbolTable;

import Exceptions.ErrorType;
import Exceptions.IrError;
import Ir.Generator;
import utils.Pair;

import java.util.List;

public class SymbolTable {
    private int num;    // 编号，可能会用得到
    private boolean isRoot;
    private FuncTable funcTable;
    public VarTable varTable;
    private SymbolTable pre = null;

    public String returnReg = null;

    public SymbolTable(boolean isRoot) throws IrError {
        assert isRoot;
        this.isRoot = true;
        this.funcTable = new FuncTable();
        this.varTable = new VarTable();
    }

    public SymbolTable() {
        this.isRoot = false;
        this.funcTable = new FuncTable();
        this.varTable = new VarTable();
    }

    public SymbolTable(boolean isRoot, FuncTable funcTable,
                       VarTable varTable, SymbolTable pre, String returnReg) {
        this.isRoot = isRoot;
        this.funcTable = funcTable;
        this.varTable = varTable;
        this.pre = pre;
        this.returnReg = returnReg;
    }

    public SymbolTable clone() {
        return new SymbolTable(isRoot, this.funcTable.clone(),
                this.varTable.clone(), pre == null ? null : pre.clone(), returnReg);
    }

    public boolean isRoot() {
        return isRoot;
    }

    public void setPre(SymbolTable pre) {
        if (this.pre == null) {
            this.pre = pre;
        } else {
            System.err.println("this table has been setPre twice!");
        }
    }

    public boolean containVar(String name) {
        return this.varTable.containVar(name);
    }

    public boolean containFunc(String name) {
        return this.funcTable.containFunc(name);
    }

    public void setPlace(String name, String place) throws IrError {
        if (this.containVar(name)) {
            this.varTable.setPlace(name, place);
        } else {
            SymbolTable nowTable = this;
            while (nowTable != null && !nowTable.containVar(name)) {
                nowTable = nowTable.getPre();
            }
            if (nowTable == null) {
                throw new IrError();
            } else {
                nowTable.setPlace(name, place);
            }
        }
    }

    public void setValue(String name, Integer value) throws IrError {
        System.err.println("set " + name + " " + value);
        SymbolTable nowTable = findRawTable(name);
        if (nowTable == null) {
            throw new IrError(ErrorType.c, "not find value of " + name);
        } else {
            nowTable.varTable.setValue(name, value);
        }
    }

    public SymbolTable getPre() {
        return pre;
    }

    private SymbolTable findRawTable(String name) {
        SymbolTable nowTable = this;
        while (nowTable != null && !nowTable.containVar(name)) {
            nowTable = nowTable.getPre();
        }
        return nowTable;
    }

    private SymbolTable findTable(String name) {
        if(Generator.inGenerator) {
            return this.findRealTable(name);
        } else {
            return this.findRawTable(name);
        }
    }

    private SymbolTable findRealTable(String name) {
        SymbolTable nowTable = findRawTable(name);
        if(nowTable.varTable.queryPlace(name) == null) {
            System.err.println("nowTable find "+name+"'s place is null!");
            while (nowTable.pre != null) {
                if(nowTable.containVar(name) && nowTable.varTable.queryPlace(name) != null) {
                    break;
                }
                nowTable = nowTable.pre;
            }
        }
        if(nowTable.containVar(name)) {
            return nowTable;
        } else {
            return null;
        }
    }

    public boolean accessVar(String name) {
        return findRawTable(name) != null;
    }

    public void put(String name, VarDecl varDecl) throws IrError {
        if (this.funcTable.containFunc(name)) {
            if (!varTable.containVar(name)) {
                this.varTable.put(name, varDecl);
                if (isRoot) {
                    this.varTable.setPlace(name, name);
                }
            }
            throw new IrError();
        }
        this.varTable.put(name, varDecl);
        if (isRoot) {
            this.varTable.setPlace(name, name);
        }
    }

    public void put(String name, FuncDecl funcDecl) throws IrError {
        if (varTable.containVar(name)) {
            if (!funcTable.containFunc(name) && isRoot) {
                this.funcTable.put(name, funcDecl);
            }
            throw new IrError();
        }
        if (isRoot) {
            this.funcTable.put(name, funcDecl);
        }
    }

    public boolean isConst(String name) throws IrError {
        SymbolTable nowTable = findRawTable(name);
        if (nowTable == null) {
            throw new IrError();
        } else {
            return nowTable.varTable.isConst(name);
        }
    }

    public boolean isFuncParam(String name) throws IrError {
        SymbolTable nowTable = findTable(name);
        if (nowTable == null) {
            throw new IrError();
        } else {
            return nowTable.varTable.isFuncParam(name);
        }
    }

    public Pair<Integer, Integer> getDimension(String name) throws IrError {
        SymbolTable nowTable = findTable(name);
        if (nowTable == null) {
            throw new IrError();
        } else {
            return nowTable.varTable.getDimension(name);
        }
    }

    public Integer queryValue(String name) throws IrError {
        SymbolTable nowTable;
        if(!isConst(name)) {
            return null;
        }
        if(Generator.inGenerator) {
            nowTable = findRealTable(name);
        } else {
            nowTable = findRawTable(name);
        }
        if (nowTable == null) {
            return null;
        } else {
            assert !nowTable.isRoot || nowTable.varTable.queryValue(name) != null;
            return nowTable.varTable.queryValue(name);
        }
    }

    public Integer queryValue(String name, int i, int j) throws IrError {
        SymbolTable nowTable;
        if(Generator.inGenerator) {
            nowTable = findRealTable(name);
        } else {
            nowTable = findRawTable(name);
        }
        if (nowTable == null) {
            return null;
        } else {
            assert !nowTable.isRoot || nowTable.varTable.queryValue(name, i, j) != null;
            return nowTable.varTable.queryValue(name, i, j);
        }
    }

    public Integer queryLevel(String name) throws IrError {
        SymbolTable nowTable = findTable(name);
        if (nowTable == null) {
            throw new IrError(ErrorType.c, "not find level of " + name);
        } else {
            assert !nowTable.isRoot || nowTable.varTable.queryValue(name) != null;
            return nowTable.varTable.queryLevel(name);
        }
    }

    public String queryPlace(String name) throws IrError {
        SymbolTable nowTable = findTable(name);
        if (nowTable == null) {
            throw new IrError();
        } else {
            if (nowTable.isRoot) {
                return "@" + nowTable.varTable.queryPlace(name);
            } else {
                String reg = nowTable.varTable.queryPlace(name);
                if (reg.charAt(0) == '%') {
                    return reg;
                } else {
                    return "%" + nowTable.varTable.queryPlace(name);
                }
            }
        }
    }

    public CType queryReturnType(String name) throws IrError {
        if (isRoot) {
            return this.funcTable.queryReturnType(name);
        } else {
            throw new IrError("not rootTable have funcTable");
        }
    }

    public List<ParamPT> queryParams(String name) throws IrError {
        if (isRoot) {
            return this.funcTable.queryParams(name);
        } else {
            throw new IrError("not rootTable have funcTable");
        }
    }

    public int queryParamNum(String name) throws IrError {
        if (isRoot) {
            return this.funcTable.queryParamNum(name);
        } else {
            throw new IrError("not rootTable have funcTable");
        }
    }

    public void setReturnReg(String returnReg) {
        this.returnReg = returnReg;
    }

    public String getReturnReg() throws IrError {
        if (this.returnReg != null) {
            return this.returnReg;
        }
        SymbolTable nowTable = this.pre;
        while (nowTable != null) {
            if (nowTable.returnReg != null) {
                return nowTable.returnReg;
            } else {
                nowTable = nowTable.pre;
            }
        }
        return "0";
    }
}
