package AST;

import SYMBOL.SymbolManager;

import java.util.ArrayList;

// ConstInitVal → ConstExp  | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
public class ConstInitVal extends Node{
    public ConstInitVal( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }


    public ArrayList<Integer> computeValue(int dim, SymbolManager symbolManager) {    // 计算值
        ArrayList<Integer> ret = new ArrayList<>();
        if(dim == 0) {
            ret.add(((ConstExp)children.get(0)).computeExpValue(symbolManager));
        } else {       // 数组形式的赋值
            for(Node child: children) {
                if(child instanceof ConstInitVal) {
                    ArrayList<Integer> tmp = ((ConstInitVal) child).computeValue(dim-1, symbolManager);
                    ret.addAll(tmp);
                }
            }
        }

        return ret;
    }



}
