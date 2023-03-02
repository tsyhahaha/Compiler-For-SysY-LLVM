package Ir.LLVMIR.LabelSystem;

import Exceptions.IrError;

public class IfUnit extends LabelUnit {
    public IfUnit() {
        super();
    }

    public void setLabel(Integer index, String label) {
        super.setLabel(index, label);
    }

    public LabelValue getLabel(Integer index) throws IrError {
        return super.getLabel(index);
    }

    public Integer getRef(Integer index) {
        return super.getRef(index);
    }
}
