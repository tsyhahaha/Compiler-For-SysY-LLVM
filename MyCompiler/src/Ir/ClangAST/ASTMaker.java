package Ir.ClangAST;

import static Exceptions.ErrorType.*;

import Exceptions.ExceptionLog;
import Exceptions.IrError;
import Exceptions.ParserError;
import FrontEnd.TokenType;
import Ir.ClangAST.AST.*;
import Ir.ClangAST.SymbolTable.*;
import FrontEnd.ParserNode;
import FrontEnd.Token;
import utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class ASTMaker {
    private static ASTMaker instance;
    private static SymbolTable root = null;
    private ParserNode compUnit;
    private SymbolTable nowTable;
    private String nowFuncName;
    public int inFlow;


    //获取唯一可用的对象
    public static ASTMaker getInstance(ParserNode root) throws ParserError, IrError {
        if (instance == null) {
            instance = new ASTMaker(root);
        }
        return instance;
    }

    private ASTMaker(ParserNode compUnit) throws ParserError, IrError {
        this.nowTable = new SymbolTable(true);
        this.inFlow = 0;
        if (root != null) {
            throw new IrError("root initial twice!");
        } else {
            root = nowTable;
        }
        root.put("main", new FuncDecl("main", new ArrayList<>(), CType.INT));
        List<ParamPT> params = new ArrayList<>();
        params.add(new ParamPT("int", 0));
        root.put("getint", new FuncDecl("getint", new ArrayList<>(), CType.INT));
        root.put("putint", new FuncDecl("putint", params, CType.VOID));
        params = new ArrayList<>();
        params.add(new ParamPT("char", 0));
        root.put("putch", new FuncDecl("putch", params, CType.VOID));
        params = new ArrayList<>();
        params.add(new ParamPT("char", 1));
        root.put("putstr", new FuncDecl("putstr", params, CType.VOID));
        if (compUnit.getName().equals("CompUnit")) {
            this.compUnit = compUnit;
        } else {
            throw new ParserError("CompUnit");
        }
    }

    public static SymbolTable getRoot() {
        return root;
    }

    public Root HandleRoot() throws ParserError, IrError {
        List<VarDeclStmt> globalVars = new ArrayList<>();
        List<FuncDeclStmt> functions = new ArrayList<>();
        FuncDeclStmt main = null;
        List<ParserNode> nodes = compUnit.getValues();

        for (ParserNode node : nodes) {
            if (node.getName().equals("Decl")) {
                globalVars.addAll(HandleVarDeclStmt(node.getValues().get(0)));
            } else if (node.getName().equals("FuncDef")) {
                FuncDeclStmt stmt = HandleFuncDeclStmt(node);
                if (stmt != null) {
                    functions.add(stmt);
                }
            } else {
                main = HandleFuncDeclStmt(node);
            }
        }
        assert main != null;
        return new Root(globalVars, functions, main);
    }

    /*
     * 可能在一行定义多个变量，需要分开考虑
     */
    private List<VarDeclStmt> HandleVarDeclStmt(ParserNode node) throws IrError {
        List<VarDeclStmt> decls = new ArrayList<>();
        List<ParserNode> nodes = node.getValues();
        nodes.remove(0);    // const / BType
        if (node.getName().equals("ConstDecl")) {           // 如果是常量 const
            // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
            nodes.remove(0);    // remove BType
            int i = 0;
            while (i < nodes.size()) {
                ParserNode decl = nodes.get(i);
                if (decl.getName().equals("ConstDef")) {
                    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
                    List<ParserNode> constDefValue = decl.getValues();
                    String name = constDefValue.get(0).getName();
                    if (nowTable.containVar(name)) {
                        // 变量名字重定义
                        int line = ((Token) constDefValue.get(0)).getLine();
                        ExceptionLog.addException(new IrError(b, line));
                        i += 2;
                        continue;
                    }
                    decls.add(HandleConstDef(decl));
                }
                i += 2;
            }
        } else if (node.getName().equals("VarDecl")) {
            // VarDecl → BType VarDef { ',' VarDef } ';'
            int i = 0;
            while (i < nodes.size() - 1) {
                ParserNode decl = nodes.get(i);
                if (decl.getName().equals("VarDef")) {
                    // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
                    List<ParserNode> varDefValue = decl.getValues();
                    String name = varDefValue.get(0).getName();
                    if (nowTable.containVar(name)) {
                        // 变量名字重定义
                        int line = ((Token) varDefValue.get(0)).getLine();
                        ExceptionLog.addException(new IrError(b, line));
                        i += 2;
                        continue;
                    }
                    decls.add(HandleVarDef(decl));
                }
                i += 2;
            }
        }
        return decls;
    }

    private VarDeclStmt HandleConstDef(ParserNode node) throws IrError {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        List<ParserNode> constDefValue = node.getValues();
        String name = constDefValue.get(0).getName();
        if (constDefValue.size() < 1 || !(constDefValue.get(1) instanceof Token &&
                ((Token) constDefValue.get(1)).getValue().equals("["))) {
            // 没有中括号的情况
            ParserNode constInitAdd = constDefValue.get(2).
                    getValues().get(0).getValues().get(0);
            Expr constExp = HandleComputeStmt(constInitAdd);
            Integer constInit = (Integer) constExp.getValue();  // const 定义必有初值
            assert constInit != null;
            System.err.println("const int " + name + ": " + constInit);
            // 填表，标明是const
            VarDecl varDecl = new VarDecl(true, name, constInit, 0, null);
            this.nowTable.put(name, varDecl);
            // 生成decls表达式，翻译用
            return new VarDeclStmt(name, nowTable.isRoot(), true, constInit);
        } else {
            // 数组
            if (constDefValue.size() == 4) {     // 一维数组初始化
                // VarDef → Ident '[' ConstExp ']'
                List<Integer> arrayInitial = new ArrayList<>();
                ComputeStmt constExp = HandleComputeStmt(constDefValue.get(2));
                Integer d1 = constExp.getValue();
                assert d1 != null;
                if (nowTable.isRoot()) {
                    for (int i = 0; i < d1; i++) {
                        arrayInitial.set(i, 0);
                    }
                }
                System.err.println("const int " + name + ": zero initializer");
                VarDecl varDecl = new VarDecl(true, true, name, arrayInitial, 1, new Pair<>(d1, 0));
                this.nowTable.put(name, varDecl);
                ParserNode exp = constDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(exp);
                Integer constValue = constStmt.getValue();
                assert constValue != null;
                Pair<Integer, Integer> varDimension = new Pair<>(constValue, 0);
                return new ArrDeclStmt(name, nowTable.isRoot(), true, varDimension);
            } else if (constDefValue.size() == 6) {
                // VarDef → Ident '[' ConstExp ']' = InitVal
                ParserNode constExp = constDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue = constStmt.getValue();
                assert constValue != null;
                Pair<Integer, Integer> dimension = new Pair<>(constValue, 0);
                ParserNode initVal = constDefValue.get(5);
                List<ComputeStmt> initList = HandleInitVal(initVal);
                List<Integer> constInitList = new ArrayList<>();
                for (ComputeStmt init : initList) {
                    constInitList.add(init.getValue()); // 应该是都能求出值的
                }
                System.err.println("const array initial: " + name + "=" + constInitList);
                VarDecl varDecl = new VarDecl(true, true, name, constInitList, 1, dimension);
                this.nowTable.put(name, varDecl);
                return new ArrDeclStmt(name, nowTable.isRoot(), true, dimension, initList);
            } else if (constDefValue.size() == 7) {
                // VarDef → Ident '[' ConstExp ']' '[' ConstExp ']'
                ParserNode constExp = constDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue1 = constStmt.getValue();
                assert constValue1 != null;
                constExp = constDefValue.get(5);
                constStmt = HandleComputeStmt(constExp);
                Integer constValue2 = constStmt.getValue();
                assert constValue2 != null;
                Pair<Integer, Integer> dimension = new Pair<>(constValue1, constValue2);
                Expr d2 = HandleComputeStmt(constDefValue.get(5));
                Integer d2Value = (Integer) d2.getValue();
                assert d2Value != null;
                VarDecl varDecl = new VarDecl(true, name, null, 1 + d2Value, dimension);
                this.nowTable.put(name, varDecl);
                return new ArrDeclStmt(name, nowTable.isRoot(), true, dimension);
            } else {    // 二维数组初始化
                // VarDef → Ident '[' ConstExp ']' '[' ConstExp ']' = initVal
                ParserNode constExp = constDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue1 = constStmt.getValue();
                assert constValue1 != null;
                constExp = constDefValue.get(5);
                constStmt = HandleComputeStmt(constExp);
                Integer constValue2 = constStmt.getValue();
                assert constValue2 != null;
                ParserNode initVal = constDefValue.get(8);
                List<ComputeStmt> initList = HandleInitVal(initVal);
                List<Integer> constInitList = new ArrayList<>();
                for (ComputeStmt init : initList) {
                    constInitList.add(init.getValue());     // 每个value应该都能计算
                }
                System.err.println("const int " + name + ": " + constInitList);
                Pair<Integer, Integer> dimension = new Pair<>(constValue1, constValue2);
                Expr d2 = HandleComputeStmt(constDefValue.get(5));
                Integer d2Value = (Integer) d2.getValue();
                assert d2Value != null;
                VarDecl varDecl = new VarDecl(true, true, name, constInitList, 1 + d2Value, dimension);
                this.nowTable.put(name, varDecl);
                return new ArrDeclStmt(name, nowTable.isRoot(), true, dimension, initList);
            }
        }
    }

    private VarDeclStmt HandleVarDef(ParserNode node) throws IrError {
        List<ParserNode> varDefValue = node.getValues();
        String name = varDefValue.get(0).getName();
        if (varDefValue.size() > 2 && varDefValue.get(2) instanceof Token &&
                ((Token) varDefValue.get(2)).getValue().equals("getint")) {
            nowTable.put(name, new VarDecl(false, name, null, 0, null));
            return new VarDeclStmt(name);
        }
        if (varDefValue.size() <= 1 ||
                !(varDefValue.get(1) instanceof Token &&
                        ((Token) varDefValue.get(1)).getValue().equals("["))) {
            // VarDef → Ident '=' InitVal 不是数组的情况
            if (varDefValue.get(varDefValue.size() - 1).getName().equals("InitVal")) {
                ComputeStmt initial =
                        HandleComputeStmt(varDefValue.get(varDefValue.size() - 1).getValues().get(0));
                if (nowTable.isRoot()) {
                    // 全局变量初始化，其初值不能当做已知量，因为任何函数都能修改
                    assert initial.getValue() != null;
                    nowTable.put(name, new VarDecl(false, name, null, 0, null));
                } else if (initial.getValue() != null) {
                    // 一般变量也可存值，但需要根据所在block来取值
                    int initialValue = initial.getValue();
                    nowTable.put(name, new VarDecl(false, name, initialValue, 0, null));
                } else {
                    nowTable.put(name, new VarDecl(false, name, 0, null));
                }
                return new VarDeclStmt(name, nowTable.isRoot(), false, initial);
            } else {
                // VarDef → Ident
                if (nowTable.isRoot()) {
                    System.err.println("autoDef int " + name + ": 0");
                    nowTable.put(name, new VarDecl(false, name, null, 0, null));
                    return new VarDeclStmt(name, true, false, 0);
                } else {
                    System.err.println("int " + name + ": no value");
                    nowTable.put(name, new VarDecl(false, name, 0, null));
                    return new VarDeclStmt(name, false);
                }
            }
        } else {
            // 数组
            if (varDefValue.size() == 4) {     // 一维数组初始化, 咱不考虑翻译，仅填表
                // VarDef → Ident '[' ConstExp ']'
                ParserNode constExp = varDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue = constStmt.getValue();
                assert constValue != null;
                Pair<Integer, Integer> dimension = new Pair<>(constValue, 0);
                VarDecl varDecl = new VarDecl(false, name, null, 1, dimension);
                this.nowTable.put(name, varDecl);
                return new ArrDeclStmt(name, nowTable.isRoot(), false, dimension);
            } else if (varDefValue.size() == 6) {
                // VarDef → Ident '[' ConstExp ']' = InitVal
                ParserNode constExp = varDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue = constStmt.getValue();
                assert constValue != null;
                Pair<Integer, Integer> dimension = new Pair<>(constValue, 0);
                VarDecl varDecl = new VarDecl(false, name, null, 1, dimension);
                this.nowTable.put(name, varDecl);
                ParserNode initVal = varDefValue.get(5);
                List<ComputeStmt> initList = HandleInitVal(initVal);
                return new ArrDeclStmt(name, nowTable.isRoot(), false, dimension, initList);
            } else if (varDefValue.size() == 7) {
                // VarDef → Ident '[' ConstExp ']' '[' ConstExp ']'
                ParserNode constExp = varDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue1 = constStmt.getValue();
                assert constValue1 != null;
                constExp = varDefValue.get(5);
                constStmt = HandleComputeStmt(constExp);
                Integer constValue2 = constStmt.getValue();
                assert constValue2 != null;
                Pair<Integer, Integer> dimension = new Pair<>(constValue1, constValue2);
                Expr d2 = HandleComputeStmt(varDefValue.get(5));
                Integer d2Value = (Integer) d2.getValue();
                assert d2Value != null;
                VarDecl varDecl = new VarDecl(false, name, null, 1 + d2Value, dimension);
                this.nowTable.put(name, varDecl);
                return new ArrDeclStmt(name, nowTable.isRoot(), false, dimension);
            } else {    // 二维数组 初始化
                // VarDef → Ident '[' ConstExp ']' '[' ConstExp ']' = initVal
                ParserNode constExp = varDefValue.get(2);
                ComputeStmt constStmt = HandleComputeStmt(constExp);
                Integer constValue1 = constStmt.getValue();
                assert constValue1 != null;
                constExp = varDefValue.get(5);
                constStmt = HandleComputeStmt(constExp);
                Integer constValue2 = constStmt.getValue();
                assert constValue2 != null;
                Pair<Integer, Integer> dimension = new Pair<>(constValue1, constValue2);
                Expr d2 = HandleComputeStmt(varDefValue.get(5));
                Integer d2Value = (Integer) d2.getValue();
                assert d2Value != null;
                VarDecl varDecl = new VarDecl(false, name, null, 1 + d2Value, dimension);
                this.nowTable.put(name, varDecl);
                ParserNode initVal = varDefValue.get(8);
                List<ComputeStmt> initList = HandleInitVal(initVal);
                return new ArrDeclStmt(name, nowTable.isRoot(), false, dimension, initList);
            }
        }
    }

    private List<ComputeStmt> HandleInitVal(ParserNode node) throws IrError {
        List<ParserNode> nodes = node.getValues();
        List<ComputeStmt> result = new ArrayList<>();
        if (nodes.size() > 1) {
            nodes.remove(0);
            nodes.remove(nodes.size() - 1);
            for (int i = 0; i < nodes.size(); i += 2) {
                ParserNode initVal = nodes.get(i);
                if (initVal.getName().equals("InitVal") || initVal.getName().equals("ConstInitVal")) {
                    result.addAll(HandleInitVal(initVal));
                }
            }
        } else {
            result.add(HandleComputeStmt(nodes.get(0)));
        }
        return result;
    }

    private void checkReturn(CType returnType, CompoundStmt compoundStmt) {
        if (returnType == CType.VOID) {
            return;
        }
        int endLine = compoundStmt.getEndLine();
        if (compoundStmt.getStmts().size() == 0) {
            ExceptionLog.addException(new IrError(g, endLine));
            return;
        }
        Stmt last = compoundStmt.getStmts().get(compoundStmt.getStmts().size() - 1);
        if (!(last instanceof ReturnStmt)) {
            ExceptionLog.addException(new IrError(g, endLine));
        } else {
            if (((ReturnStmt) last).getReturnExpr() == null) {
                ExceptionLog.addException(new IrError(g, endLine));
            }
        }
    }

    private List<ParamPT> HandleFuncParams(ParserNode node) throws IrError {
        List<ParamPT> result = new ArrayList<>();
        List<ParserNode> nodes = node.getValues();
        for (ParserNode param : nodes) {
            if (param.getName().equals("FuncFParam")) {
                List<ParserNode> paramInfo = param.getValues();
                String name = ((Token) paramInfo.get(1)).getValue();
                if (paramInfo.size() == 2) {
                    // FuncFParam → BType Ident
                    result.add(new ParamPT(name, 0));
//                    System.err.println("funcVar: " + name + ", level: " + 0);
                } else if (paramInfo.size() == 4) {
                    // FuncFParam → BType Ident '[' ']'
                    result.add(new ParamPT(name, 1));
//                    System.err.println("funcVar: " + name + ", level: " + 1);

                } else {
                    ComputeStmt constExp = HandleComputeStmt(paramInfo.get(5));
                    Integer constValue = constExp.getValue();
                    assert constValue != null;
                    result.add(new ParamPT(name, 1 + constValue));
//                    System.err.println("funcVar: " + name + ", level: " + (1 + constValue));
                }
            }
        }
        return result;
    }

    private FuncDeclStmt HandleFuncDeclStmt(ParserNode node) throws IrError, ParserError {
        List<ParserNode> nodes = node.getValues();
        if (node.getName().equals("MainFuncDef")) {
            this.nowFuncName = "main";
            CompoundStmt compoundStmt = HandleCompoundStmt(nodes.get(4), null);
            checkReturn(CType.INT, compoundStmt);
            return new FuncDeclStmt(compoundStmt);
        } else {
            CType returnType = (((Token) nodes.get(0).getValues().get(0)).getType() == TokenType.VOIDTK) ?
                    CType.VOID : CType.INT;
            String name = ((Token) nodes.get(1)).getValue();
            int line = ((Token) nodes.get(1)).getLine();
            this.nowFuncName = name;
            if (this.nowTable.containFunc(name)) {
                ExceptionLog.addException(new IrError(b, line));
                return null;
            }
            if (nodes.get(3) instanceof Token) {
                // 函数没有参数的情况
                try {
                    System.err.println("try to add function " + name + ", returnType " + returnType);
                    root.put(name, new FuncDecl(name, new ArrayList<>(), returnType));
                    System.err.println("add successfully");
                } catch (IrError e) {
                    ExceptionLog.addException(new IrError(b, line));
                }
                CompoundStmt compoundStmt = HandleCompoundStmt(nodes.get(4), null);
                checkReturn(returnType, compoundStmt);
                return new FuncDeclStmt(returnType, name, 0, compoundStmt);
            } else {
                List<ParamPT> params = HandleFuncParams(nodes.get(3));
                int paramNum = params.size();
//                List<Pair<Integer, Integer>> paramsPrototype = new ArrayList<>(params.values());
                // 将函数参数都填表
                SymbolTable table = new SymbolTable();
                for (ParamPT param : params) {
                    Pair<Integer, Integer> dimension = null;
                    if (param.getLevel() > 1) {
                        dimension = new Pair<>(param.getLevel() - 1, 0);
                    }
                    VarDecl varDecl = new VarDecl(false, param.getName(),
                            param.getLevel(), true, dimension);
                    try {
                        table.put(param.getName(), varDecl);
                    } catch (IrError e) {
                        ExceptionLog.addException(new IrError(b, line));
                    }
                }
                try {
                    System.err.println("try to add function " + name + ", returnType " + returnType);
                    root.put(name, new FuncDecl(name, params, returnType));
                    System.err.println("add successfully");
                } catch (IrError e) {
                    ExceptionLog.addException(new IrError(b, line));
                }
                CompoundStmt compoundStmt = HandleCompoundStmt(nodes.get(5), table);
                checkReturn(returnType, compoundStmt);
                List<Integer> levelList = new ArrayList<>();
                for (ParamPT paramPT : params) {
                    levelList.add(paramPT.getLevel());
                }
                return new FuncDeclStmt(returnType, name, paramNum, compoundStmt, levelList);
            }
        }
    }

    private ComputeStmt HandleCondStmt(ParserNode node) throws IrError {
        List<ParserNode> nodes = node.getValues();
        ParserNode tail = nodes.get(nodes.size() - 1);
        CondStmt right = null;
        ComputeStmt rightContent;
        if (tail.getName().equals("AddExp")) {
            rightContent = HandleComputeStmt(tail);
        } else {
            rightContent = HandleCondStmt(tail);
        }
        int i = nodes.size() - 2;
        if (i <= 0) {
            if (!(rightContent instanceof CondStmt)) {
                if (node.getName().equals("LOrExp")) {
                    return new CondStmt(true, rightContent);
                }
                return rightContent;
            } else {
                return rightContent;
            }
        }
        while (i > 0) {
            TokenType tk = ((Token) nodes.get(i)).getType();
            Op op = tk == TokenType.AND ? Op.and :
                    tk == TokenType.OR ? Op.or :
                            tk == TokenType.EQL ? Op.eq :
                                    tk == TokenType.NEQ ? Op.ne :
                                            tk == TokenType.LEQ ? Op.le :
                                                    tk == TokenType.LSS ? Op.lt :
                                                            tk == TokenType.GEQ ? Op.ge :
                                                                    tk == TokenType.GRE ? Op.gt : null;
            if (rightContent.getValue() != null) {
                right = new CondStmt(op, HandleCondStmt(nodes.get(i - 1)), rightContent.getValue());
            } else {
                right = new CondStmt(op, HandleCondStmt(nodes.get(i - 1)), rightContent);
            }
            i -= 2;
        }
        return right;
    }

    private ComputeStmt HandleComputeStmt(ParserNode node) throws IrError {
        List<ParserNode> nodes = node.getValues();
        ComputeStmt right;
        ParserNode tail = nodes.get(nodes.size() - 1);
        if (tail.getName().equals("UnaryExp")) {
            right = HandleComputeUnaryExp(tail);
        } else {
            right = HandleComputeStmt(tail);
        }
        int i = nodes.size() - 2;
        while (i > 0) {
            TokenType tk = ((Token) nodes.get(i)).getType();
            Op op = tk == TokenType.PLUS ? Op.add :
                    tk == TokenType.MINU ? Op.minus :
                            tk == TokenType.MULT ? Op.mult :
                                    tk == TokenType.BITANDTK ? Op.and :
                                            tk == TokenType.DIV ? Op.div :
                                                    tk == TokenType.MOD ? Op.mod : null;
            if (right.getValue() != null) {
                right = new ComputeStmt(op, HandleComputeStmt(nodes.get(i - 1)), right.getValue());
            } else {
                right = new ComputeStmt(op, HandleComputeStmt(nodes.get(i - 1)), right);
            }
            i -= 2;
        }
        return right;
    }

    private ComputeStmt HandleComputeUnaryExp(ParserNode node) throws IrError {
        List<ParserNode> nodes = node.getValues();
        ParserNode head = nodes.get(0);
        if (head.getName().equals("PrimaryExp")) {
            return HandlePrimaryExp(head);
        } else if (head instanceof Token) {
            if (((Token) head).getType() == TokenType.IDENFR) {
                String name = ((Token) head).getValue();
                int line = ((Token) head).getLine();
                List<Expr> params = new ArrayList<>();
                if (nodes.get(2) instanceof Token && ((Token) nodes.get(2)).getType() == TokenType.RPARENT) {
                    // 没有参数的函数
                    try {
                        if (root.queryParamNum(name) > 0) {
                            // 参数数量不匹配错误
                            ExceptionLog.addException(new IrError(d, line));
                        }
                    } catch (IrError e) {
                        // 函数名字不存在错误
                        ExceptionLog.addException(new IrError(c, line));
                    }
                    return new ComputeStmt(inFlow > 0, new CallExpr(name, params, line));
                } else {
                    // 有参数的函数
                    List<ParserNode> funcRParams = nodes.get(2).getValues();
                    for (ParserNode param : funcRParams) {
                        if (param.getName().equals("Exp")) {
                            params.add(HandleComputeStmt(param));
                        }
                    }
                    try {    // 检测参数匹配错误
                        int len = root.queryParamNum(name);
                        boolean exception = false;
                        if (len != params.size()) {
                            ExceptionLog.addException(new IrError(d, line));
                        } else {
                            List<ParamPT> paramPTS = root.queryParams(name);
                            for (int i = 0; i < len; i++) {
                                if (!checkParam(paramPTS.get(i), (ComputeStmt) params.get(i), line)) {
                                    exception = true;
                                }
                            }
                            if (exception) {
                                ExceptionLog.addException(new IrError(e, line));
                            }
                        }
                    } catch (IrError e) {
                        // 函数名字不存在异常
                        ExceptionLog.addException(new IrError(c, line));
                    }
                    return new ComputeStmt(inFlow > 0, new CallExpr(name, params, line));
                }
            } else {
                throw new IrError("Handle Compute UnaryExp error!");
            }
        } else if (head.getName().equals("UnaryOp")) {
            head = head.getValues().get(0);
            TokenType tk = ((Token) head).getType();
            Op op = tk == TokenType.PLUS ? Op.add :
                    tk == TokenType.MINU ? Op.minus :
                            tk == TokenType.NOT ? Op.not : null;
            ComputeStmt next = HandleComputeUnaryExp(nodes.get(1));
            if (op == Op.not) {
                ComputeStmt temp = new ComputeStmt(Op.add, 0, next);
                temp.setNot(true);
                return temp;
            } else {
                return new ComputeStmt(op, 0, next);
            }
        } else {
            throw new IrError("Handle Compute UnaryExp error!");
        }
    }

    private boolean checkParam(ParamPT paramPT, ComputeStmt param, int line) {
        if (param.isLeaf()) {
            if (param.getLeafValue() instanceof DeclRefExpr) {
                return paramPT.getLevel() == ((DeclRefExpr) param.getLeafValue()).getLevel();
            } else if (param.getLeafValue() instanceof CallExpr) {
                String name = ((CallExpr) param.getLeafValue()).getFuncName();
                try {
                    CType type = root.queryReturnType(name);
                    return type != CType.VOID && paramPT.getLevel() == 0;
                } catch (IrError e) {
                    ExceptionLog.addException(new IrError(c, line));
                    return true;
                }
            } else {
                return paramPT.getLevel() == 0;
            }
        } else {
            return paramPT.getLevel() == 0;
        }
    }

    private ComputeStmt HandlePrimaryExp(ParserNode node) throws IrError {
        List<ParserNode> nodes = node.getValues();
        ParserNode head = nodes.get(0);
        if (head instanceof Token &&
                ((Token) head).getType() == TokenType.LPARENT) {
            // ( Exp )
            return HandleComputeStmt(nodes.get(1));
        } else if (head.getName().equals("LVal")) {
            // LVal
            String name = ((Token) head.getValues().get(0)).getValue();
            int line = ((Token) head.getValues().get(0)).getLine();
            if (!this.nowTable.accessVar(name)) {
                ExceptionLog.addException(new IrError(c, line));
            } else {
                List<ParserNode> values = head.getValues();
                String indentName = values.get(0).getName();
                int level = nowTable.queryLevel(indentName);
                Pair<ComputeStmt, ComputeStmt> dimension = null;
                if (values.size() == 4) {
                    // Ident '[' Exp ']'
                    level = Math.min(level - 1, 1);
                    ComputeStmt d1 = HandleComputeStmt(values.get(2));
                    dimension = new Pair<>(d1, null);
                } else if (values.size() > 4) {
                    // Ident '[' Exp ']' '[' Exp ']'
                    level = Math.min(level - 2, 0);
                    ComputeStmt d1 = HandleComputeStmt(values.get(2));
                    ComputeStmt d2 = HandleComputeStmt(values.get(5));
                    dimension = new Pair<>(d1, d2);
                }
                if (inFlow > 0) {
                    nowTable.setValue(name, null);
                }
                System.err.println("DeclRefExpr---" + name + ": value = " + nowTable.queryValue(name) + " level = " + level + ", line = " + line);
                DeclRefExpr ref;
                if (dimension == null) {
                    ref = new DeclRefExpr(false, name, nowTable, level, line);
                } else {
                    ref = new DeclRefExpr(false, name, nowTable, level, line, dimension);
                }
                return new ComputeStmt(inFlow > 0, ref);
            }
            return new ComputeStmt(inFlow > 0, new MyInteger(0));
        } else if (head.getName().equals("Number")) {
            // Number
            int value = Integer.parseInt(((Token) head.getValues().get(0)).getValue());
            return new ComputeStmt(inFlow > 0, new MyInteger(value));
        } else {
            throw new IrError("Handle Primary Exp error!");
        }
    }


    private CompoundStmt HandleCompoundStmt(ParserNode node, SymbolTable table) throws IrError, ParserError {
        // 处理符号表之间的关系
        SymbolTable newTable = new SymbolTable();
        if (table != null) {
            newTable = table;
        }
        newTable.setPre(nowTable);
        nowTable = newTable;
        List<ParserNode> nodes = node.getValues();
        assert nodes.get(0) instanceof Token && ((Token) nodes.get(0)).getType() == TokenType.LPARENT;
        //  移除 block 前后两个大括号，剩下为 BlockItem
        nodes.remove(0);
        ParserNode end = nodes.remove(nodes.size() - 1);
        List<Stmt> stmts = new ArrayList<>();   // 存放 stmt
        for (ParserNode blockItem : nodes) {
            // BlockItem → Decl | Stmt
            ParserNode content = blockItem.getValues().get(0);
            if (content.getName().equals("Stmt")) {
                Stmt stmt = HandleStmt(content);
                if (stmt != null) {
                    stmts.add(stmt);
                }
            } else if (content.getName().equals("Decl")) {
                stmts.addAll(HandleVarDeclStmt(content.getValues().get(0))); // 可能一行多个定义
            }
        }
        CompoundStmt compoundStmt = new CompoundStmt(stmts, nowTable, ((Token) end).getLine());
        this.nowTable = nowTable.getPre();
        return compoundStmt;
    }

    private Stmt HandleStmt(ParserNode node) throws IrError, ParserError {
        List<ParserNode> StmtNodes = node.getValues();
        ParserNode head = StmtNodes.get(0);
        if (head instanceof Token) {
            String name = ((Token) head).getValue();
            int line = ((Token) head).getLine();
            switch (name) {
                case "if": {
                    inFlow++;
                    CondStmt cond = (CondStmt) HandleCondStmt(StmtNodes.get(2));
                    Stmt then = HandleStmt(StmtNodes.get(4));
                    Stmt elsePart = null;
                    if (StmtNodes.size() > 5) {
                        elsePart = HandleStmt(StmtNodes.get(6));
                    }
                    inFlow--;
                    return new IfStmt(cond, then, elsePart);
                }
                case "while": {
                    inFlow++;
                    System.err.println("inWhile");
                    CondStmt cond = (CondStmt) HandleCondStmt(StmtNodes.get(2));
                    Stmt body = HandleStmt(StmtNodes.get(4));
                    System.err.println("outWhile");
                    inFlow--;
                    return new WhileStmt(cond, body);
                }
                case "break":
                    return new BreakStmt(line);
                case "continue":
                    return new ContinueStmt(line);
                case "printf":
                    System.err.println("in printf");
                    String format = ((Token) StmtNodes.get(2)).getValue();
                    List<Expr> params = new ArrayList<>();
                    if (!(StmtNodes.get(3) instanceof Token &&
                            ((Token) StmtNodes.get(3)).getType() == TokenType.RPARENT)) {
                        // 如果有参数
                        for (int i = 3; i < StmtNodes.size() - 1; i++) {
                            ParserNode StmtNode = StmtNodes.get(i);
                            if (StmtNode.getName().equals("Exp")) {
                                params.add(HandleComputeStmt(StmtNode));
                            }
                        }
                    }
                    return new CallExpr(true, format, params, line);
                case "return":
                    if (StmtNodes.size() <= 2) {
                        // Q: 考虑存在返回值的函数只有空 return？
                        return new ReturnStmt(line);
                    } else {
                        CType returnType = root.queryReturnType(this.nowFuncName);
                        if (returnType != CType.INT) {
                            ExceptionLog.addException(new IrError(f, line));
                        }
                        ComputeStmt returnExpr = HandleComputeStmt(StmtNodes.get(1));
                        return new ReturnStmt(returnExpr, line);
                    }
                case ";":
                    return null;
                default:
                    throw new IrError("Stmt Token head error: " + name);
            }
        } else {
            switch (head.getName()) {
                case "LVal":
                    List<ParserNode> values = head.getValues();
                    String name = ((Token) values.get(0)).getValue();
                    Pair<ComputeStmt, ComputeStmt> dimension = null;
                    if (values.size() == 4) {
                        ComputeStmt d1 = HandleComputeStmt(values.get(2));
                        dimension = new Pair<>(d1, null);
                    } else if (values.size() > 4) {
                        ComputeStmt d1 = HandleComputeStmt(values.get(2));
                        ComputeStmt d2 = HandleComputeStmt(values.get(5));
                        dimension = new Pair<>(d1, d2);
                    }
                    int line = ((Token) head.getValues().get(0)).getLine();
                    if (!this.nowTable.accessVar(name)) {
                        this.nowTable.put(name, new VarDecl(false, name, 0, null));
                        ExceptionLog.addException(new IrError(c, line));
                    } else if (this.nowTable.isConst(name)) {
                        ExceptionLog.addException(new IrError(h, line));
                    }
                    DeclRefExpr left;
                    if (dimension == null) {
                        left = new DeclRefExpr(inFlow > 0, true, name, nowTable);
                    } else {
                        left = new DeclRefExpr(inFlow > 0, true, name, nowTable, dimension);
                    }
                    if (StmtNodes.size() <= 4) {
                        ComputeStmt right = HandleComputeStmt(StmtNodes.get(2));
                        if (right.getValue() != null && inFlow == 0) {
                            nowTable.setValue(name, right.getValue());
                        } else {
                            nowTable.setValue(name, null);
                        }
                        return new AssignStmt(left, right);
                    } else if (StmtNodes.size() <= 6) {
                        CallExpr right = new CallExpr("getint", new ArrayList<>(), line);
                        nowTable.setValue(name, null);
                        return new AssignStmt(left, right);
                    } else {
                        throw new IrError("LVal stmt has length error: " + StmtNodes.size());
                    }
                case "Block":
                    return HandleCompoundStmt(head, null);
                case "Exp":
                    return HandleComputeStmt(head);
                default:
                    throw new IrError("Stmt ParserNode head error: " + head.getName());
            }
        }
    }


}
