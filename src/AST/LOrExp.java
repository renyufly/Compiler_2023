package AST;

import MidCode.Midcode;
import MidCode.Operation;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

//  逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp   =>   LAndExp {'||' LAndExp}
public class LOrExp extends Node{

    private int jump;

    public LOrExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }



    @Override
    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label) {
        jump_cnt++;
        this.jump = jump_cnt;

        for(Node child: children) {
            if(child instanceof LAndExp) {
                ((LAndExp) child).genCondMidCode(printer, symbolManager, this.jump);
            }
        }

        printer.addMidCodeList(new Midcode(Operation.GOTO, "Jump_" + jump_label));

        printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Jump_" + this.jump));


        return null;
    }


    @Override
    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label, boolean is_for) {
        jump_cnt++;
        this.jump = jump_cnt;

        for(Node child: children) {
            if(child instanceof LAndExp) {
                ((LAndExp) child).genCondMidCode(printer, symbolManager, this.jump);
            }
        }

        printer.addMidCodeList(new Midcode(Operation.GOTO, "Loop_" + jump_label + "_end"));    //

        printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Jump_" + this.jump));


        return null;
    }




}
