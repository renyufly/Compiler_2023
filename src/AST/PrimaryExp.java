package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// PrimaryExp → '(' Exp ')' | LVal | Number
public class PrimaryExp extends Node{
    public PrimaryExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {
        int ret = 0;
        if(children.get(0) instanceof Number_AST) {
            ret = children.get(0).computeExpValue(symbolManager);
        } else if(children.get(0) instanceof LVal) {
            ret = children.get(0).computeExpValue(symbolManager);
        } else {
            ret = children.get(1).computeExpValue(symbolManager);
        }

        return ret;
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
        if(children.get(0) instanceof LVal) {
            return ((LVal)children.get(0)).genValue(printer, symbolManager);
        } else if(children.get(0) instanceof Number_AST) {
            return children.get(0).genValue(printer, symbolManager);
        } else {        // 属于 '(' Exp ')'
            return children.get(1).genValue(printer, symbolManager);
        }

    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager, int arrayDim) {
        if(children.get(0) instanceof LVal) {
            return ((LVal)children.get(0)).genRParamValue(printer, symbolManager, arrayDim);
        } else if(children.get(0) instanceof Number_AST) {
            return children.get(0).genRParamValue(printer, symbolManager, arrayDim);
        } else {        // 属于 '(' Exp ')'
            return children.get(1).genRParamValue(printer, symbolManager, arrayDim);
        }

    }

}
