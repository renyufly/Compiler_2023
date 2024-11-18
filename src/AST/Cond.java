package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// 条件表达式 Cond → LOrExp
public class Cond extends Node{
    public Cond(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }



    @Override
    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label) {           // 生成中间代码
        for(Node child: children) {
            if(child instanceof LOrExp) {
                ((LOrExp) child).genCondMidCode(printer, symbolManager, jump_label);
            }
        }

        return null;
    }


    @Override
    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label, boolean is_for) {  // 针对for中条件表达式
        for(Node child: children) {
            if(child instanceof LOrExp) {
                ((LOrExp) child).genCondMidCode(printer, symbolManager, jump_label, is_for);
            }
        }

        return null;
    }




}
