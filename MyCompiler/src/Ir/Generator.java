package Ir;

import Exceptions.ExceptionLog;
import Exceptions.IrError;

import static Exceptions.ErrorType.*;

import Ir.ClangAST.AST.*;
import Ir.ClangAST.ASTMaker;
import Ir.ClangAST.SymbolTable.CType;
import Ir.ClangAST.SymbolTable.ParamPT;
import Ir.ClangAST.SymbolTable.SymbolTable;
import Ir.LLVMIR.Component.GlobalFunc;
import Ir.LLVMIR.Component.GlobalVar;
import Ir.LLVMIR.Component.Model;
import Ir.LLVMIR.IRInstr.*;
import Ir.LLVMIR.LabelSystem.*;
import utils.Pair;
import utils.Regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;

public class Generator {
    private SymbolTable nowTable;
    private static SymbolTable rootTable = null;
    private int nowReg;
    private final Root astRoot;
    private int lastOp;
    public static Instr lastInstr;
    public static boolean inGenerator = false;
    private static Generator instance = null;
    private LabelStack labelStack;
    private LabelStack trueStack;
    private LabelStack falseStack;
    private Stack<Boolean> andOr;
    private boolean isLastOr;
    private int inFlow;
    private boolean arrayDown = false;


    // 保存一个栈式存储结构，用于 break 和 continue 的回填
    public static Generator getInstance(SymbolTable table, Root astRoot) {
        if (instance == null) {
            instance = new Generator(table, astRoot);
        }
        return instance;
    }

    private Generator(SymbolTable table, Root ASTRoot) {
        nowTable = table;
        astRoot = ASTRoot;
        rootTable = ASTMaker.getRoot();
        nowReg = 0;
        inFlow = 0;
        this.trueStack = new LabelStack();
        this.falseStack = new LabelStack();
        this.isLastOr = true;
        this.andOr = new Stack<>();
    }

    private void initialReg(int n) {
        this.nowReg = n;
    }

    private int genReg() {
        lastOp = nowReg;
        return this.nowReg++;
    }

    public Model run() throws IrError {
        inGenerator = true;
        List<GlobalVar> globalVar = new ArrayList<>();
        List<GlobalFunc> globalFunc = new ArrayList<>();
        GlobalFunc main;

        List<VarDeclStmt> globalVarList = this.astRoot.getGlobalVars();
        List<FuncDeclStmt> funcDeclStmts = this.astRoot.getFunctions();
        FuncDeclStmt mainStmt = this.astRoot.getMain();

        for (VarDeclStmt varDeclStmt : globalVarList) {
            globalVar.add(HandleGlobalVarDeclStmt(varDeclStmt));
        }
        for (FuncDeclStmt funcDeclStmt : funcDeclStmts) {
            globalFunc.add(HandleFuncDeclStmt(funcDeclStmt));
        }
        main = HandleFuncDeclStmt(mainStmt);
        return new Model(globalVar, globalFunc, main);
    }

    private GlobalVar HandleGlobalVarDeclStmt(VarDeclStmt varDeclStmt) throws IrError {
        String name = varDeclStmt.getName();
        String type = varDeclStmt.isConst() ? "constant" : "global";
        if (varDeclStmt instanceof ArrDeclStmt) {
            Pair<Integer, Integer> dimension = ((ArrDeclStmt) varDeclStmt).getDimension();
            List<ComputeStmt> arrValue = ((ArrDeclStmt) varDeclStmt).getInitialList();
            List<Integer> arrConstInit = new ArrayList<>();
            if (arrValue != null) {
                for (ComputeStmt computeStmt : arrValue) {
                    Integer value = computeStmt.getValue();
                    assert value != null;
                    arrConstInit.add(value);
                }
            } else {
                arrConstInit = null;
            }
            return new GlobalVar(new GlobalVarIR(type, name, dimension, arrConstInit));
        } else {
            String value = varDeclStmt.getValue().toString();
            return new GlobalVar(new GlobalVarIR(type, name, value));
        }
    }

