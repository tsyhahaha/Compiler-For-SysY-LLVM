package FrontEnd;

import Exceptions.ErrorType;
import Exceptions.ExceptionLog;
import Exceptions.LexerError;
import utils.ReadFile;

import java.util.HashMap;

public class Lexer {
    private final String input;
    private int index;
    private int line;
    private char c;
    private HashMap<String, TokenType> reservedWord;

    public Lexer() {
        index = 0;
        line = 1;
        reservedWord = new HashMap<>();
        reservedInit();
        this.input = ReadFile.readToString("testfile.txt");
        if (input != null) {
            this.c = input.charAt(index);
        } else {
            c = 0;
        }
    }

    private Lexer(String input, int index, int line, char c, HashMap<String, TokenType> reservedWord) {
        this.input = input;
        this.index = index;
        this.line = line;
        this.c = c;
        this.reservedWord = reservedWord;
    }

    private void reservedInit() {
        reservedWord.put("main", TokenType.MAINTK);
        reservedWord.put("const", TokenType.CONSTTK);
        reservedWord.put("int", TokenType.INTTK);
        reservedWord.put("break", TokenType.BREAKTK);
        reservedWord.put("continue", TokenType.CONTINUETK);
        reservedWord.put("if", TokenType.IFTK);
        reservedWord.put("else", TokenType.ELSETK);
        reservedWord.put("while", TokenType.WHILETK);
        reservedWord.put("getint", TokenType.GETINTTK);
        reservedWord.put("printf", TokenType.PRINTFTK);
        reservedWord.put("return", TokenType.RETURNTK);
        reservedWord.put("void", TokenType.VOIDTK);
        reservedWord.put("bitand", TokenType.BITANDTK);
    }

    private void getChar() {
        index++;
        if (index >= input.length()) {
            c = 0;
            index = 0;
            return;
        }
        c = input.charAt(index);
    }

    /*
    每次都要保证，处理当前token信息时，pointer指向该token的下一个字符
    对getChar函数，需要用if语句包含，防止访问越界
     */
    public Token getSymbol() {
        while (isSpace()) {
            getChar();
        }
        if (isLetter() || c == '_') {
            return handleLetter();
        } else if (isDigit()) {
            return handleDigit();
        } else if (c == '"') {
            return handleQuo();
        } else if (c == '!') {
            getChar();
            if (c == '=') {
                getChar();
                return new Token("!=", TokenType.NEQ, line);
            } else {
                return new Token("!", TokenType.NOT, line);
            }
        } else if (c == '&') {
            getChar();
            if (c == '&') {
                getChar();
                return new Token("&&", TokenType.AND, line);
            } else {
                System.err.println("err: & just one!");
            }
        } else if (this.c == '|') {
            getChar();
            if (c == '|') {
                getChar();
                return new Token("||", TokenType.OR, line);
            } else {
                System.err.println("err: | just one!");
            }
        } else if (this.c == '+') {
            getChar();
            return new Token("+", TokenType.PLUS, line);
        } else if (this.c == '-') {
            getChar();
            return new Token("-", TokenType.MINU, line);
        } else if (this.c == '*') {
            getChar();
            return new Token("*", TokenType.MULT, line);
        } else if (this.c == '/') {
            getChar();
            if (c == '/') {
                while (c != '\n') {
                    getChar();
                }
                return getSymbol();
            } else if (c == '*') {      // 发现了/*的结构
                getChar();
                while (c == '*') {
                    getChar();
                    if (c == '/') {
                        getChar();
                        return getSymbol();
                    }
                }
                while (true) {
                    getChar();
                    while (c != '*') {  // 处理所有不是*的字符，直到遇到*，检测是否到结尾
                        getChar();
                    }
                    while (c == '*') {   // 可能有多个*，先处理掉
                        getChar();
                        if (c == '/') {  // 可能直接结尾
                            getChar();
                            return getSymbol();    // 注释处理完毕，重新运行getSymbol
                        }
                    }   // 也可能是骗人的****，之后遇到非*字符，继续处理注释
                }
            } else {
                return new Token("/", TokenType.DIV, line);
            }
            // 注意注释的处理
        } else if (this.c == '%') {
            getChar();
            return new Token("%", TokenType.MOD, line);
        } else if (this.c == '<') {
            getChar();
            if (c == '=') {
                getChar();
                return new Token("<=", TokenType.LEQ, line);
            } else {
                return new Token("<", TokenType.LSS, line);
            }
        } else if (this.c == '>') {
            getChar();
            if (c == '=') {
                getChar();
                return new Token(">=", TokenType.GEQ, line);
            } else {
                return new Token(">", TokenType.GRE, line);
            }
        } else if (this.c == '=') {
            getChar();
            if (c == '=') {
                getChar();
                return new Token("==", TokenType.EQL, line);
            } else {
                return new Token("=", TokenType.ASSIGN, line);
            }
        } else if (this.c == ';') {
            getChar();
            return new Token(";", TokenType.SEMICN, line);
        } else if (this.c == ',') {
            getChar();
            return new Token(",", TokenType.COMMA, line);
        } else if (this.c == '(') {
            getChar();
            return new Token("(", TokenType.LPARENT, line);
        } else if (this.c == ')') {
            getChar();
            return new Token(")", TokenType.RPARENT, line);
        } else if (this.c == '[') {
            getChar();
            return new Token("[", TokenType.LBRACK, line);
        } else if (this.c == ']') {
            getChar();
            return new Token("]", TokenType.RBRACK, line);
        } else if (this.c == '{') {
            getChar();
            return new Token("{", TokenType.LBRACE, line);
        } else if (this.c == '}') {
            getChar();
            return new Token("}", TokenType.RBRACE, line);
        } else if (this.c == 0) {
            return null;
        } else {
            System.err.println("FrontEnd.Lexer can't understand " + this.c);
        }
        return null;
    }

