package Ir.LLVMIR.IRInstr;

import utils.Pair;

import java.util.List;

public class GlobalVarIR extends Instr {
    private String type;
    private String value;
    private String name;
    private Pair<Integer, Integer> arrayType = null;
    private List<Integer> arrayValue = null;

    public GlobalVarIR(String type, String name, String value) {
        super(new GlobalVarIR());
        this.type = type;
        this.value = value;
        this.name = name;
    }

    public GlobalVarIR(String type, String name, Pair<Integer, Integer> arrayType, List<Integer> arrayValue) {
        super(new GlobalVarIR());
        this.type = type;
        this.name = name;
        this.arrayType = arrayType;
        this.arrayValue = arrayValue;
    }

    public GlobalVarIR() {
        super();
    }


    @Override
    public String genIr() {
        if (this.arrayType != null) {
            int d1 = arrayType.getKey();
            int d2 = arrayType.getValue();
            if (d2 == 0) {
                if (this.arrayValue == null) {
                    return "@" + name + " = dso_local " + type + " [" + d1 + " x i32] " + "zeroinitializer";
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("@").append(name).append(" = dso_local ").append(type);
                    sb.append(" [").append(d1).append(" x i32] ").append("[");
                    sb.append("i32 ").append(this.arrayValue.get(0));   // 这有初值就能保证数组大小
                    for (int i = 1; i < this.arrayValue.size(); i++) {
                        sb.append(", i32 ").append(this.arrayValue.get(i));
                    }
                    sb.append("]");
                    return sb.toString();
                }
            } else {
                if (this.arrayValue == null) {
                    return "@" + name + " = dso_local " + type + " [" + d1 + " x [" + d2 + " x i32]] " + "zeroinitializer";
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("@").append(name).append(" = dso_local ").append(type);
                    sb.append(" [").append(d1).append(" x [").append(d2).append(" x i32]] ").append("[");
                    sb.append("[").append(d2).append(" x i32] [i32 ").append(this.arrayValue.get(0));
                    for(int i=1; i<d2; i++) {
                        sb.append(", i32 ").append(this.arrayValue.get(i));
                    }
                    sb.append("]");
                    for(int i=1; i<d1; i++) {
                        sb.append(", [").append(d2).append(" x i32] [");
                        sb.append("i32 ").append(this.arrayValue.get(i*d2));
                        for(int j=1; j<d2; j++) {
                            sb.append(", i32 ").append(this.arrayValue.get(i*d2+j));
                        }
                        sb.append("]");
                    }
                    sb.append("]");
                    return sb.toString();
                }
            }
        }
        return "@" + name + " = dso_local " + type + " i32 " + value;
    }
}