    private GlobalFunc HandleFuncDeclStmt(FuncDeclStmt funcDeclStmt) throws IrError {
        this.labelStack = new LabelStack(); // 每个函数都需要一个标签栈
        this.nowReg = 0;        // 每个函数的寄存器都是从 0 开始分配
        if (funcDeclStmt.isMain()) {
            initialReg(1);  // 只需为入口标签留位
            FuncDeclIR mainDecl = funcDeclStmt.genIr();
            List<Instr> content = new ArrayList<>();
            // 为函数分配返回值寄存器
            String returnReg = "%" + genReg();
            content.add(new AllocaIR(returnReg));
            this.nowTable.setReturnReg(returnReg);
            // 处理函数体
            content.addAll(HandleCompoundStmt(funcDeclStmt.getContent()));
            if (this.labelStack.needReturnLabel()) {
                // 事实上，目前除了void函数，其他函数都会这么干，但或许可以简化，譬如只有一个return 0
                this.labelStack.setReturnLabel("%" + genReg());
                content.add(new Label(this.labelStack.getReturnLabel()));
                content.add(new LoadIR("%" + genReg(), nowTable.getReturnReg(), 0));
                content.add(new RetIR("%" + lastOp));
            }
            return new GlobalFunc(mainDecl, content);
        } else {
            String name = funcDeclStmt.getName();
            CType funcType = rootTable.queryReturnType(name);
            nowTable = funcDeclStmt.getContent().getTable();
            FuncDeclIR decl = funcDeclStmt.genIr();
            List<Instr> content = new ArrayList<>();
            List<ParamPT> params = rootTable.queryParams(name);
            for (ParamPT paramPT : params) {
                if (paramPT.getLevel() >= 1) {
                    String arrayName = paramPT.getName();
                    String reg = "%" + genReg();
                    nowTable.setPlace(arrayName, reg);
                    continue;
                }
                genReg();
            }
            genReg();    // 入口标签分配寄存器
            if (funcType == CType.INT) {
                // 如果函数有返回值，就提前分配返回值的寄存器
                String returnReg = "%" + genReg();
                content.add(new AllocaIR(returnReg));
                this.nowTable.getPre().setReturnReg(returnReg);
            }
            Stack<Integer> stack = new Stack<>();
            for (ParamPT param : params) {
                if (param.getLevel() >= 1) {
                    continue;
                }
                Integer reg = genReg();
                content.add(new AllocaIR("%" + reg));
                stack.push(reg);
            }
            int paramRank = 0;
            while (stack.size() > 0) {
                if (params.get(paramRank).getLevel() >= 1) {
                    paramRank++;
                    continue;
                }
                String paramPlace = String.valueOf(stack.pop());
                content.add(new StoreIR("%" + paramRank, "%" + paramPlace));
                nowTable.setPlace(params.get(paramRank).getName(), paramPlace);
                System.err.println("set FuncParam place: " + params.get(paramRank).getName() + " " + paramPlace);
                paramRank++;
            }
            content.addAll(HandleCompoundStmt(funcDeclStmt.getContent()));
            if (this.labelStack.needReturnLabel()) {
                this.labelStack.setReturnLabel("%" + genReg());
                content.add(new Label(this.labelStack.getReturnLabel()));
            }
            if (funcType == CType.VOID) {
                content.add(new RetIR());
            } else {
                content.add(new LoadIR("%" + genReg(), nowTable.getReturnReg(), 0));
                content.add(new RetIR("%" + lastOp));
            }
            return new GlobalFunc(decl, content);
        }
    }

    private List<Instr> HandleCompoundStmt(CompoundStmt compoundStmt) throws IrError {
        nowTable = compoundStmt.getTable();
        List<Stmt> stmts = compoundStmt.getStmts();
        List<Instr> Instrs = new ArrayList<>();
        for (Stmt stmt : stmts) {
            if (stmt instanceof VarDeclStmt) {
                Instrs.addAll(HandleVarDecl((VarDeclStmt) stmt));
            } else {
                Instrs.addAll(HandleStmt(stmt));
                if (stmt instanceof ReturnStmt || stmt instanceof ContinueStmt || stmt instanceof BreakStmt) {
                    break;
                }
            }
        }
        nowTable = nowTable.getPre();   // 处理完 compoundStmt 就会回退到上一个nowTable
        return Instrs;
    }

    private List<Instr> HandleVarDecl(VarDeclStmt varDeclStmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        if (varDeclStmt instanceof ArrDeclStmt) {
            ArrDeclStmt declStmt = (ArrDeclStmt) varDeclStmt;
            String name = declStmt.getName();
            Pair<Integer, Integer> dimension = declStmt.getDimension();
            List<ComputeStmt> initList = declStmt.getInitialList();
            String reg = "%" + genReg();
            result.add(new AllocaIR(reg, dimension));
            nowTable.setPlace(name, reg);
            if (initList != null && initList.size() > 0) {
                int d1 = dimension.getKey();
                int d2 = dimension.getValue();
                if (d2 == 0) {
                    for (int i = 0; i < d1; i++) {
                        List<String> offset = new ArrayList<>();
                        offset.add("0");
                        offset.add(i + "");
                        String aim = "%" + genReg();
                        result.add(new GetElementPtrIR(aim, reg, dimension, offset));
                        ComputeStmt computeStmt = initList.get(i);
                        if (computeStmt.getValue() != null) {
                            result.add(new StoreIR(computeStmt.getValue() + "", aim));
                        } else {
                            result.addAll(HandleComputeStmt(computeStmt));
                            result.add(new StoreIR("%" + lastOp, aim));
                        }
                    }
                } else {
                    for (int i = 0; i < d1; i++) {
                        for (int j = 0; j < d2; j++) {
                            List<String> offset = new ArrayList<>();
                            offset.add("0");
                            offset.add(i + "");
                            offset.add(j + "");
                            String aim = "%" + genReg();
                            result.add(new GetElementPtrIR(aim, reg, dimension, offset));
                            ComputeStmt computeStmt = initList.get(i * d2 + j);
                            if (computeStmt.getValue() != null) {
                                result.add(new StoreIR(computeStmt.getValue() + "", aim));
                            } else {
                                result.addAll(HandleComputeStmt(computeStmt));
                                result.add(new StoreIR("%" + lastOp, aim));
                            }
                        }
                    }
                }
            }
        } else {
            // alloca 为变量分配地址
            int reg = genReg();
            result.add(new AllocaIR("%" + reg));
            nowTable.setPlace(varDeclStmt.getName(), reg + "");
            String name = varDeclStmt.getName();
            String place = nowTable.queryPlace(name);
            if (varDeclStmt.isConst()) {
                // 如果是const变量，则直接存值即可
                Integer constValue = nowTable.queryValue(name);
                result.add(new StoreIR(constValue.toString(), place));
            } else {
                // 其他变量，如果有初始化，则需要计算并存值
                if (varDeclStmt.hasValue()) {
                    Integer varValue = varDeclStmt.getValue();
                    if (varValue != null) {
                        // 如果有现成的值则存值
                        result.add(new StoreIR(varValue.toString(), place));
                    } else {
                        // 没有现成的值，则需解析 initial 表达式
                        List<Instr> computeInstr = HandleComputeStmt(varDeclStmt.getInitial());
                        String resultPlace = "%" + lastOp;
                        result.addAll(computeInstr);
                        result.add(new StoreIR(resultPlace, place));
                    }
                } else if (varDeclStmt.isGetInt()) {
                    result.add(new CallIR("%" + genReg(), "getint", null, "i32"));
                    result.add(new StoreIR("%" + lastOp, place));
                } else {
                    // 该变量没初始化，所以暂时没值，只有存储位置，测试数据应该不会到这一步？
                }
            }
        }
        return result;
    }

