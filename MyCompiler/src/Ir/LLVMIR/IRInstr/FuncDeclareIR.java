package Ir.LLVMIR.IRInstr;

import utils.Pair;

import java.util.List;

public class FuncDeclareIR extends Instr{
    private String type;
    private String name;
    private List<Pair<String, Integer>> params;

    public FuncDeclareIR(String type, String name, List<Pair<String, Integer>> params) {
        super(new FuncDeclareIR());
        this.type = type;
        this.name = name;
        this.params = params;
    }

    public FuncDeclareIR() {
        super();
    }

    @Override
    public String genIr() {
        StringBuilder sb = new StringBuilder();
        sb.append("declare ").append(type);
        sb.append(" @").append(name);
        sb.append("(");
        if(params != null && params.size() > 0) {
            Pair<String, Integer> head = params.get(0);
            sb.append(head.getKey());
            if(head.getValue() == 1) {
                sb.append("*");
            }
            for(int i = 1; i < params.size(); i++) {
                sb.append(", ");
                Pair<String ,Integer> param = params.get(i);
                sb.append(head.getKey());
                if(head.getValue() == 1) {
                    sb.append("*");
                }
            }
        }
        sb.append(")");
        return sb.toString();
    }

}
