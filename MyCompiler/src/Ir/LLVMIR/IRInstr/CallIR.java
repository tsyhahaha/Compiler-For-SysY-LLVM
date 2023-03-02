package Ir.LLVMIR.IRInstr;

import Exceptions.IrError;
import Ir.ClangAST.ASTMaker;
import Ir.ClangAST.SymbolTable.ParamPT;
import utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class CallIR extends Instr {
    private String result = null;
    private String type;
    private String funcName;
    private List<Pair<String, Integer>> params;

    public CallIR(String result, String funcName, List<Pair<String, Integer>> params, String type) {
        super(new CallIR());
        this.funcName = funcName;
        this.params = params;
        this.result = result;
        this.type = type;
    }

    public CallIR() {
        super();
    }

    public CallIR(String funcName, List<Pair<String, Integer>> params, String type) {
        super(new CallIR());
        this.funcName = funcName;
        this.params = params;
        this.type = type;
    }

    public String l2s(int level) {
        if(level == 0) {
            return "i32";
        } else if(level == 1) {
            return "i32*";
        } else {
            return "[" + (level - 1) + " x i32]*";
        }
    }

    @Override
    public String genIr() {
        List<ParamPT> pts = new ArrayList<>();
        try {
            pts = ASTMaker.getRoot().queryParams(funcName);
        } catch (IrError e) {  }   //没找到函数原型
        StringBuilder base = new StringBuilder();
        int level;
        if(this.result != null && type.equals("i32")) {
            base.append(result).append(" = ");
        }
        base.append("call ");
        base.append(type).append(" @").append(funcName).append("(");
        if(this.params != null && this.params.size() > 0) {
            Pair<String, Integer> head = params.get(0);
            String headString = head.getValue() == 0 ? head.getKey() : "%"+head.getKey();
            level = pts.get(0).getLevel();
            String l2s = l2s(level);
            base.append(l2s).append(" ").append(headString);
            for(int i = 1; i < params.size(); i++) {
                Pair<String, Integer> pair = params.get(i);
                String pairString = pair.getValue() == 0 ? pair.getKey() : "%"+pair.getKey();
                level = pts.get(i).getLevel();
                l2s = l2s(level);
                base.append(", ").append(l2s).append(" ").append(pairString);
            }
        }
        base.append(")");
        return base.toString();
    }
}
