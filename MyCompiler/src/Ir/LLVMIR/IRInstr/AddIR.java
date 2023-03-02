package Ir.LLVMIR.IRInstr;

public class AddIR extends Instr implements ArithIR{
    private String op1;
    private String op2;
    private String result;

    public AddIR(String result, String op1, String op2) {
        super(new AddIR());
        this.result = result;
        this.op1 = op1;
        this.op2 = op2;
    }

    public AddIR() {
        super();
    }


    @Override
    public String genIr() {
        return result+" = add i32 " + op1 + ", "+op2;
    }
}
