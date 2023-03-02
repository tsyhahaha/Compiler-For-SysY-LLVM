package FrontEnd;

import java.util.List;

public class ParserNode {
    private boolean isLeaf;
    private String name;
    private List<ParserNode> values;

    public ParserNode(boolean isLeaf, String name, List<ParserNode> values) {
        this.isLeaf = isLeaf;
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return this.name;
    }

    public List<ParserNode> getValues() {
        return values;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public String toIr() {
        return values.stream()
                .map(ParserNode::toIr)
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("");
    }

    public String toString() {
        return values.stream()
                .map(ParserNode::toString)
                .reduce((s1, s2) -> s1 + "\n" + s2).orElse("") +
                (!name.equals("Decl") && !name.equals("BlockItem") && !name.equals("BType") ? "\n<" + name + ">" : "");
    }
}
