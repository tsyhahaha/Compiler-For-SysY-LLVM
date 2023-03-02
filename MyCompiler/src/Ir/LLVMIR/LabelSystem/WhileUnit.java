package Ir.LLVMIR.LabelSystem;

import Exceptions.IrError;

public class WhileUnit extends LabelUnit{
    private boolean isTrue = false;
    public WhileUnit() {
        super();
    }

    public void setLabel(Integer index, String label) {
        super.setLabel(index, label);
    }

    public LabelValue getLabel(Integer index) throws IrError {
        return super.getLabel(index);
    }

    public void setTrue() {
        this.isTrue = true;
    }

    public Integer getRef(Integer index) {
        return super.getRef(index);
    }

    public boolean isTrue() {
        return isTrue;
    }
}
