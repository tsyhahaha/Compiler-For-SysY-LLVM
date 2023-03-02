package Ir.ClangAST.AST;

import Exceptions.IrError;

public class CondStmt extends ComputeStmt {
    private Op op;
    private boolean isLeaf;
    private LeafStmt leafValue;
    private Integer leftValue = null;
    private Integer rightValue = null;
    private ComputeStmt leftExpr;
    private ComputeStmt rightExpr;   // CondStmt 的操作数可以是 ComputeStmt

    public CondStmt(Op op, Integer left, Integer right) {
        super(op, true, true);
        this.op = op;
        this.leftValue = left;
        this.rightValue = right;
    }

    public CondStmt(Op op, Integer left, ComputeStmt rightExpr) throws IrError {
        super(op, true, rightExpr.getValue() != null);
        this.op = op;
        this.leftValue = left;
        if (rightExpr.getValue() != null) {
            this.rightValue = rightExpr.getValue();
        }
        this.rightExpr = rightExpr;
    }

    public CondStmt(Op op, ComputeStmt leftExpr, Integer rightValue) throws IrError {
        super(op, leftExpr.getValue() != null, true);
        this.op = op;
        if (leftExpr.getValue() != null) {
            this.leftValue = leftExpr.getValue();
        }
        this.leftExpr = leftExpr;
        this.rightValue = rightValue;
    }

    public CondStmt(Op op, ComputeStmt leftExpr, ComputeStmt rightExpr) throws IrError {
        super(op, leftExpr.getValue() != null, rightExpr.getValue() != null);
        this.op = op;
        this.leftExpr = leftExpr;
        if (leftExpr.getValue() != null) {
            this.leftValue = leftExpr.getValue();
        }
        if (rightExpr.getValue() != null) {
            this.rightValue = rightExpr.getValue();
        }
        this.rightExpr = rightExpr;
    }

    public CondStmt(boolean isLeaf, LeafStmt value) throws IrError {
        super();
        if(value == null) {
            throw new IrError();
        }
        this.isLeaf = isLeaf;
        this.leafValue = value;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public LeafStmt getLeafValue() {
        return leafValue;
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


    @Override
    public Op getOp() {
        return op;
    }

    public ComputeStmt getRightExpr() {
        return rightExpr;
    }

    public ComputeStmt getLeftExpr() {
        return leftExpr;
    }

    public int getLeftValue() {
        return leftValue;
    }

    public int getRightValue() {
        return rightValue;
    }

    @Override
    public Integer getValue() throws IrError {
        if (isLeaf) {
            if(leafValue.getValue() == null) {
                return null;
            }
            if (leafValue instanceof ComputeStmt) {
                return (Integer) leafValue.getValue() > 0 ? 1 : 0;
            } else if (leafValue instanceof MyInteger) {
                return ((MyInteger) leafValue).getValue() > 0 ? 1 : 0;
            } else {
                throw new IrError("error leafValue type in condStmt!");
            }
        }
        Integer left = leftValue;
        Integer right = rightValue;

        if (left == null && right == null) {
            left = leftExpr.getValue();
            right = rightExpr.getValue();
        }
        if (right == null || left == null) {
            // 如果getValue 有值，则二者必有一个不为0
            return null;
        }
        Op op = getOp();
        if (op == Op.and) {
            return (left != 0 && right != 0) ? 1 : 0;
        } else if (op == Op.or) {
            return (left == 1 || right == 1) ? 1 : 0;
        } else if (op == Op.le) {
            return (left <= right) ? 1 : 0;
        } else if (op == Op.lt) {
            return (left < right) ? 1 : 0;
        } else if (op == Op.ge) {
            return (left >= right) ? 1 : 0;
        } else if (op == Op.gt) {
            return (left > right) ? 1 : 0;
        } else if( op == Op.eq) {
            return left.equals(right) ? 1 : 0;
        } else if( op == Op.ne) {
            return !left.equals(right) ? 1 : 0;
        }else {
            throw new IrError("Compute op error: " + op +",left: "+left+", right: "+right);
        }
    }
}
