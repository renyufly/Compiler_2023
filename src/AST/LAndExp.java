package AST;

import MidCode.Midcode;
import MidCode.Operation;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;


// 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp  ==>    EqExp {'&&' EqExp}
public class LAndExp extends Node{

    private int jump;
    public LAndExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }


    @Override
    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label) {
        jump_cnt++;
        this.jump = jump_cnt;

        for(Node child: children) {
            if(child instanceof EqExp) {
                String ret = child.genCondMidCode(printer, symbolManager, jump_label);
                printer.addMidCodeList(new Midcode(Operation.BZ, "Jump_" + this.jump, ret));  // (执行条件失败的语句)
            }

        }

        printer.addMidCodeList(new Midcode(Operation.GOTO, "Jump_" + jump_label));  // 无条件跳转(执行条件成功的语句)

        printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Jump_" + this.jump));  // 设置Jump标签

        return null;
    }





}