    private Token handleLetter() {
        StringBuilder indent = new StringBuilder(String.valueOf(this.c));
        getChar();
        while (isNonDigit() || isDigit()) {
            indent.append(this.c);
            getChar();
        }
        String value = indent.toString();
        TokenType query = reservedQuery(value);
        if (query == null) {
            return new Token(value, TokenType.IDENFR, line);
        } else {
            return new Token(value, query, line);
        }
    }

    private Token handleDigit() {
        StringBuilder digit = new StringBuilder(String.valueOf(this.c));
        getChar();
        while (isDigit()) {
            digit.append(this.c);
            getChar();
        }
        String value = digit.toString();
        return new Token(value, TokenType.INTCON, line);
    }

    private Token handleQuo() {
        StringBuilder format = new StringBuilder(String.valueOf(this.c));
        getChar();
        boolean isFormat = false;
        boolean isConv = false;
        boolean exception = false;
        while (!(this.c == '"')) {      // 读到双引号位置，但是可能存在非法符号
            if(c == '%') {
                if(!isFormat) {
                    isFormat = true;
                } else {        // %%
                    exception = true;
                }
            } else if(c == '\\') {
                if(!isConv) {
                    isConv = true;
                } else {        // \\
                    exception = true;
                }
            } else if(!(c == 32 || c == 33 || (c >= 40 && c <= 126))) {
                exception = true;
            } else {
                if((isFormat && c != 'd') || (isConv && c != 'n')) {
                    exception = true;
                } else {
                    isFormat = false;
                    isConv = false;
                }
            }
            format.append(c);
            getChar();
        }
        if(exception || isFormat || isConv) {
            ExceptionLog.addException(new LexerError(ErrorType.a, line));
        }
        format.append('"');
        getChar();
        String value = format.toString();
        return new Token(value, TokenType.STRCON, line);
    }

    private TokenType reservedQuery(String value) {
        return reservedWord.getOrDefault(value, null);
    }

    private boolean isDigit() {
        return c >= '0' && c <= '9';
    }

    private boolean isSpace() {
        if (c == '\n') {
            line++;
        }
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private boolean isNonDigit() {
        return isLetter() || this.c == '_';
    }

    private boolean isLetter() {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    public Lexer myClone() {
        return new Lexer(input, index, line, c, reservedWord);
    }

}
