package Ir.ClangAST.AST;

import Exceptions.IrError;
import Ir.ClangAST.SymbolTable.SymbolTable;
import utils.Pair;

public class DeclRefExpr extends Expr implements LeafStmt {
    private boolean isLeft;  // true：等式左边；false：等式右边
    private SymbolTable table;
    private String name;
    private Integer value = null;
    private int level;
    private Pair<ComputeStmt, ComputeStmt> dimension = null;
    int line;

    public DeclRefExpr(boolean inWhile, boolean isLeft, String name, SymbolTable table) {
        this.isLeft = isLeft;
        this.name = name;
        this.table = table;
        if (!inWhile) {
            try {
                this.value = table.queryValue(name);
            } catch (IrError ignore) {
            }
        }
    }

    public DeclRefExpr(boolean inWhile, boolean isLeft, String name,
                       SymbolTable table, Pair<ComputeStmt, ComputeStmt> dimension){
        this.isLeft = isLeft;
        this.name = name;
        this.table = table;
        this.dimension = dimension;
        if (!inWhile) {
            try {
                if (dimension != null) {
                    this.value = getArrValue(dimension);
                } else {
                    this.value = table.queryValue(name);
                }
            } catch (IrError ignore) {
            }
        }
    }

    public DeclRefExpr(boolean isLeft, String name, SymbolTable table, int level,
                       int line) {
        this.isLeft = isLeft;
        this.name = name;
        this.table = table;
        this.level = level;
        this.line = line;
    }

    public DeclRefExpr(boolean isLeft, String name, SymbolTable table, int level,
                       int line, Pair<ComputeStmt, ComputeStmt> dimension) {
        this.isLeft = isLeft;
        this.name = name;
        this.table = table;
        this.level = level;
        this.line = line;
        this.dimension = dimension;
        try {
            if (dimension != null) {
                this.value = getArrValue(dimension);
            } else {
                this.value = table.queryValue(name);
            }
        } catch (IrError ignore) { }
    }

    private Integer getArrValue(Pair<ComputeStmt, ComputeStmt> dimension) throws IrError {
        if(!table.isConst(this.name)) {
            return null;
        }
        assert dimension != null;
        ComputeStmt d1 = dimension.getKey();
        ComputeStmt d2 = dimension.getValue();
        if(level > 0) {
            return null;
        } else {
            Integer d1Value = d1.getValue();
            if(d1Value != null) {
                if(d2 == null) {
                    return table.queryValue(name, d1Value, 0);
                } else {
                    if(d2.getValue() != null) {
                        return table.queryValue(name, d1Value, d2.getValue());
                    }
                }
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public Integer getValue() throws IrError {
        if (level > 0) {
            return null;
        }
        if (value != null) {
            return value;
        }
        System.err.println("DeclRefExpr getValue from table:" + name + "---" + table.queryValue(name));
        return table.queryValue(name);
    }

    public int getLevel() {
        return level;
    }

    public String getPlace() throws IrError {
        return table.queryPlace(name);
    }

    public Pair<ComputeStmt, ComputeStmt> getDimension() {
        return dimension;
    }

    public boolean isFuncParam() throws IrError {
        return this.table.isFuncParam(this.name);
    }

    public boolean isLeft() {
        return isLeft;
    }

    public int getLine() {
        return line;
    }
}