    private List<Instr> HandleStmt(Stmt stmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        if (stmt instanceof AssignStmt) {
            result.addAll(HandleAssignStmt((AssignStmt) stmt));
        } else if (stmt instanceof CompoundStmt) {
            result.addAll(HandleCompoundStmt((CompoundStmt) stmt));
        } else if (stmt instanceof Expr) {
            result.addAll(HandleExpr((Expr) stmt));
        } else if (stmt instanceof ReturnStmt) {
            ComputeStmt returnExpr = ((ReturnStmt) stmt).getReturnExpr();
            if (returnExpr == null) {    // break; return;
                if (!(lastInstr instanceof BrIR)) {
                    result.add(new BrIR(labelStack.getReturnLabel()));
                }
            } else {
                if (lastInstr instanceof BrIR) {
                    // return 不可达
                    result.add(new Label(new LabelValue("%" + genReg())));
                }
                // 错误处理此处要处理
                if (returnExpr.isLeaf() && returnExpr.getValue() != null) {
                    // return 0;
                    Integer leafValue = returnExpr.getValue();
                    result.add(new StoreIR(leafValue + "", nowTable.getReturnReg()));
                    result.add(new BrIR(labelStack.getReturnLabel()));
                } else {
                    // return a + b;
                    List<Instr> returnIrs = HandleComputeStmt(returnExpr);
                    result.addAll(returnIrs);
                    String resultPlace = "%" + lastOp;
                    result.add(new StoreIR(resultPlace, nowTable.getReturnReg()));
                    result.add(new BrIR(labelStack.getReturnLabel()));
                }
            }
        } else if (stmt instanceof WhileStmt) {
            inFlow++;
            result.addAll(HandleWhileStmt((WhileStmt) stmt));
            inFlow--;
        } else if (stmt instanceof IfStmt) {
            inFlow++;
            result.addAll(HandleIfStmt((IfStmt) stmt));
            inFlow--;
        } else if (stmt instanceof BreakStmt) {
            try {
                WhileUnit nowWhile = (WhileUnit) this.labelStack.peek();
                if (!(lastInstr instanceof BrIR)) {       // return; break;
                    result.add(new BrIR(nowWhile.getLabel(2), "break"));     // br end
                }
            } catch (IrError e) {
                int line = ((BreakStmt) stmt).getLine();
                ExceptionLog.addException(new IrError(m, line));
            }

        } else if (stmt instanceof ContinueStmt) {
            try {
                WhileUnit nowWhile = (WhileUnit) this.labelStack.peek();
                if (!(lastInstr instanceof BrIR)) {       // return; continue;
                    result.add(new BrIR(nowWhile.getLabel(nowWhile.isTrue() ? 1 : 0), "continue"));     // br end
                }
            } catch (IrError e) {
                int line = ((ContinueStmt) stmt).getLine();
                ExceptionLog.addException(new IrError(m, line));
            }
        } else if (stmt == null) {
            return result;
        } else {
            throw new IrError("Error Stmt type!");
        }
        return result;
    }

    public List<Instr> HandleAssignStmt(AssignStmt stmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        DeclRefExpr left = stmt.getLeft();       // 等号左侧 LVal
        String name = left.getName();
        assert left.isLeft();   // 必定在等号左边
        String leftPlace = left.getPlace();
        if (left.getDimension() != null) {       // 如果 dimension 不为 null 则为数组
            Pair<ComputeStmt, ComputeStmt> dimension = left.getDimension();
            boolean isFuncParam = left.isFuncParam();
            ComputeStmt d1 = dimension.getKey();
            ComputeStmt d2 = dimension.getValue();
            String d1Value;
            String d2Value = null;
            if (d1.getValue() != null) {
                d1Value = d1.getValue() + "";
            } else {
                result.addAll(HandleComputeStmt(d1));
                d1Value = "%" + lastOp;
            }
            if (d2 != null) {
                if (d2.getValue() != null) {
                    d2Value = d2.getValue() + "";
                } else {
                    result.addAll(HandleComputeStmt(d2));
                    d2Value = "%" + lastOp;
                }
            }
            // 取出数组赋值对象
            List<String> offset = new ArrayList<>();
            if (!isFuncParam) {
                offset.add("0");
            }
            offset.add(d1Value);
            if (d2Value != null) {
                offset.add(d2Value);
            }
            result.add(new GetElementPtrIR("%" + genReg(), leftPlace, nowTable.getDimension(name), offset));
            leftPlace = "%" + lastOp;
        }
        Expr assignRight = stmt.getRight();
        if (assignRight.getValue() != null) {
            result.add(new StoreIR(assignRight.getValue() + "", leftPlace));
        } else {
            nowTable.setValue(name, null);
            List<Instr> rightInstr = HandleExpr(assignRight);
            result.addAll(rightInstr);
            String resultPlace = "%" + lastOp;
            result.add(new StoreIR(resultPlace, leftPlace));
        }
        return result;
    }

