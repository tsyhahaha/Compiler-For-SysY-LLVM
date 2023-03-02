package Ir.LLVMIR.IRInstr;

public class MulIR extends Instr implements ArithIR{
    private String op1;
    private String op2;
    private String result;

    public MulIR(String result, String op1, String op2) {
        super(new MulIR());
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public MulIR(){super();}


    @Override
    public String genIr() {
        return result+" = mul i32 " + op1 + ", "+op2;
    }

}
