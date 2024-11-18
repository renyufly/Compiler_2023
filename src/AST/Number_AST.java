package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// Number → IntConst
public class Number_AST extends Node{
    public Number_AST(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {
        int ret = ((TokenNode)children.get(0)).getToken().getValue();
        return ret;
    }

    @Override
    public int getDim(SymbolManager symbolManager) {
        return 0;
    }

    @Override
    public String genValue(Printer printer, SymbolManager symbolManager) {
        return ((TokenNode)children.get(0)).getToken().getTokenname();
    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager,  int arrayDim) {
        return ((TokenNode)children.get(0)).getToken().getTokenname();
    }


}