    private List<Instr> HandleWhileStmt(WhileStmt stmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        CondStmt cond = stmt.getCond();
        Stmt body = stmt.getBody();
        WhileUnit whileUnit = new WhileUnit();
        this.labelStack.push(whileUnit);
        if (cond.getValue() != null) {
            if (cond.getValue() == 0) {
                // 如果循环条件为假，则内部语句不可达
                // HandleStmt(body);        // 错误处理再开启
                labelStack.pop();
                return result;
            }
        }
        if (cond.getValue() != null && cond.getValue() == 1) {        // 如果条件永为真
            whileUnit.setTrue();
            if (!(lastInstr instanceof BrIR)) {
                whileUnit.setLabel(1, "%" + genReg());
                result.add(new BrIR(whileUnit.getLabel(1)));    // br body
                result.add(new Label(whileUnit.getLabel(1), "while body"));
            } else {
                LabelValue labelValue = new LabelValue("%"+genReg());
                result.add(new Label(labelValue));
                whileUnit.setLabel(1, labelValue.toString());
            }
            result.addAll(HandleStmt(body));
            if (!(lastInstr instanceof BrIR)) {
                result.add(new BrIR(whileUnit.getLabel(1)));    // br body
            }
            if (whileUnit.getRef(2) > 0) {
                result.add(new Label(whileUnit.getLabel(2), "while end"));   // end:
                whileUnit.setLabel(2, "%" + genReg());
            }
        } else {
            LabelValue whileBodyValue = whileUnit.getLabel(1);
            LabelValue whileEndValue = whileUnit.getLabel(2);
            LabelUnit whileBody = new LabelUnit(whileBodyValue);
            LabelUnit whileEnd = new LabelUnit(whileEndValue);
            whileUnit.setLabel(0, "%" + genReg());
            if (!(lastInstr instanceof BrIR)) {
                result.add(new BrIR(whileUnit.getLabel(0)));    // br cond
            }
            result.add(new Label(whileUnit.getLabel(0), "while cond"));   // cond:
            trueStack.push(whileBody);
            falseStack.push(whileEnd);
            andOr.push(true);   // 最外层是 and 和 or
            result.addAll(HandleCondStmt(cond));
            LabelUnit label = trueStack.pop();    // assert pop 的一定是 body label
            label.setLabel("%" + genReg());
            whileUnit.setLabel(1, label.getLabel().toString());
            result.add(new Label(whileUnit.getLabel(1), "while body"));   // body:
            result.addAll(HandleStmt(body));
            if (!(lastInstr instanceof BrIR)) {
                result.add(new BrIR(whileUnit.getLabel(0)));    // br cond
            }
            label = falseStack.pop();
            label.setLabel("%" + genReg());
            whileUnit.setLabel(2, label.getLabel().toString());
            result.add(new Label(whileUnit.getLabel(2), "while end"));   // end:
        }
        labelStack.pop();
        return result;
    }

    private List<Instr> HandleIfStmt(IfStmt stmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        CondStmt condStmt = stmt.getCond();
        Stmt then = stmt.getThenPart();
        assert then != null;
        Stmt elsePart = stmt.getElsePart();
        Integer condValue = condStmt.getValue();
        if (condValue != null) {   // 如果cond的值可以确定
            if (condValue == 1) {
                // 能获取cond值且为真
                result.addAll(HandleStmt(then));
            } else {
                // HandleStmt(then);     // 错误处理在开启
                if (elsePart != null) {
                    result.addAll(HandleStmt(elsePart));
                }
            }
        } else {        // 如果cond的值需要现算
            IfUnit ifUnit = new IfUnit();
            LabelValue ifBodyValue = ifUnit.getLabel(0);
            LabelValue ifElseValue = ifUnit.getLabel(1);
            LabelValue ifEndValue = ifUnit.getLabel(2);
            LabelUnit ifBody = new LabelUnit(ifBodyValue);
            LabelUnit ifElse = new LabelUnit(ifElseValue);
            LabelUnit ifEnd = new LabelUnit(ifEndValue);
            andOr.push(true);
            if (elsePart == null) {
                trueStack.push(ifBody);
                falseStack.push(ifEnd);
                result.addAll(HandleCondStmt(condStmt));
            } else {
                trueStack.push(ifBody);
                falseStack.push(ifElse);
                result.addAll(HandleCondStmt(condStmt));
            }
            LabelUnit labelUnit = trueStack.pop();    // assert pop 的一定是 body label
            labelUnit.setLabel("%" + genReg());
            ifUnit.setLabel(0, labelUnit.getLabel().toString());
            result.add(new Label(ifUnit.getLabel(0), "if then"));  // then :
            result.addAll(HandleStmt(then));
            if (elsePart != null) {
                labelUnit = falseStack.pop();    // assert pop 的一定是 else label
                labelUnit.setLabel("%" + genReg());
                ifUnit.setLabel(1, labelUnit.getLabel().toString());     // else
                if (!(lastInstr instanceof BrIR)) {
                    result.add(new BrIR(ifUnit.getLabel(2)));       // br end
                }
                result.add(new Label(ifUnit.getLabel(1), "if else"));      // else:
                result.addAll(HandleStmt(elsePart));
            }
            if (!(lastInstr instanceof BrIR)) {
                result.add(new BrIR(ifUnit.getLabel(2)));       // br end
            }
            if (ifUnit.getRef(2) > 0) {         // 如果 end 的引用大于 0
                ifEndValue.setLabelValue("%" + genReg());
                ifUnit.setLabel(2, ifEndValue.toString());
                result.add(new Label(ifUnit.getLabel(2), "if end"));      // end:
                try {
                    if (elsePart == null) {
                        falseStack.pop();    // assert pop 的一定是 end label;
                    }
                } catch (Exception ignore) {
                }
            }
            System.err.println("if end refs: " + ifUnit.getRef(2));
        }
        return result;
    }

