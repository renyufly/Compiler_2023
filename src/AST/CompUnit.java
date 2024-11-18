package AST;


import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// CompUnit  ->  {ConstDecl | VarDecl} {FuncDef} MainFunDef
public class CompUnit extends Node {

    public CompUnit(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        symbolManager.setIsGlobalDecl(true);
        symbolManager.pushSymbolTable();      // 当前作用域
        super.checkError(printer, symbolManager);    // 遍历子树
        symbolManager.popSymbolTable();
    }

    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {
        symbolManager.setIsGlobalDecl(true);
        symbolManager.pushSymbolTable();      // 当前作用域
        if(children.size() == 0) {
            return;
        } else {
            for(Node child:children) {
                child.genMidCode(printer, symbolManager);
            }
        }

        symbolManager.popSymbolTable();
    }

}
