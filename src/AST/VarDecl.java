package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// VarDecl → BType VarDef { ',' VarDef } ';'
public class VarDecl extends Node{

    public VarDecl( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        super.checkError(printer, symbolManager);
    }





}
