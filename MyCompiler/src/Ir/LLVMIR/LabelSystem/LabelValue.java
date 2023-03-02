package Ir.LLVMIR.LabelSystem;

public class LabelValue {
    private String labelValue;
    private int refs;

    public LabelValue() {
        labelValue = null;
        refs = 0;
    }

    public LabelValue(String labelValue) {
        this.labelValue = labelValue;
        refs = 0;
    }

    public void addRef() {
        refs++;
    }

    public void setLabelValue(String labelValue) {
        this.labelValue = labelValue;
    }

    public int getRefs() {
        return this.refs;
    }

    public String toString() {
        return labelValue;
    }
}
