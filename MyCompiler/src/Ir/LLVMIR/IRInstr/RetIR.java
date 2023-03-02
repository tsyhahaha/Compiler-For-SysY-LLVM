package Ir.LLVMIR.IRInstr;

public class RetIR extends Instr{
    private String returnValue;

    public RetIR(String returnValue) {
        super(new RetIR());
        this.returnValue = returnValue;
    }

    public RetIR() {
        this.returnValue = null;
    }

    @Override
    public String genIr() {
        if(returnValue == null || this.returnValue.equals("")) {
            return "ret void";
        }
        return "ret i32 "+returnValue;
    }
}
