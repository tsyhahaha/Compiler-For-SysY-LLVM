package Ir.ClangAST.SymbolTable;

public class ParamPT {
    private String name;
    private int level;

    public ParamPT(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }
}
