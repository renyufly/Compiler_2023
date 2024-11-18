package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
public class ConstDecl extends Node{
    public ConstDecl(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {   // 继承的子类若不重写父类方法，执行时默认父类方法
        super.checkError(printer, symbolManager);                  // [该重写不必要，可删去]
    }


}
