package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// Exp → AddExp
public class Exp extends Node{
    private int expRet;
    public Exp( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {
        int ret = children.get(0).computeExpValue(symbolManager);

        expRet = ret;
        return ret;
    }

    public int getExpRet() {
        return this.expRet;
    }

    @Override
    public int getDim(SymbolManager symbolManager) {
        for(Node child: children) {
            if(child.getDim(symbolManager) != -1) {
                return child.getDim(symbolManager);
            }
        }
        return -1;
    }

    @Override
    public String genValue(Printer printer, SymbolManager symbolManager) {
        for(Node child: children) {
            if(child instanceof AddExp) {
                return child.genValue(printer, symbolManager);
            }
        }
        return null;
    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager, int arrayDim) {     // 用于获取函数实参
        for(Node child: children) {
            if(child instanceof AddExp) {
                return child.genRParamValue(printer, symbolManager, arrayDim);
            }
        }
        return null;
    }




}
