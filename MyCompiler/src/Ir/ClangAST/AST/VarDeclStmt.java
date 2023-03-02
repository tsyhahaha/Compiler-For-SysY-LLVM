package Ir.ClangAST.AST;

import Exceptions.IrError;

public class VarDeclStmt extends DeclStmt {
    /*
    global: @a = dso_local global i32 2, align 4
            @b = dso_local constant i32 3, align 4
    local:  %2 = alloca i32, align 4
            store i32 2, i32* %2, align 4
     */
    private String name;
    private boolean isGlobal;
    private boolean isConst;
    private Integer value = null;
    private ComputeStmt initial = null;   // const 初始化一定有值
    private boolean isGetInt = false;

    public VarDeclStmt(String name, boolean isGlobal, boolean isConst, ComputeStmt initial) throws IrError {
        this.name = name;
        this.isGlobal = isGlobal;
        this.isConst = isConst;
        this.initial = initial;
        if (this.initial.getValue() != null) {
            this.value = this.initial.getValue();
        }
        if(isGlobal && value == null) {
            value = 0;
        }
    }

    public VarDeclStmt(String name) {
        this.name = name;
        this.isGlobal = false;
        this.isConst = false;
        this.isGetInt = true;
    }
    public VarDeclStmt(String name, boolean isGlobal, boolean isConst, int value) {
        this.name = name;
        this.isGlobal = isGlobal;
        this.isConst = isConst;
        this.value = value;
    }

    /*
    没初始化的全局变量
     */
    public VarDeclStmt(String name, boolean isGlobal) {
        this.isGlobal = isGlobal;
        this.name = name;
        this.isConst = false;
    }

    public String getName() {
        return name;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public boolean isGetInt() {
        return isGetInt;
    }

    public boolean isConst() {
        return isConst;
    }

    public Integer getValue() throws IrError {
        if(this.initial!=null) {
            if(this.initial.getValue() != null) {
                this.value = initial.getValue();
            } else {
                return null;
            }
        }
        return value;
    }

    public ComputeStmt getInitial() {
        return initial;
    }

    public boolean hasValue() {
        return !(initial == null && value == null);
    }
}