    private List<Instr> HandleExpr(Expr expr) throws IrError {
        List<Instr> result = new ArrayList<>();
        if (expr instanceof DeclRefExpr) {
            DeclRefExpr ref = (DeclRefExpr) expr;
            String name = ref.getName();
            Integer value = ref.getValue();
            String place = ref.getPlace();
            Pair<ComputeStmt, ComputeStmt> dimension = ref.getDimension();
            int level = ref.getLevel();
            if (dimension != null) {
                ComputeStmt d1 = dimension.getKey();
                ComputeStmt d2 = dimension.getValue();
                String d1Value;
                String d2Value = null;
                if (d1.getValue() == null) {
                    result.addAll(HandleComputeStmt(d1));
                    d1Value = "%" + lastOp;
                } else {
                    d1Value = d1.getValue() + "";
                }
                if (d2 != null) {
                    if (d2.getValue() == null) {
                        result.addAll(HandleComputeStmt(d2));
                        d2Value = "%" + lastOp;
                    } else {
                        d2Value = d2.getValue() + "";
                    }
                }
                List<String> offset = new ArrayList<>();
                if (!nowTable.isFuncParam(name)) {
                    offset.add("0");
                }
                offset.add(d1Value);
                if (d2Value != null) {
                    offset.add(d2Value);
                }
                if (arrayDown) {         // 可能存在 a[2][2] 取 a[1] 的情况, 注意该语句的位置
                    offset.add("0");
                }
                Pair<Integer, Integer> dimensionPT = nowTable.getDimension(name);
                result.add(new GetElementPtrIR("%" + genReg(), place, dimensionPT, offset));
                place = "%" + lastOp;
            } else if (level >= 1) {  // 平级偏移量为 0
                List<String> offset = new ArrayList<>();
                offset.add("0");
                if (arrayDown) {
                    offset.add("0");
                }
                Pair<Integer, Integer> dimensionPT = nowTable.getDimension(name);
                result.add(new GetElementPtrIR("%" + genReg(), place, dimensionPT, offset));
                place = "%" + lastOp;
            }
            if (value == null && level == 0) {
                result.add(new LoadIR("%" + genReg(), place, level));
            }
        } else if (expr instanceof ComputeStmt) {
            result.addAll(HandleComputeStmt((ComputeStmt) expr));
        } else if (expr instanceof CallExpr) {
            CallExpr callExpr = (CallExpr) expr;
            String funcName = callExpr.getFuncName();
            List<Expr> params = callExpr.getParams();
            if (callExpr.isPrintf()) {
                result.addAll(HandlePrintf(callExpr));
            } else {
                String type = rootTable.queryReturnType(funcName) == CType.VOID ? "void" : "i32";
                if (params != null) {
                    Pair<List<Pair<String, Integer>>, List<Instr>> paramResult = HandleCallParams(params);
                    List<Pair<String, Integer>> paramsValue = paramResult.getKey();
                    if (paramResult.getValue() != null) {
                        result.addAll(paramResult.getValue());
                    }
                    if (type.equals("void")) {
                        result.add(new CallIR(funcName, paramsValue, type));
                    } else {
                        result.add(new CallIR("%" + genReg(), funcName, paramsValue, type));
                    }
                } else {
                    if (type.equals("void")) {
                        result.add(new CallIR(funcName, null, type));
                    } else {
                        result.add(new CallIR("%" + genReg(), funcName, null, type));
                    }
                }
            }
        } else {
            throw new IrError("Expr type error!");
        }
        return result;
    }

