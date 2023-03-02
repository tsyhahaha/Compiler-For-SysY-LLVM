package Ir.LLVMIR.IRInstr;

public class SubIR extends Instr implements ArithIR{
    private String op1;
    private String op2;
    private String result;

    public SubIR(String result, String op1, String op2) {
        super(new SubIR());
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public SubIR(){super();}

    @Override
    public String genIr() {
        return result+" = sub i32 " + op1 + ", "+op2;
    }
}
