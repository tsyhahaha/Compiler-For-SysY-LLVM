package Ir.LLVMIR.IRInstr;

import utils.Pair;

public class AllocaIR extends Instr {
    private String result;
    private Pair<Integer, Integer> dimension = null;

    public AllocaIR(String result) {
        super(new AllocaIR());
        this.result = result;
    }

    public AllocaIR(String result, Pair<Integer, Integer> dimension) {
        super(new AllocaIR());
        this.result = result;
        this.dimension = dimension;
    }

    public AllocaIR() {
        super();
    }

    @Override
    public String genIr() {
        if (dimension != null) {
            int d1 = dimension.getKey();
            int d2 = dimension.getValue();
            if(d2 == 0) {
                return result+" = alloca ["+d1+" x i32]";
            } else {
                return result+" = alloca ["+d1+" x ["+d2+" x i32]]";
            }
        } else {
            return result + " = alloca i32";
        }
    }
}
