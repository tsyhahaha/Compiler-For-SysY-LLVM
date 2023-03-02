package Ir.LLVMIR.IRInstr;

public class LoadIR extends Instr implements ArithIR {
    private String result;
    private String place;
    private int level;

    public LoadIR(String result, String place, int level) {
        super(new LoadIR());
        this.level = level;
        this.result = result;
        this.place = place;
    }

    public String l2s(int level) {
        if(level == 0) {
            return "i32";
        } else if(level == 1) {
            return "i32*";
        } else {
            return "[" + (level - 1) + " x i32]*";
        }
    }

    public LoadIR() {
        super();
    }

    @Override
    public String genIr() {
        String type = l2s(level);
        return result + " = load " + type + ", " + type + "* " + place;
    }
}
