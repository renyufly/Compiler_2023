package AST;

import SYMBOL.SymbolManager;

import java.util.ArrayList;

// ConstExp → AddExp
public class ConstExp extends Node{
    public ConstExp( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {
        return children.get(0).computeExpValue(symbolManager);
    }



}
