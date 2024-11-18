package AST;

import java.util.ArrayList;

// FuncType → 'void' | 'int'
public class FuncType extends Node{
    public FuncType(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }

    public String getFuncType(){
        String ret = ((TokenNode)children.get(0)).getToken().getTokenname();
        return ret;
    }

}
