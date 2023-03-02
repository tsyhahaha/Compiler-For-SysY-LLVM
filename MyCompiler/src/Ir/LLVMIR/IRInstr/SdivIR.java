package Ir.LLVMIR.IRInstr;

public class SdivIR extends Instr implements ArithIR {
    private String op1;
    private String op2;
    private String result;

    public SdivIR(String result, String op1, String op2) {
        super(new SdivIR());
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public SdivIR() {super();}


    @Override
    public String genIr() {
        return result+" = sdiv i32 " + op1 + ", "+op2;
    }

}
