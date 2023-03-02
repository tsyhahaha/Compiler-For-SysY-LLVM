package FrontEnd;

import Exceptions.ExceptionLog;
import Exceptions.ParserError;

import java.util.ArrayList;
import java.util.List;

import static Exceptions.ErrorType.*;
import static FrontEnd.TokenType.*;

public class Parser {
    private LexerController lexer;
    private Token nowToken;
    private int line;
    private int lastLine;

    public Parser(LexerController lexer) {
        this.lexer = lexer;
        this.nowToken = null;
    }

    private Parser(LexerController lexer, Token nowToken) {
        this.lexer = lexer;
        this.nowToken = nowToken;
    }

    private void getSymbol() {
        this.lastLine = line;
        this.nowToken = lexer.getToken();
        if (nowToken != null) {
            this.line = nowToken.getLine();
//            System.err.println("token: " + nowToken + " update line: "+line);
        }
    }

    public ParserNode run() throws ParserError {
        getSymbol();
        return HandleCompUnit();
    }

    private void rollBack() {
        this.lexer.rollBack();
        this.nowToken = lexer.getToken();
        this.line = nowToken.getLine();
    }

    private ParserNode getLeaf(TokenType type) throws ParserError {
        Token token = this.nowToken.myClone();
        if (token.getType() != type) {
            System.err.println("hope: " + type + ", get: " + token.getType());
            rollBack();
            throw new ParserError(type);    // 检测到非期望type的token
        } else {
            getSymbol();
            return new Token(token.getValue(), token.getType(), token.getLine());
        }
    }

