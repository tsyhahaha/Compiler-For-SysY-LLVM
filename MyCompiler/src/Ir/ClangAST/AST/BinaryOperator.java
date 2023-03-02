package Ir.ClangAST.AST;

import Exceptions.IrError;

public class BinaryOperator extends Expr {
    private Op op;
    private boolean leftLeaf = false;
    private boolean rightLeaf=false;

    public BinaryOperator(Op op, boolean leftLeaf, boolean rightLeaf) {
        this.op = op;
        this.leftLeaf = leftLeaf;
        this.rightLeaf = rightLeaf;
    }

    public BinaryOperator() {
    }

    public boolean isLeftLeaf() throws IrError {
        return leftLeaf;
    }

    public boolean isRightLeaf() throws IrError {
        return rightLeaf;
    }

    public Op getOp() {
        return op;
    }

    @Override
    public Object getValue() throws IrError { return null; }
}
