package Ir.ClangAST.AST;

import Exceptions.IrError;

public interface LeafStmt {
    public Object getValue() throws IrError;
}
