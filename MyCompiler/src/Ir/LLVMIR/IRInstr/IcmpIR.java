package Ir.LLVMIR.IRInstr;

public class IcmpIR extends Instr implements LogicIR{
    private String result;
    private String cond;
    private String op1;
    private String op2;

    public IcmpIR(String result, String cond, String op1, String op2) {
        super(new IcmpIR());
        this.result = result;
        this.cond = cond;
        this.op1 = op1;
        this.op2 = op2;
    }

    public IcmpIR() {
        super();
    }

    @Override
    public String genIr() {
        return result+" = icmp "+cond+" i32 "+op1+", "+op2;
    }
}
