package AST;

import MidCode.Midcode;
import MidCode.Operation;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// 语句块 Block → '{' { BlockItem } '}'      【BlockItem → ConstDecl | VarDecl | Stmt】
public class Block extends Node{
    private static int orderNum = 0;
    private int curOrder;
    public Block(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    public static int getBlockOrderNum() {      // Block的序号(从1开始)
        orderNum++;
        return orderNum;
    }

    // Block分为在函数定义里面的带return的，以及不带return的。
    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {
        orderNum++;
        curOrder = orderNum;
        printer.addMidCodeList(new Midcode(Operation.LABEL, String.valueOf(curOrder), "begin"));
        for(Node child: children) {
            child.genMidCode(printer, symbolManager);
        }

        printer.addMidCodeList(new Midcode(Operation.LABEL, String.valueOf(curOrder), "end"));

    }

    public void genReturnBlockMidCode(Printer printer, SymbolManager symbolManager, int order) {      // 带return的在函数定义里的
        curOrder = order;
        for(Node child: children) {
            child.genMidCode(printer, symbolManager);
        }

        printer.addMidCodeList(new Midcode(Operation.RETURN, null));    //
        printer.addMidCodeList(new Midcode(Operation.LABEL, String.valueOf(curOrder), "end"));


    }



}
