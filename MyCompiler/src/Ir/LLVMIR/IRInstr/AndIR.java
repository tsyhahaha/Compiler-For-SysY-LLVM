package Ir.LLVMIR.IRInstr;

public class AndIR extends Instr{
    private String result;
    private String op1;
    private String op2;

    public AndIR(String result, String op1, String op2) {
        super(new AddIR());
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public AndIR() {
        super();
    }

    public String genIr() {
        return result+" = and i32 " + op1 + ", "+op2;
    }

}
