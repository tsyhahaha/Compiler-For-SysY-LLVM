package FrontEnd;

import java.util.ArrayList;

public class Token extends ParserNode {
    private TokenType type;
    private String value;
    private int intValue;
    private int line;

    public Token(String value, TokenType type, int line) {
        super(true,value, new ArrayList<>());
        this.type = type;
        this.value = value;
        this.line = line;
        if (type == TokenType.INTCON) {
            this.intValue = Integer.parseInt(value);
        }
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }   // 如果类型是int，这个属性会保留初值

    public int getLine() {
        return line;
    }

    public Token myClone() {
        return new Token(value, type, line);
    }

    @Override
    public String toString() {
        return type + " " + value;
    }
}
