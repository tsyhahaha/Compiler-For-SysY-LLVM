package Ir.ClangAST.AST;

import Exceptions.IrError;

public class ComputeStmt extends BinaryOperator implements LeafStmt {
    private Boolean isNot = null;
    private boolean isLeaf;
    private LeafStmt leafValue;
    private Integer leftValue = null;
    private Integer rightValue = null;
    private ComputeStmt leftExpr;
    private ComputeStmt rightExpr;


    public ComputeStmt(Op op, int left, int right) {
        super(op, true, true);
        this.leftValue = left;
        this.rightValue = right;
    }

    public ComputeStmt(Op op, int left, ComputeStmt rightExpr) throws IrError {
        super(op, true, rightExpr.getValue() != null);
        if(op == Op.not) {
            assert left == 0;
            this.isNot = true;  // 只有这种情况才有
        }
        if(rightExpr.getValue() != null) {
            rightValue = rightExpr.getValue();
        }
        this.leftValue = left;
        this.rightExpr = rightExpr;
    }

    public ComputeStmt(Op op, ComputeStmt leftExpr, int rightValue) throws IrError {
        super(op, leftExpr.getValue() != null, true);
        if(leftExpr.getValue() != null) {
            leftValue = leftExpr.getValue();
        }
        this.leftExpr = leftExpr;
        this.rightValue = rightValue;
    }

    public ComputeStmt(Op op, ComputeStmt leftExpr, ComputeStmt rightExpr) throws IrError {
        super(op, leftExpr.getValue() != null, rightExpr.getValue() != null);
        if(leftExpr.getValue() != null) {
            leftValue = leftExpr.getValue();
        }
        if(rightExpr.getValue() != null) {
            rightValue = rightExpr.getValue();
        }
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
    }

    public ComputeStmt(boolean inWhile, LeafStmt leafValue) throws IrError {
        super(null, false, false);
        this.isLeaf = true;
        if(leafValue.getValue()!=null && !inWhile) {
            this.leafValue = new MyInteger((Integer) leafValue.getValue());
        } else {
            this.leafValue = leafValue;
        }
    }

    public ComputeStmt() {
        super();
    }

    public ComputeStmt(Op op, boolean leftLeaf, boolean rightLeaf) {
        super(op, leftLeaf, rightLeaf);
    }

    public void setNot(boolean isNot) {
        this.isNot = isNot;
    }

    public boolean isNot() {
        if(isNot == null) {
            return false;
        }
        return isNot;
    }

    @Override
    public Integer getValue() throws IrError {
        if(this.isLeaf) {
            if(leafValue.getValue() != null) {
                return (Integer) leafValue.getValue();
            } else {
                return null;
            }
        }
        Integer left = leftValue;
        Integer right = rightValue;
        int result;
        if(right == null || left == null) {
            return null;
        }
        Op op = getOp();
        if(op == Op.add) {
            result = left + right;
        } else if(op == Op.minus) {
            result = left - right;
        } else if(op == Op.mult) {
            result = left * right;
        } else if(op == Op.and) {
            result = left & right;
        } else if(op == Op.div) {
            // 数组没写之前统一赋值为0，为防止异常
            if(right == 0) {
                return null;
            }
            result = left / right;
        } else if(op == Op.mod) {
            if(right == 0) {
                return null;
            }
            result = left % right;
        } else {
            throw new IrError("Compute op error: "+op);
        }
        if(this.isNot != null) {     // 如果 not 被 set 过，说明是出现在 cond 中的，反之则 cond 自行判断
            if(isNot) {
                return result == 0 ? 1 : 0;
            } else {
                return result == 0 ? 0 : 1;
            }
        }
        return result;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public LeafStmt getLeafValue() {
        return leafValue;
    }

    public int getRightValue() {
        return rightValue;
    }

    public int getLeftValue() {
        return leftValue;
    }

    @Override
    public Op getOp() {
        return super.getOp();
    }

    @Override
    public boolean isLeftLeaf() throws IrError {
        if(this.leftExpr!=null && this.leftExpr.getValue()!=null) {
            this.leftValue = leftExpr.getValue();
            return true;
        }
        return leftValue != null;
    }

    @Override
    public boolean isRightLeaf() throws IrError {
        if(this.rightExpr!=null && this.rightExpr.getValue()!=null) {
            this.rightValue = rightExpr.getValue();
            return true;
        }
        return rightValue != null;
    }

    public ComputeStmt getLeftExpr() {
        return leftExpr;
    }

    public ComputeStmt getRightExpr() {
        return rightExpr;
    }
}
