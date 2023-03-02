package FrontEnd;

import java.util.ArrayList;
import java.util.List;

public class LexerController {
    private Lexer lexer;
    private List<Token> tokenList;
    private int index;

    public LexerController(Lexer lexer) {
        this.lexer = lexer;
        this.index = 0;
        this.tokenList = new ArrayList<>();
        Token bottom = lexer.getSymbol();
        if(bottom == null) {
            System.err.println("There is no Token!");
        } else {
            this.tokenList.add(bottom);
        }
    }

    private LexerController(Lexer lexer, List<Token> tokenList, int index) {
        this.lexer = lexer;
        this.tokenList = tokenList;
        this.index = index;
    }

    public void rollBack() {
        if(index >= 1) {
            index -= 1;
        }
    }

    public Token getToken() {
        if(tokenList.size() < index + 1) {
            tokenList.add(lexer.getSymbol());
        }
        return tokenList.get(index++);
    }

    public Token getFollow(int n) {
        if (index - 1 + n  >= this.tokenList.size()) {
            for (int i = 0; i < n; i++) {
                Token pointer = lexer.getSymbol();
                if (pointer != null) {
                    this.tokenList.add(pointer);
                } else {
                    return null;
                }
            }
        }
        return this.tokenList.get(index - 1 + n);
    }

    public LexerController myClone() {
        Lexer temp = this.lexer.myClone();
        List<Token> tempList = new ArrayList<>(this.tokenList);
        return new LexerController(temp, tempList, index);
    }

    public Token getAhead(int n) {
        return index - n >= 0 ? this.tokenList.get(index - n) : null;
    }
}
