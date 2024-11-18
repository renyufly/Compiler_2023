package AST;

import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

// ForStmt → LVal '=' Exp
public class ForStmt extends Node{
    public ForStmt( String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        // <LVal>为常量时，不能对其修改
        LVal lVal = (LVal) children.get(0);
        if(lVal.isConst(symbolManager)) {
            printer.addErrorMessage(lVal.getEndLine(), ErrorType.h);
        }

        super.checkError(printer, symbolManager);

    }



    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {   // ForStmt → LVal '=' Exp
        String ident = ((TokenNode)children.get(0).getChildren().get(0)).getToken().getTokenname();
        String ret_val = children.get(2).genValue(printer, symbolManager);    // Exp
        LVal lVal = (LVal) children.get(0);

        if(lVal.getChildren().size() > 1) {   // 是数组变量
            int array_dim = 0;
            String[] dim = new String[2];   // 记录对应数组变量的一维/二维的维数
            for(Node child: lVal.getChildren()) {
                if(child instanceof Exp) {
                    dim[array_dim] = child.genValue(printer, symbolManager);   //
                    array_dim++;
                }
            }
            if(array_dim == 1) {  // 一维数组 a[3] = b;
                printer.addMidCodeList(new Midcode(Operation.SETARR, ident, dim[0], ret_val));
            } else {             // 二维数组 a[3][4] = 4;
                Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                Temp temp1 = new Temp();
                printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp1.toString(), dim[0], String.valueOf(symbol.getDim2())));
                Temp temp2 = new Temp();
                printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp2.toString(), temp1.toString(), dim[1]));

                printer.addMidCodeList(new Midcode(Operation.SETARR, ident, temp2.toString(), ret_val));
            }
        } else {   // 非数组变量
            printer.addMidCodeList(new Midcode(Operation.ASSIGN, ident, ret_val));
        }
    }



}
