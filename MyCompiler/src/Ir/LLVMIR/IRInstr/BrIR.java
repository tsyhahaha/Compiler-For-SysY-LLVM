package Ir.LLVMIR.IRInstr;

import Ir.LLVMIR.LabelSystem.LabelValue;

public class BrIR extends Instr{
    private boolean hasCond;
    private String cond;
    private LabelValue label1;
    private LabelValue label2;
    private String quote = "";

    public BrIR(LabelValue label) {
        super(new BrIR());
        this.hasCond = false;
        this.label1 = label;
    }

    public BrIR(LabelValue label, String quote) {
        super(new BrIR());
        this.hasCond = false;
        this.label1 = label;
        this.quote = quote;
    }

    public BrIR() {
        super();
    }

    public BrIR(String cond, LabelValue label1, LabelValue label2) {
        super(new BrIR());
        hasCond = true;
        this.cond = cond;
        this.label1 = label1;
        this.label2 = label2;
    }

    public BrIR(String cond, LabelValue label1, LabelValue label2, String quote) {
        super(new BrIR());
        hasCond = true;
        this.cond = cond;
        this.label1 = label1;
        this.label2 = label2;
        this.quote = quote;
    }


    @Override
    public String genIr() {
        if(hasCond) {
            return "br i1 "+cond+", label "+label1 + ", label "+label2 + (quote.equals("") ? "":("\t\t\t\t\t;"+quote));
        } else {
            return "br label "+ label1 + (quote.equals("") ? "":("\t\t\t\t\t;"+quote));
        }
    }
}
