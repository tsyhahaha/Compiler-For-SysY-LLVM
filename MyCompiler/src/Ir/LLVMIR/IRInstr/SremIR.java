package Ir.LLVMIR.IRInstr;

public class SremIR extends Instr implements ArithIR{
    private String op1;
    private String op2;
    private String result;

    public SremIR(String result, String op1, String op2) {
        super(new SremIR());
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public SremIR(){super();}


    @Override
    public String genIr() {
        return result+" = srem i32 " + op1 + ", "+op2;
    }
}
