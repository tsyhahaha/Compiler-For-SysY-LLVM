package Ir.LLVMIR.Component;

import Ir.LLVMIR.IRInstr.GlobalVarIR;

public class GlobalVar {
    GlobalVarIR ir;

    public GlobalVar(GlobalVarIR ir) {
        this.ir = ir;
    }

    public String toString() {
        return ir.genIr() + "\n";
    }
}
