import Exceptions.ExceptionLog;
import Exceptions.IrError;
import Exceptions.ParserError;
import FrontEnd.Lexer;
import FrontEnd.LexerController;
import FrontEnd.Parser;
import Ir.ClangAST.AST.Root;
import Ir.ClangAST.ASTMaker;
import FrontEnd.ParserNode;
import FrontEnd.Token;
import Ir.LLVMIR.Component.Model;
import Ir.Generator;
import utils.Regex;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.regex.Matcher;

public class Compiler {
    public static void LexerTest(){
        Lexer lexer = new Lexer();
        Token token = lexer.getSymbol();
        while (token != null) {
            System.out.println(token);
            token = lexer.getSymbol();
        }
    }

    public static void ParserTest() throws ParserError {
        Lexer lexer = new Lexer();
        LexerController lexerController = new LexerController(lexer);
        Parser parser = new Parser(lexerController);
        ParserNode root = parser.run();
        System.out.println(root.toString());
    }

    public static void IrTest() throws ParserError, IrError {
        Lexer lexer = new Lexer();
        LexerController lexerController = new LexerController(lexer);
        Parser parser = new Parser(lexerController);
        ParserNode root = parser.run();
        ASTMaker ast = ASTMaker.getInstance(root);
        Root astRoot = ast.HandleRoot();
        Generator generator = Generator.getInstance(ASTMaker.getRoot(), astRoot);
        Model model = generator.run();
        System.out.println(model.toString());
        System.err.println("finished");
    }

    public static void ErrTest() throws ParserError, IrError {
        Lexer lexer = new Lexer();
        LexerController lexerController = new LexerController(lexer);
        Parser parser = new Parser(lexerController);
        ParserNode root = parser.run();
        ASTMaker ast = ASTMaker.getInstance(root);
        Root astRoot = ast.HandleRoot();
        Generator generator = Generator.getInstance(ASTMaker.getRoot(), astRoot);
        generator.run();
        System.out.println(ExceptionLog.getInstance());
    }

    public static void main(String[] args) throws FileNotFoundException, ParserError, IrError {
        PrintStream ps = new PrintStream("llvm_ir.txt");
        System.setOut(ps);
        IrTest();
        ps.close();
    }
}
