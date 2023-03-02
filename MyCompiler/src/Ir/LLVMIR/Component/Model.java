package Ir.LLVMIR.Component;

import Ir.LLVMIR.IRInstr.FuncDeclareIR;
import utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class Model {
    private List<FuncDeclareIR> declareIRList;
    private List<GlobalVar> globalVar;
    private List<GlobalFunc> globalFunc;
    private GlobalFunc main;

    public Model(List<GlobalVar> globalVar,List<GlobalFunc> globalFunc,GlobalFunc main) {
        this.globalFunc = globalFunc;
        this.globalVar = globalVar;
        this.main = main;
        declareIRList = new ArrayList<>();
        List<Pair<String, Integer>> params = new ArrayList<>();
        params.add(new Pair<>("i32", 0));
        declareIRList.add(new FuncDeclareIR("i32", "getint", null));
        declareIRList.add(new FuncDeclareIR("void", "putint", params));
        declareIRList.add(new FuncDeclareIR("void", "putch", params));
        params = new ArrayList<>();
        params.add(new Pair<>("i8", 1));
        declareIRList.add(new FuncDeclareIR("void", "putstr", params));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(FuncDeclareIR ir: declareIRList) {
            sb.append(ir.genIr()).append("\n");
        }
        if(declareIRList.size() > 0) {
            sb.append("\n");
        }
        for(GlobalVar var: globalVar) {
            sb.append(var.toString());
        }
        if(globalVar.size() > 0) {
            sb.append("\n");
        }
        for(GlobalFunc func: globalFunc) {
            sb.append(func.toString());
        }
        if(globalFunc.size() > 0) {
            sb.append("\n");
        }
        sb.append(main.toString());
        return sb.toString();
    }
}
