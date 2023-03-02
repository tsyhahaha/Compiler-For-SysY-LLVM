package Ir.LLVMIR.IRInstr;

import Ir.LLVMIR.LabelSystem.LabelValue;
import utils.Regex;

public class Label extends Instr {
    private LabelValue labelValue;
    private String quote = "";

    public Label(LabelValue labelValue) {
        super(new Label());
        this.labelValue = labelValue;
    }

    public Label(LabelValue labelValue, String quote) {
        super(new Label());
        this.quote = quote;
        this.labelValue = labelValue;
    }

    public Label() {
        super();
    }

    @Override
    public String genIr() {
        return ";<label>:"+Regex.parseNum(labelValue.toString()) + ":"+(quote.equals("") ? "":("\t\t\t\t\t\t;"+quote));
    }
}
