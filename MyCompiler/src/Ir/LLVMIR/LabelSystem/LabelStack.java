package Ir.LLVMIR.LabelSystem;

import Exceptions.IrError;

import java.util.Stack;

public class LabelStack {
    private Stack<LabelUnit> labelStack;
    private LabelValue returnLabel;
    private int returnRef;

    public LabelStack() {
        this.labelStack = new Stack<>();
        returnRef = 0;
        returnLabel = new LabelValue();     // 初始化后就不能在改变了，br需要引用该label
    }

    public void push(LabelUnit unit) {
//        System.err.println("get in a whileStmt");
        this.labelStack.push(unit);
    }

    public int size() {
        return labelStack.size();
    }

    public LabelUnit pop() {
//        System.err.println("get out a whileStmt");
        return this.labelStack.pop();
    }

    public LabelUnit peek() throws IrError {
        if(this.labelStack.size() == 0) {
            throw new IrError();
        }
        return this.labelStack.peek();
    }

    public void setReturnLabel(String value) {
        this.returnLabel.setLabelValue(value);
    }

    public LabelValue getReturnLabel() {
        this.returnRef++;
        return returnLabel;
    }

    public boolean needReturnLabel() {
        return this.returnRef > 0;
    }
}