    private ParserNode HandleCompUnit() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        /*
        检测Decl
         */
        while (lexer.getFollow(2) != null && (
                nowToken.getType() == CONSTTK ||
                        nowToken.getType() == INTTK &&
                                lexer.getFollow(1).getType() != MAINTK &&
                                lexer.getFollow(2).getType() != LPARENT
        )) {
            nodes.add(HandleDecl());
        }
        /*
        检测自定义函数
         */
        while (lexer.getFollow(2) != null && (
                nowToken.getType() == VOIDTK ||
                        nowToken.getType() == INTTK &&
                                lexer.getFollow(1).getType() != MAINTK &&
                                lexer.getFollow(2).getType() == LPARENT
        )) {
            nodes.add(HandleFuncDef());
        }
        nodes.add(HandleMainFuncDef());
        return new ParserNode(false, "CompUnit", nodes);
    }

    private ParserNode HandleDecl() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken.getType() == CONSTTK) {
            nodes.add(HandleConstDecl());
        } else {
            nodes.add(HandleVarDecl());
        }
        return new ParserNode(false, "Decl", nodes);
    }

    private ParserNode HandleConstDecl() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(CONSTTK));
        nodes.add(HandleBType());
        nodes.add(HandleConstDef());
        while (nowToken != null && nowToken.getType() == COMMA) {
            nodes.add(getLeaf(COMMA));
            nodes.add(HandleConstDef());
        }
        try {
            nodes.add(getLeaf(SEMICN));
        } catch (ParserError e) {
            // 处理缺失分号错误
            nodes.add(logError(SEMICN));
        }
        return new ParserNode(false, "ConstDecl", nodes);
    }

    private ParserNode HandleVarDecl() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleBType());
        nodes.add(HandleVarDef());
        while (nowToken != null && nowToken.getType() == COMMA) {
            nodes.add(getLeaf(COMMA));
            nodes.add(HandleVarDef());
        }
        try {
            nodes.add(getLeaf(SEMICN));
        } catch (ParserError e) {
            // 处理缺少分号错误
            nodes.add(logError(SEMICN));
        }
        return new ParserNode(false, "VarDecl", nodes);
    }

    private ParserNode HandleBType() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(INTTK));
        return new ParserNode(false, "BType", nodes);
    }

    private ParserNode HandleConstDef() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(IDENFR));
        /*
        左右中括号需闭合
         */
        while (nowToken != null && nowToken.getType() == LBRACK) {
            nodes.add(getLeaf(LBRACK));
            nodes.add(HandleConstExp());
            try {
                nodes.add(getLeaf(RBRACK));
            } catch (ParserError e) {
                // 中括号右侧不闭合
                nodes.add(logError(RBRACK));
            }
        }
        nodes.add(getLeaf(ASSIGN)); // const定义必有初值
        nodes.add(HandleConstInitVal());
        return new ParserNode(false, "ConstDef", nodes);
    }

    private ParserNode HandleConstInitVal() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null && nowToken.getType() == LBRACE) {
            nodes.add(getLeaf(LBRACE));
            if (nowToken.getType() != RBRACE) {
                nodes.add(HandleConstInitVal());
                while (nowToken != null && nowToken.getType() == COMMA) {
                    nodes.add(getLeaf(COMMA));
                    nodes.add(HandleConstInitVal());
                }
            }
            nodes.add(getLeaf(RBRACE));
        } else {
            nodes.add(HandleConstExp());    // 非数组初始化
        }
        return new ParserNode(false, "ConstInitVal", nodes);
    }

    private ParserNode HandleVarDef() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(IDENFR));
        /*
        左右中括号需闭合
         */
        System.err.println(lexer.getFollow(1));
        if (lexer.getFollow(1).getType() == GETINTTK) {
            nodes.add(getLeaf(ASSIGN));
            nodes.add(getLeaf(GETINTTK));
            nodes.add(getLeaf(LPARENT));
            nodes.add(getLeaf(RPARENT));
            return new ParserNode(false, "VarDef", nodes);
        }
        while (nowToken != null && nowToken.getType() == LBRACK) {
            nodes.add(getLeaf(LBRACK));
            nodes.add(HandleConstExp());
            try {
                nodes.add(getLeaf(RBRACK));
            } catch (ParserError e1) {
                // 中括号右侧不闭合
                nodes.add(logError(RBRACK));
            }
        }
        if (nowToken != null && nowToken.getType() == ASSIGN) {
            nodes.add(getLeaf(ASSIGN)); // const定义必有初值
            nodes.add(HandleInitVal());
        }
        return new ParserNode(false, "VarDef", nodes);
    }

    private ParserNode HandleInitVal() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null && nowToken.getType() == LBRACE) {
            nodes.add(getLeaf(LBRACE));
            if (nowToken.getType() != RBRACE) {
                nodes.add(HandleInitVal());
                while (nowToken != null && nowToken.getType() == COMMA) {
                    nodes.add(getLeaf(COMMA));
                    nodes.add(HandleInitVal());
                }
            }
            nodes.add(getLeaf(RBRACE));
        } else {
            nodes.add(HandleExp());
        }
        return new ParserNode(false, "InitVal", nodes);
    }

    private ParserNode HandleFuncDef() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleFuncType());
        nodes.add(getLeaf(IDENFR));
        nodes.add(getLeaf(LPARENT));
        if (nowToken != null && nowToken.getType() == INTTK) {
            nodes.add(HandleFuncFParams());
        }
        try {
            nodes.add(getLeaf(RPARENT));
        } catch (ParserError e) {
            // 右括号未闭合
            nodes.add(logError(RPARENT));
        }
        nodes.add(HandleBlock());
        return new ParserNode(false, "FuncDef", nodes);
    }

    private ParserNode HandleFuncType() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null && nowToken.getType() == VOIDTK) {
            nodes.add(getLeaf(VOIDTK));
        } else {
            nodes.add(getLeaf(INTTK));
        }
        return new ParserNode(false, "FuncType", nodes);
    }

    private ParserNode HandleFuncFParams() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleFuncFParam());
        while (nowToken != null && nowToken.getType() == COMMA) {
            nodes.add(getLeaf(COMMA));
            nodes.add(HandleFuncFParam());
        }
        return new ParserNode(false, "FuncFParams", nodes);
    }

    private ParserNode HandleFuncFParam() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleBType());
        nodes.add(getLeaf(IDENFR));
        /*
        参数为数组，则需左右括号闭合
         */
        if (nowToken != null && nowToken.getType() == LBRACK) {
            nodes.add(getLeaf(LBRACK));
            try {
                nodes.add(getLeaf(RBRACK));
            } catch (ParserError e) {
                // 处理中括号右侧不闭合
                nodes.add(logError(RBRACK));
            }
            while (nowToken != null && nowToken.getType() == LBRACK) {
                nodes.add(getLeaf(LBRACK));
                nodes.add(HandleConstExp());
                try {
                    nodes.add(getLeaf(RBRACK));
                } catch (ParserError e) {
                    // 处理中括号右侧不闭合
                    nodes.add(logError(RBRACK));
                }
            }
        }
        return new ParserNode(false, "FuncFParam", nodes);
    }

    private ParserNode HandleBlock() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(LBRACE));
        while (nowToken != null && nowToken.getType() != RBRACE) {
            nodes.add(HandleBlockItem());
        }
        nodes.add(getLeaf(RBRACE));
        return new ParserNode(false, "Block", nodes);
    }

    private ParserNode HandleBlockItem() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null && (nowToken.getType() == CONSTTK || nowToken.getType() == INTTK)) {
            nodes.add(HandleDecl());
        } else {
            nodes.add(HandleStmt());
        }
        return new ParserNode(false, "BlockItem", nodes);
    }

    private ParserNode HandleStmt() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken == null) {
            throw new ParserError();
        }
        if (nowToken.getType() == LBRACE) {
            nodes.add(HandleBlock());
        } else if (nowToken.getType() == IFTK) {
            nodes.add(getLeaf(IFTK));
            nodes.add(getLeaf(LPARENT));
            nodes.add(HandleCond());
            try {
                nodes.add(getLeaf(RPARENT));
            } catch (ParserError e) {
                // 右小括号未闭合错误处理
                nodes.add(logError(RPARENT));
            }
            nodes.add(HandleStmt());
            if (nowToken != null && nowToken.getType() == ELSETK) {
                nodes.add(getLeaf(ELSETK));
                nodes.add(HandleStmt());
            }
        } else if (nowToken.getType() == WHILETK) {
            nodes.add(getLeaf(WHILETK));
            nodes.add(getLeaf(LPARENT));
            nodes.add(HandleCond());
            try {
                nodes.add(getLeaf(RPARENT));
            } catch (ParserError e) {
                // 处理圆括号右侧不闭合
                nodes.add(logError(RPARENT));
            }
            nodes.add(HandleStmt());
        } else if (nowToken.getType() == BREAKTK) {
            nodes.add(getLeaf(BREAKTK));
            try {
                nodes.add(getLeaf(SEMICN));
            } catch (ParserError e) {
                // 缺少分号错误处理
                nodes.add(logError(SEMICN));
            }
        } else if (nowToken.getType() == CONTINUETK) {
            nodes.add(getLeaf(CONTINUETK));
            try {
                nodes.add(getLeaf(SEMICN));
            } catch (ParserError e) {
                // 缺少分号错误处理
                nodes.add(logError(SEMICN));
            }
        } else if (nowToken.getType() == RETURNTK) {
            nodes.add(getLeaf(RETURNTK));
            if (nowToken != null && nowToken.getType() != SEMICN) {
                try {
                    Parser parser = new Parser(lexer.myClone(), nowToken.myClone());
                    ParserNode temp = parser.HandleExp();
                    nodes.add(HandleExp());
                } catch (ParserError ignore) {
                    // 未检测到Exp则忽略，回溯
                }
            }
            try {
                nodes.add(getLeaf(SEMICN));
            } catch (ParserError e) {
                // 缺少分号错误处理
                nodes.add(logError(SEMICN));
            }
        } else if (nowToken.getType() == PRINTFTK) {
            nodes.add(getLeaf(PRINTFTK));
            nodes.add(getLeaf(LPARENT));
            nodes.add(getLeaf(STRCON));
            while (nowToken != null && nowToken.getType() == COMMA) {
                nodes.add(getLeaf(COMMA));
                nodes.add(HandleExp());
            }
            try {
                nodes.add(getLeaf(RPARENT));
            } catch (ParserError e) {
                // 处理圆括号右侧未闭合错误
                nodes.add(logError(RPARENT));
            }
            try {
                nodes.add(getLeaf(SEMICN));
            } catch (ParserError e) {
                // 处理分号缺失错误
                nodes.add(logError(SEMICN));
            }
        } else {
            try {
                Parser parser = new Parser(lexer.myClone(), nowToken.myClone());
                parser.HandleLVal();
                parser.getLeaf(ASSIGN);
                nodes.add(HandleLVal());
                nodes.add(getLeaf(ASSIGN));
            /*
            等号右侧可能是getint()，也可能是Exp
             */
                // 逻辑有问题，大大的问题！！！
                if (nowToken != null && nowToken.getType() == GETINTTK) {
                    nodes.add(getLeaf(GETINTTK));
                    nodes.add(getLeaf(LPARENT));
                    try {
                        nodes.add(getLeaf(RPARENT));
                    } catch (ParserError e) {
                        // 右侧圆括号错误处理
                        nodes.add(logError(RPARENT));
                    }
                } else {
                    nodes.add(HandleExp());
                }
            } catch (ParserError e) {
                /*
                处理无LVal的情况
                 */
                if (nowToken != null && nowToken.getType() != SEMICN) {
                    nodes.add(HandleExp());
                }
            }
            try {
                nodes.add(getLeaf(SEMICN));
            } catch (ParserError e) {
                // 处理缺少分号错误
                nodes.add(logError(SEMICN));
            }
        }
        return new ParserNode(false, "Stmt", nodes);
    }

    private ParserNode HandleCond() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleLOrExp());
        return new ParserNode(false, "Cond", nodes);
    }

    private ParserNode HandleLVal() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(IDENFR));
        while (nowToken != null && nowToken.getType() == LBRACK) {
            nodes.add(getLeaf(LBRACK));
            nodes.add(HandleExp());
            try {
                nodes.add(getLeaf(RBRACK));
            } catch (ParserError e) {
                // 处理中括号未闭合错误
                nodes.add(logError(RBRACK));
            }
        }
        return new ParserNode(false, "LVal", nodes);
    }

    private ParserNode HandlePrimaryExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null && nowToken.getType() == LPARENT) {
            nodes.add(getLeaf(LPARENT));
            nodes.add(HandleExp());
            try {
                nodes.add(getLeaf(RPARENT));
            } catch (ParserError e) {
                // 圆括号未闭合错误处理
                nodes.add(logError(RPARENT));
            }
        } else if (nowToken != null && nowToken.getType() == INTCON) {
            nodes.add(HandleNumber());
        } else {
            nodes.add(HandleLVal());
        }
        return new ParserNode(false, "PrimaryExp", nodes);
    }

    private ParserNode HandleNumber() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(INTCON));
        return new ParserNode(false, "Number", nodes);
    }

    private ParserNode HandleUnaryExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null && nowToken.getType() == IDENFR &&
                lexer.getFollow(1).getType() == LPARENT) {
            nodes.add(getLeaf(IDENFR));
            nodes.add(getLeaf(LPARENT));
            try {
                Parser parser = new Parser(this.lexer.myClone(), this.nowToken.myClone());
                parser.HandleExp();
                nodes.add(HandleFuncRParams());
            } catch (ParserError e) {
            }
            try {
                nodes.add(getLeaf(RPARENT));
            } catch (ParserError e) {
                // 处理圆括号未闭合错误
                nodes.add(logError(RPARENT));
            }
        } else if (nowToken != null && (nowToken.getType() == PLUS ||
                nowToken.getType() == MINU || nowToken.getType() == NOT)) {
            nodes.add(HandleUnaryOp());
            nodes.add(HandleUnaryExp());
        } else {
            nodes.add(HandlePrimaryExp());
        }
        return new ParserNode(false, "UnaryExp", nodes);
    }

    private ParserNode HandleUnaryOp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        if (nowToken != null) {
            switch (nowToken.getType()) {
                case PLUS:
                case MINU:
                case NOT:
                    nodes.add(getLeaf(nowToken.getType()));
                    break;
                default:
                    throw new ParserError();
            }
        }
        return new ParserNode(false, "UnaryOp", nodes);
    }

    private ParserNode HandleFuncRParams() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleExp());
        while (nowToken != null && nowToken.getType() == COMMA) {
            nodes.add(getLeaf(COMMA));
            nodes.add(HandleExp());
        }
        return new ParserNode(false, "FuncRParams", nodes);
    }

    private ParserNode HandleMulExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleUnaryExp());
        while (nowToken != null && (nowToken.getType() == MULT ||
                nowToken.getType() == MOD || nowToken.getType() == DIV ||
                nowToken.getType() == BITANDTK)) {
            ParserNode mulNode = new ParserNode(false, "MulExp", new ArrayList<>(nodes));
            nodes.clear();
            nodes.add(mulNode);
            nodes.add(getLeaf(nowToken.getType()));
            nodes.add(HandleUnaryExp());
        }
        return new ParserNode(false, "MulExp", nodes);
    }

    private ParserNode HandleAddExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleMulExp());
        while (nowToken != null && (nowToken.getType() == PLUS ||
                nowToken.getType() == MINU)) {
            ParserNode addNode = new ParserNode(false, "AddExp", new ArrayList<>(nodes));
            nodes.clear();
            nodes.add(addNode);
            nodes.add(getLeaf(nowToken.getType()));
            nodes.add(HandleMulExp());
        }
        return new ParserNode(false, "AddExp", nodes);
    }

    private ParserNode HandleRelExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleAddExp());
        while (nowToken != null && (nowToken.getType() == GRE ||
                nowToken.getType() == LSS || nowToken.getType() == GEQ ||
                nowToken.getType() == LEQ)) {
            ParserNode RelNode = new ParserNode(false, "RelExp", new ArrayList<>(nodes));
            nodes.clear();
            nodes.add(RelNode);
            nodes.add(getLeaf(nowToken.getType()));
            nodes.add(HandleAddExp());
        }
        return new ParserNode(false, "RelExp", nodes);
    }

    private ParserNode HandleEqExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleRelExp());
        while (nowToken != null && (nowToken.getType() == EQL ||
                nowToken.getType() == NEQ)) {
            ParserNode eqNode = new ParserNode(false, "EqExp", new ArrayList<>(nodes));
            nodes.clear();
            nodes.add(eqNode);
            nodes.add(getLeaf(nowToken.getType()));
            nodes.add(HandleRelExp());
        }
        return new ParserNode(false, "EqExp", nodes);
    }

    private ParserNode HandleLAndExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleEqExp());
        while (nowToken != null && nowToken.getType() == AND) {
            ParserNode lAndNode = new ParserNode(false, "LAndExp", new ArrayList<>(nodes));
            nodes.clear();
            nodes.add(lAndNode);
            nodes.add(getLeaf(AND));
            nodes.add(HandleEqExp());
        }
        return new ParserNode(false, "LAndExp", nodes);
    }

    private ParserNode HandleLOrExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleLAndExp());
        while (nowToken != null && nowToken.getType() == OR) {
            ParserNode lOrNode = new ParserNode(false, "LOrExp", new ArrayList<>(nodes));
            nodes.clear();
            nodes.add(lOrNode);
            nodes.add(getLeaf(OR));
            nodes.add(HandleLAndExp());
        }
        return new ParserNode(false, "LOrExp", nodes);
    }

    private ParserNode HandleMainFuncDef() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(getLeaf(INTTK));
        nodes.add(getLeaf(MAINTK));
        nodes.add(getLeaf(LPARENT));
        try {
            nodes.add(getLeaf(RPARENT));
        } catch (ParserError e) {
            // 圆括号未闭合错误处理
            nodes.add(logError(RPARENT));
        }
        nodes.add(HandleBlock());
        return new ParserNode(false, "MainFuncDef", nodes);
    }

    private ParserNode HandleConstExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleAddExp());
        return new ParserNode(false, "ConstExp", nodes);
    }

    private ParserNode HandleExp() throws ParserError {
        List<ParserNode> nodes = new ArrayList<>();
        nodes.add(HandleAddExp());
        return new ParserNode(false, "Exp", nodes);
    }

    private Token logError(TokenType type) {
        switch (type) {
            case RPARENT:
                ExceptionLog.addException(new ParserError(j, lastLine));
                return new Token(")", RPARENT, line);
            case RBRACK:
                ExceptionLog.addException(new ParserError(k, lastLine));
                return new Token("]", RBRACK, line);
            case SEMICN:
                ExceptionLog.addException(new ParserError(i, lastLine));
                return new Token(";", SEMICN, line);
            default:
                return null;
        }
    }
}