    private List<Instr> HandlePrintf(CallExpr callExpr) throws IrError {
        List<Instr> result = new ArrayList<>();
        String format = callExpr.getFormat().replace("\"", "");
        List<Expr> params = callExpr.getParams();
        Matcher m = Regex.parseD(format);
        int count = 0;
        List<Integer> startPlace = new ArrayList<>();
        while (m.find()) {
            count++;
            startPlace.add(m.start());
        }
        System.err.println("format: " + format + ", start places: " + startPlace);
        if (count != params.size()) {
            int line = callExpr.getLine();
            ExceptionLog.addException(new IrError(l, line));
            return result;
        }
        Stack<List<Pair<String, Integer>>> intParamList = new Stack<>();
        for (int i = startPlace.size() - 1; i >= 0; i--) {
            List<Pair<String, Integer>> intParam = new ArrayList<>();
            if (params.get(i).getValue() != null) {
                intParam.add(new Pair<>(params.get(i).getValue() + "", 0));
            } else {
                List<Instr> exprInstrs = HandleExpr(params.get(i));
                result.addAll(exprInstrs);
                String paramResult = "" + lastOp;
                intParam.add(new Pair<>(paramResult, 1));
            }
            intParamList.push(intParam);
        }
        int lastEnd = 0;
        for (int i = 0; i < startPlace.size(); i++) {
            int start = startPlace.get(i);
            for (int j = lastEnd; j < start; j++) {
                List<Pair<String, Integer>> paramList = new ArrayList<>();
                if (format.charAt(j) == '\\') {
                    j++;
                    paramList.add(new Pair<>(10 + "", 0));
                    result.add(new CallIR("putch", paramList, "void"));
                } else {
                    paramList.add(new Pair<>((int) format.charAt(j) + "", 0));
                    result.add(new CallIR("putch", paramList, "void"));
                }
            }
            result.add(new CallIR("putint", intParamList.pop(), "void"));
            lastEnd = start + 2;
        }
        if (lastEnd <= format.length() - 1) {
            for (int j = lastEnd; j < format.length(); j++) {
                List<Pair<String, Integer>> paramList = new ArrayList<>();
                if (format.charAt(j) == '\\') {
                    j++;
                    paramList.add(new Pair<>(10 + "", 0));
                    result.add(new CallIR("putch", paramList, "void"));
                } else {
                    paramList.add(new Pair<>((int) format.charAt(j) + "", 0));
                    result.add(new CallIR("putch", paramList, "void"));
                }
            }
        }
        return result;
    }

    private Pair<List<Pair<String, Integer>>, List<Instr>> HandleCallParams(List<Expr> params) throws IrError {
        List<Pair<String, Integer>> paramsValue = new ArrayList<>();
        List<Instr> paramInstr = new ArrayList<>();
        for (Expr paramExpr : params) {
            if (paramExpr.getValue() != null && paramExpr.getValue() instanceof Integer) {
                paramsValue.add(new Pair<>("" + paramExpr.getValue(), 0));
            } else {
                if (paramExpr instanceof DeclRefExpr ||
                        paramExpr instanceof ComputeStmt &&
                                ((ComputeStmt) paramExpr).isLeaf() &&
                                ((ComputeStmt) paramExpr).getLeafValue() instanceof DeclRefExpr) {
                    DeclRefExpr refExpr;
                    if (paramExpr instanceof DeclRefExpr) {
                        refExpr = (DeclRefExpr) paramExpr;
                    } else {
                        refExpr = (DeclRefExpr) ((ComputeStmt) paramExpr).getLeafValue();
                    }
                    if (refExpr.getLevel() >= 1 && !refExpr.isFuncParam() ||
                            refExpr.isFuncParam() &&
                                    refExpr.getLevel() < nowTable.queryLevel(refExpr.getName()) &&
                                    nowTable.queryLevel(refExpr.getName()) > 1 && refExpr.getLevel() > 0) {
                        // 数组类型（不包括直接 i32）直接作为函数参数需要降维
                        this.arrayDown = true;
                    }
                }
                paramInstr.addAll(HandleExpr(paramExpr));
                arrayDown = false;
                String resultPlace = "" + lastOp;
                paramsValue.add(new Pair<>(resultPlace, 1));
            }
        }
        if (paramInstr.size() == 0) {
            paramInstr = null;
        }
        return new Pair<>(paramsValue, paramInstr);
    }

    private List<Instr> HandleCondCompute(ComputeStmt computeStmt) throws IrError {
        List<Instr> result = new ArrayList<>(HandleComputeStmt(computeStmt));
        String resultPlace = "%" + lastOp;
        String cond;
        if (computeStmt.isNot()) {
            cond = "eq";
        } else {
            cond = "ne";
        }
        result.add(new IcmpIR("%" + genReg(), cond, "0", resultPlace));
        String place = "%" + lastOp;
        result.add(new BrIR(place, trueStack.peek().getLabel(), falseStack.peek().getLabel()));
        return result;
    }

