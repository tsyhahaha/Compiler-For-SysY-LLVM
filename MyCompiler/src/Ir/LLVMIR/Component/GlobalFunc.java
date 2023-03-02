package Ir.LLVMIR.Component;

import Ir.LLVMIR.IRInstr.*;
import Ir.LLVMIR.LabelSystem.LabelValue;
import utils.Regex;

import java.util.List;

public class GlobalFunc {
    private FuncDeclIR decl;
    private List<Instr> content;
    private Instr lastInstr;

    public GlobalFunc(FuncDeclIR decl, List<Instr> content) {
        this.decl = decl;
        this.content = content;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(decl.genIr());
        sb.append(" #0 {\n");
        for(Instr ir: content) {
            if(ir instanceof Label) {
                if(!(lastInstr instanceof BrIR)) {
                    String label = "%"+Regex.parseNum(ir.genIr());
                    LabelValue labelValue = new LabelValue();
                    labelValue.setLabelValue(label);
                    BrIR insert = new BrIR(labelValue);
                    sb.append("\t").append(insert.genIr()).append("\n");
                }
                sb.append("\n").append(ir.genIr()).append("\n");
            } else {
                sb.append("\t").append(ir.genIr()).append("\n");
            }
            lastInstr = ir;
        }
        if(content.size() > 0 && !(content.get(content.size() - 1) instanceof RetIR)) {
            sb.append("\t").append("ret void\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
