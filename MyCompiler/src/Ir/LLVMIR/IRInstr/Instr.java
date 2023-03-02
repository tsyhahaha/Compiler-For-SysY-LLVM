package Ir.LLVMIR.IRInstr;

import Ir.Generator;

public class Instr {
    public Instr(Instr instr) {
        assert instr != null;
        Generator.lastInstr = instr;
    }

    public Instr() {
    }

    public String genIr() {
        return null;
    }
}