    private List<Instr> HandleCondStmt(CondStmt condStmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        Op op = condStmt.getOp();
        if (condStmt.isLeaf() || condStmt.getValue() != null) {         // cond 可以直接计算的情况，在进入该函数前就已被讨论
            LeafStmt leafValue = condStmt.getLeafValue();
            assert leafValue instanceof ComputeStmt;
            result.addAll(HandleCondCompute((ComputeStmt) leafValue));
        } else {
            if (op == Op.and || op == Op.or) {
                andOr.push(true);
                LabelValue labelValue = new LabelValue();
                LabelUnit label = new LabelUnit(labelValue);
                if (op == Op.and) {
                    trueStack.push(label);
                    if (condStmt.isLeftLeaf()) {
                        result.add(new IcmpIR("%" + genReg(), "ne", "0", condStmt.getLeftValue() + ""));
                        result.add(new BrIR("%" + lastOp, trueStack.peek().getLabel(), falseStack.peek().getLabel()));
                    } else {
                        ComputeStmt leftExpr = condStmt.getLeftExpr();
                        if (leftExpr instanceof CondStmt) {
                            result.addAll(HandleCondStmt((CondStmt) leftExpr));
                        } else {
                            result.addAll(HandleCondCompute(leftExpr));
                        }
                    }
                    label = trueStack.pop();
                    label.setLabel("%" + genReg());
                    result.add(new Label(label.getLabel()));
                    if (condStmt.isRightLeaf()) {
                        result.add(new IcmpIR("%" + genReg(), "ne", "0", condStmt.getRightValue() + ""));
                        result.add(new BrIR("%" + lastOp, trueStack.peek().getLabel(), falseStack.peek().getLabel()));
                    } else {
                        ComputeStmt rightExpr = condStmt.getRightExpr();
                        if (rightExpr instanceof CondStmt) {
                            result.addAll(HandleCondStmt((CondStmt) rightExpr));
                        } else {
                            result.addAll(HandleCondCompute(rightExpr));
                        }
                    }
                } else {
                    falseStack.push(label);
                    if (condStmt.isLeftLeaf()) {
                        result.add(new IcmpIR("%" + genReg(), "ne", "0", condStmt.getLeftValue() + ""));
                        result.add(new BrIR("%" + lastOp, trueStack.peek().getLabel(), falseStack.peek().getLabel()));
                    } else {
                        ComputeStmt leftExpr = condStmt.getLeftExpr();
                        if (leftExpr instanceof CondStmt) {
                            result.addAll(HandleCondStmt((CondStmt) leftExpr));
                        } else {
                            result.addAll(HandleCondCompute(leftExpr));
                        }
                    }
                    label = falseStack.pop();
                    label.setLabel("%" + genReg());
                    result.add(new Label(label.getLabel()));
                    if (condStmt.isRightLeaf()) {
                        result.add(new IcmpIR("%" + genReg(), "ne", "0", condStmt.getRightValue() + ""));
                        result.add(new BrIR("%" + lastOp, trueStack.peek().getLabel(), falseStack.peek().getLabel()));
                    } else {
                        ComputeStmt rightExpr = condStmt.getRightExpr();
                        if (rightExpr instanceof CondStmt) {
                            result.addAll(HandleCondStmt((CondStmt) rightExpr));
                        } else {
                            result.addAll(HandleCondCompute(rightExpr));
                        }
                    }
                }
                andOr.pop();
            } else {
                andOr.push(false);
                String op1;
                String op2;
                if (condStmt.isLeftLeaf()) {
                    op1 = condStmt.getLeftValue() + "";
                } else {
                    ComputeStmt leftExpr = condStmt.getLeftExpr();
                    Pair<List<Instr>, String> condValue = CondValue(leftExpr, op);
                    List<Instr> valueInstr = condValue.getKey();
                    result.addAll(valueInstr);
                    op1 = condValue.getValue();
                }
                if (condStmt.isRightLeaf()) {
                    op2 = condStmt.getRightValue() + "";
                } else {
                    ComputeStmt rightExpr = condStmt.getRightExpr();
                    Pair<List<Instr>, String> condValue = CondValue(rightExpr, op);
                    List<Instr> valueInstr = condValue.getKey();
                    result.addAll(valueInstr);
                    op2 = condValue.getValue();
                }
                result.add(BinaryInstr(op, "%" + genReg(), op1, op2));
                andOr.pop();
                boolean isAndOr = andOr.peek();
                if (isAndOr) {
                    result.add(new BrIR("%" + lastOp, trueStack.peek().getLabel(), falseStack.peek().getLabel()));
                }
            }
        }
        return result;
    }

    private Pair<List<Instr>, String> CondValue(ComputeStmt expr, Op op) throws IrError {
        List<Instr> result = new ArrayList<>();
        String opr;
        if (expr instanceof CondStmt) {
            CondStmt condExpr = (CondStmt) expr;
            if (condExpr.getValue() != null) {
                opr = condExpr.getValue() + "";
            } else {
                result.addAll(HandleCondStmt(condExpr));
                opr = "%" + lastOp;
                if (lastInstr instanceof LogicIR && needIcmp(op)) {
                    result.add(new ZextIR("%" + genReg(), "i1", opr, "i32"));
                    opr = "%" + lastOp;
                }
            }
        } else {
            if (expr.getValue() != null) {
                opr = expr.getValue() + "";
            } else {
                result.addAll(HandleComputeStmt(expr));
                opr = "%" + lastOp;
                if (expr.isNot()) {
                    result.add(new IcmpIR("%" + genReg(), "eq", "0", opr));
                }
                opr = "%" + lastOp;
                if (lastInstr instanceof LogicIR && needIcmp(op)) {
                    result.add(new ZextIR("%" + genReg(), "i1", opr, "i32"));
                    opr = "%" + lastOp;
                }
            }
        }
        return new Pair<>(result, opr);
    }

