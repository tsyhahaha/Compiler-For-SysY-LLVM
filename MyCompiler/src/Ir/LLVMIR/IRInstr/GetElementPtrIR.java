package Ir.LLVMIR.IRInstr;

import utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class GetElementPtrIR extends Instr{
    private String result;
    private String aim;
    private Pair<Integer, Integer> dimension;
    private List<String> offset;

    public GetElementPtrIR(String result, String aim,
                           Pair<Integer, Integer> dimension, List<String> offset) {
        this.result= result;
        this.aim = aim;
        this.dimension = dimension;
        this.offset = offset;
    }

    public GetElementPtrIR(String result, String aim, Pair<Integer, Integer> dimension) {
        this.result= result;
        this.aim = aim;
        this.dimension = dimension;
        this.offset = new ArrayList<>();
        offset.add("0");
    }

    @Override
    public String genIr() {
        StringBuilder sb = new StringBuilder();
        sb.append(result).append(" = getelementptr ");
        if(dimension == null) {
            sb.append("i32, ");
            sb.append("i32* ").append(aim);
            for(String off: offset) {
                sb.append(", i32 ").append(off);
            }
            return sb.toString();
        }
        sb.append("[");
        int d1 = dimension.getKey();
        int d2 = dimension.getValue();
        if(d2 == 0) {
            sb.append(d1).append(" x i32], [").append(d1).append(" x i32]* ").append(aim);
            for(String off: offset) {
                sb.append(", i32 ").append(off);
            }
        } else {
            sb.append(d1).append(" x [").append(d2).append(" x i32]], ");
            sb.append("[").append(d1).append(" x [").append(d2).append(" x i32]]* ");
            sb.append(aim);
            for(String off: offset) {
                sb.append(", i32 ").append(off);
            }
        }
        return sb.toString();
    }
}
