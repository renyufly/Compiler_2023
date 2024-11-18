package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// UnaryOp → '+' | '−' | '!'
public class UnaryOp extends Node{
    public UnaryOp( String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        super.checkError(printer, symbolManager);
    }


    public String getUnaryOp() {
        TokenNode tokenNode = (TokenNode) children.get(0);
        return tokenNode.getToken().getTokenname();

    }


}
