package Ir.LLVMIR.LabelSystem;

import Exceptions.IrError;

import java.util.HashMap;

public class LabelUnit {
    private HashMap<Integer, LabelValue> labels;

    public LabelUnit() {
        this.labels = new HashMap<>(3);
        // 初始化后就不能在改变了，br需要引用该label
        this.labels.put(0, new LabelValue());
        this.labels.put(1, new LabelValue());
        this.labels.put(2, new LabelValue());
    }

    public LabelUnit(LabelValue value) {
        this.labels = new HashMap<>(3);
        this.labels.put(0, value);
    }

    public void setLabel(Integer index, String label) {
        assert index >= 0 && index <= 2;
        this.labels.get(index).setLabelValue(label);
    }

    public void setLabel(String label) {
        this.labels.get(0).setLabelValue(label);
    }

    public LabelValue getLabel() throws IrError {
        if(!this.labels.containsKey(0)) {
            throw new IrError("if label doesn't have index: "+0);
        }
        this.labels.get(0).addRef();
        return this.labels.get(0);
    }

    public LabelValue getLabel(Integer index) throws IrError {
        if(!this.labels.containsKey(index)) {
            throw new IrError("if label doesn't have index: "+index);
        }
        this.labels.get(index).addRef();
        return this.labels.get(index);
    }

    public Integer getRef(Integer index) {
        return this.labels.get(index).getRefs();
    }
}
