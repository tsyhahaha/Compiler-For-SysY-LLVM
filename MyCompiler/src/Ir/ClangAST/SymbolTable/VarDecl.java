package Ir.ClangAST.SymbolTable;

import Exceptions.IrError;
import utils.Pair;

import java.util.List;

public class VarDecl {
    private final String name;
    private final boolean isConst;
//    private String type;  // 恒为int类型
    private Integer value = null;
    private Pair<Integer, Integer> dimension;
    private List<Integer> arrayList = null;
    private int level;
    private boolean isFuncParam = false;
    private String place;  // 保存其存储的寄存器编号 %place

    public VarDecl(boolean isConst, String name, Integer value,
                   int level, Pair<Integer, Integer> dimension) {
        this.isConst = isConst;
        this.name = name;
        this.value = value;
        this.level = level;
        this.dimension = dimension;
    }

    public VarDecl(boolean isConst, boolean isArray, String name,
                   List<Integer> arrayList, int level, Pair<Integer, Integer> dimension) {
        assert isArray;
        this.isConst = isConst;
        this.name = name;
        this.arrayList = arrayList;
        this.level = level;
        this.dimension = dimension;
    }

    public VarDecl(boolean isConst, String name, int level, Pair<Integer, Integer> dimension) {
        assert !isConst;
        this.isConst = false;
        this.name = name;
        this.level = level;
        this.dimension = dimension;
    }

    public VarDecl(boolean isConst, String name, int level,
                   boolean isFuncParam, Pair<Integer, Integer> dimension) {
        assert !isConst;
        this.isConst = false;
        this.name = name;
        this.level = level;
        this.isFuncParam = isFuncParam;
        this.dimension = dimension;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isFuncParam() {
        return isFuncParam;
    }

    public Pair<Integer, Integer> getDimension() {
        return dimension;
    }

    public Integer getValue() throws IrError {
        return value;
    }

    public Integer getValue(int i, int j) throws IrError {
        if(arrayList == null) {
            return null;
        }
        if(this.isConst) {
            if(level == 1) {
                assert j == 0;
                return arrayList.get(i);
            } else if(level > 1) {
                int d2 = level - 1;
                return arrayList.get(i * d2 + j);
            }
        }
        return null;
    }

    public void setValue(Integer value){
        if(this.level != 0) {
            System.err.println("not 0 level array getValue!");
        }
        if(!isConst) {
            this.value = value;
        }
    }


    public void setPlace(String place) {
        this.place = place;
    }

    public String getPlace() {
        return place;
    }
}
