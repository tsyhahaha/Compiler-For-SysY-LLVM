package Ir.LLVMIR.IRInstr;

import java.util.List;

public class FuncDeclIR extends Instr {
    private String name;
    private String type;
    private int paramNum;
    private List<Integer> paramsLevel;

    public FuncDeclIR(String name, String type, int paramNum,List<Integer> paramsLevel)  {
        super(new FuncDeclIR());
        this.name = name;
        this.type = type;
        this.paramNum = paramNum;
        this.paramsLevel = paramsLevel;
    }

    public FuncDeclIR() {
        super();
    }

    @Override
    public String genIr() {
        StringBuilder sb = new StringBuilder();
        sb.append("define dso_local ").append(type).append(" @").append(name);
        sb.append("(");
        if(paramNum > 0) {
            int level = paramsLevel.get(0);
            if(level == 0) {
                sb.append("i32");
            } else if(level == 1) {
                sb.append("i32*");
            } else {
                int d2 = level - 1;
                sb.append("[").append(d2).append(" x i32]*");
            }
        }
        for(int i=1; i<paramNum; i++) {
            sb.append(", ");
            int level = paramsLevel.get(i);
            if(level == 0) {
                sb.append("i32");
            } else if(level == 1) {
                sb.append("i32*");
            } else {
                int d2 = level - 1;
                sb.append("[").append(d2).append(" x i32]*");
            }

        }
        sb.append(")");
        return sb.toString();
    }
}
