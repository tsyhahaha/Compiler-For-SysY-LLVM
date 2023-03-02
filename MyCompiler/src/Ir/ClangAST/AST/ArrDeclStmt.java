package Ir.ClangAST.AST;

import utils.Pair;

import java.util.List;

public class ArrDeclStmt extends VarDeclStmt {
    private List<ComputeStmt> initialList = null;
    private boolean isConst;
    private Pair<Integer, Integer> dimension;

    public ArrDeclStmt(String name, boolean isGlobal, boolean isConst, Pair<Integer, Integer> dimension) {
        super(name, isGlobal);
        this.isConst = isConst;
        this.dimension = dimension;
    }

    public ArrDeclStmt(String name, boolean isGlobal, boolean isConst,
                       Pair<Integer, Integer> dimension, List<ComputeStmt> init) {
        super(name, isGlobal);
        this.isConst = isConst;
        this.dimension = dimension;
        this.initialList = init;
    }

    @Override
    public boolean isConst() {
        return isConst;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    public Pair<Integer, Integer> getDimension() {
        return dimension;
    }

    public List<ComputeStmt> getInitialList() {
        return initialList;
    }
}
