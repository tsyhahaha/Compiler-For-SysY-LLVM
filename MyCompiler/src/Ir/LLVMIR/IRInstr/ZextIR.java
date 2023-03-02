package Ir.LLVMIR.IRInstr;

public class ZextIR extends Instr {
    private String result;
    private String ty1;
    private String value;
    private String ty2;

    public ZextIR(String result, String ty1, String value,String ty2) {
        super(new ZextIR());
        this.result = result;
        this.ty1 = ty1;
        this.ty2 = ty2;
        this.value = value;
    }

    public ZextIR() { }

    @Override
    public String genIr() {
        return result+" = zext "+ty1+ " "+value+" to "+ty2;
    }
}
