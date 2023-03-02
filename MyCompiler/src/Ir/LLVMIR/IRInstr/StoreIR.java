package Ir.LLVMIR.IRInstr;

public class StoreIR extends Instr{
    private String var;
    private String place;

    public StoreIR(String var, String place) {
        super(new StoreIR());
        this.var = var;
        this.place = place;
    }

    public StoreIR(){super();}


    @Override
    public String genIr() {
        return "store i32 "+var+", i32* "+place;
    }
}