    private List<Instr> HandleComputeStmt(ComputeStmt computeStmt) throws IrError {
        List<Instr> result = new ArrayList<>();
        Op op = computeStmt.getOp();
        if (computeStmt.isLeaf()) {         // compute 可以直接计算的情况，在进入该函数前就已被讨论
            LeafStmt leafValue = computeStmt.getLeafValue();
            if (leafValue instanceof CallExpr) {
                List<Expr> params = ((CallExpr) leafValue).getParams();
                String funcName = ((CallExpr) leafValue).getFuncName();
                Pair<List<Pair<String, Integer>>, List<Instr>> paramResult = null;
                List<Pair<String, Integer>> paramsValue = null;
                if (params != null) {
                    paramResult = HandleCallParams(params);
                    paramsValue = paramResult.getKey();
                }
                if (paramResult != null && paramResult.getValue() != null) {
                    result.addAll(paramResult.getValue());
                }
                try {
                    if (rootTable.queryReturnType(funcName) == CType.VOID) {
                        result.add(new CallIR(funcName, paramsValue, "void"));
                    } else {
                        result.add(new CallIR("%" + genReg(), funcName, paramsValue, "i32"));
                    }
                } catch (IrError ignore) {
                    // 错误处理不需要生成代码
                }
            } else if (leafValue instanceof DeclRefExpr) {
                DeclRefExpr ref = (DeclRefExpr) leafValue;
                String name = ref.getName();
                String place = ref.getPlace();
                Pair<ComputeStmt, ComputeStmt> dimension = ref.getDimension();
                int level = ref.getLevel();
                if (dimension != null) {
                    ComputeStmt d1 = dimension.getKey();
                    ComputeStmt d2 = dimension.getValue();
                    String d1Value;
                    String d2Value = null;
                    if (d1.getValue() == null) {
                        result.addAll(HandleComputeStmt(d1));
                        d1Value = "%" + lastOp;
                    } else {
                        d1Value = d1.getValue() + "";
                    }
                    if (d2 != null) {
                        if (d2.getValue() == null) {
                            result.addAll(HandleComputeStmt(d2));
                            d2Value = "%" + lastOp;
                        } else {
                            d2Value = d2.getValue() + "";
                        }
                    }
                    List<String> offset = new ArrayList<>();
                    if (!nowTable.isFuncParam(name)) {
                        offset.add("0");
                    }
                    offset.add(d1Value);
                    if (d2Value != null) {
                        offset.add(d2Value);
                    }
                    if (arrayDown) {         // 可能存在 a[2][2] 取 a[1] 的情况, 注意该语句的位置
                        offset.add("0");
                    }
                    Pair<Integer, Integer> dimensionPT = nowTable.getDimension(name);
                    result.add(new GetElementPtrIR("%" + genReg(), place, dimensionPT, offset));
                    place = "%" + lastOp;
                } else if (level >= 1) {  // 无dimension偏移，平级偏移且为 0
                    List<String> offset = new ArrayList<>();
                    offset.add("0");
                    if (arrayDown) {
                        offset.add("0");
                    }
                    Pair<Integer, Integer> dimensionPT = nowTable.getDimension(name);
                    result.add(new GetElementPtrIR("%" + genReg(), place, dimensionPT, offset));
                    place = "%" + lastOp;
                }
                if (level == 0) {
                    result.add(new LoadIR("%" + genReg(), place, level));
                }
            }
        } else {
            String op1;
            String op2;
            if (computeStmt.isLeftLeaf()) {
                op1 = computeStmt.getLeftValue() + "";
            } else {
                List<Instr> irs = HandleExpr(computeStmt.getLeftExpr());
                result.addAll(irs);
                op1 = "%" + lastOp;
            }
            if (computeStmt.isRightLeaf()) {
                op2 = computeStmt.getRightValue() + "";
            } else {
                List<Instr> irs = HandleExpr(computeStmt.getRightExpr());
                result.addAll(irs);
                op2 = "%" + lastOp;
            }
            result.add(BinaryInstr(op, "%" + genReg(), op1, op2));
        }
        return result;
    }

    private Instr BinaryInstr(Op op, String result, String op1, String op2) throws IrError {
        if (op == Op.add) {
            return new AddIR(result, op1, op2);
        } else if (op == Op.div) {
            return new SdivIR(result, op1, op2);
        } else if (op == Op.mod) {
            return new SremIR(result, op1, op2);
        } else if (op == Op.mult) {
            return new MulIR(result, op1, op2);
        } else if (op == Op.and) {
            return new AndIR(result, op1, op2);
        } else if (op == Op.minus) {
            return new SubIR(result, op1, op2);
        } else if (op == Op.eq) {
            return new IcmpIR(result, "eq", op1, op2);
        } else if (op == Op.ne) {
            return new IcmpIR(result, "ne", op1, op2);
        } else if (op == Op.le) {
            return new IcmpIR(result, "sle", op1, op2);
        } else if (op == Op.ge) {
            return new IcmpIR(result, "sge", op1, op2);
        } else if (op == Op.gt) {
            return new IcmpIR(result, "sgt", op1, op2);
        } else if (op == Op.lt) {
            return new IcmpIR(result, "slt", op1, op2);
        } else {
            throw new IrError("Binary Ir error!");
        }
    }

    private boolean needIcmp(Op op) {
        return op == Op.eq || op == Op.ne || op == Op.le || op == Op.ge || op == Op.gt || op == Op.lt;
    }
}
