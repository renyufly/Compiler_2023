package AST;

import LEXER.LexType;
import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;
import java.util.regex.Pattern;

// LVal → Ident {'[' Exp ']'}
public class LVal extends Node{

    public LVal(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);

    }


    public boolean isConst(SymbolManager symbolManager) {     // 检查是否是常量
        String ident = ((TokenNode)children.get(0)).getToken().getTokenname();
        Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
        if(symbol == null) {
            return false;
        }
        if(symbol.isConstVar()) {
            return true;
        }

        return false;

    }


    @Override
    public int computeExpValue(SymbolManager symbolManager) {
        String ident = ((TokenNode)children.get(0)).getToken().getTokenname();
        Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
        if(symbol == null) {
            return 0;
        }

        // 对于数组形式-计算Exp对应值
        int i=0;
        int[] dim = new int[2];   // 对应数组形式的一维/二维的维数
        for(Node child: children) {
            if(child instanceof Exp) {
                dim[i] = child.computeExpValue(symbolManager);
                i = i+1;
            }
        }

        if(symbol.getDim() == 0) {
            return symbol.getValue();
        } else if(symbol.getDim() == 1) {     // a[1]
            return symbol.getValue(dim[0]);
        }
        return symbol.getValue(dim[0], dim[1]);

    }



    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        // 使用了未定义的标识符
        String tokenname = ((TokenNode)children.get(0)).getToken().getTokenname();
        Symbol ident = symbolManager.getSymbolFromCurTable(tokenname);
        if(ident == null) {
            printer.addErrorMessage(((TokenNode) children.get(0)).getEndLine(), ErrorType.c);
        }

        super.checkError(printer, symbolManager);
    }


    @Override
    public int getDim(SymbolManager symbolManager) {
        String tokenname = ((TokenNode)children.get(0)).getToken().getTokenname();
        Symbol ident = symbolManager.getSymbolFromCurTable(tokenname);
        if(ident == null) {
            return -1;
        }

        int dim = 0;
        dim = ident.getDim();       // 符号原本的维数

        int cnt = 0;
        for(Node child: children) {
            if(child instanceof TokenNode && ((TokenNode) child).getToken().getLexType().equals(LexType.LBRACK.name())) {
                cnt++;
            }
        }

        return dim - cnt;       // 实参情况下的维数： a[1][2] --> fun(int b) 对应0维
    }


    @Override
    public String genValue(Printer printer, SymbolManager symbolManager) {    //  LVal → Ident {'[' Exp ']'}
        int array_dim = 0;
        String[] dim = new String[2];   // 记录对应数组变量的一维/二维的维数
        for(Node child: children) {
            if(child instanceof Exp) {
                dim[array_dim] = child.genValue(printer, symbolManager);   //
                array_dim++;
            }
        }


        String ident = ((TokenNode)children.get(0)).getToken().getTokenname();

        if(array_dim == 0) {       // 普通变量 a
            if(isConst(symbolManager)) {     // 是常量就直接返回数值
                Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                int value = symbol.getValue();
                return String.valueOf(value);
            } else {
                return ident;
            }


        } else if(array_dim == 1) {  // 一维数组 a[5]
            if(isConst(symbolManager)) {     // 是const-数组
                Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isNumber =  pattern.matcher(dim[0]).matches();
                if(isNumber) {     // 还要看维度表示是否是常数
                    int value = symbol.getValue(Integer.parseInt(dim[0]));
                    return String.valueOf(value);
                } else {
                    Temp temp = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.GETARR, temp.toString(), ident, dim[0]));
                    return temp.toString();
                }
            } else {
                Temp temp = new Temp();
                printer.addMidCodeList(new Midcode(Operation.GETARR, temp.toString(), ident, dim[0]));
                return temp.toString();
            }


        } else {                 // 二维数组 a[3][4]
            if(isConst(symbolManager)) {    // 是const-数组
                Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isNumber_dim0 =  pattern.matcher(dim[0]).matches();
                boolean isNumber_dim1 =  pattern.matcher(dim[1]).matches();
                if(isNumber_dim0 && isNumber_dim1) {    // 维度表示是常数字
                    int value = symbol.getValue(Integer.parseInt(dim[0]), Integer.parseInt(dim[1]));
                    return String.valueOf(value);
                } else {
                    Temp temp1 = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp1.toString(), dim[0], String.valueOf(symbol.getDim2())));
                    Temp temp2 = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp2.toString(), temp1.toString(), dim[1]));
                    Temp temp3 = new Temp();
                    //String ret = ident + "[" + temp2.toString() + "]";
                    printer.addMidCodeList(new Midcode(Operation.GETARR, temp3.toString(), ident, temp2.toString()));
                    return temp3.toString();      // 返回寄存器
                }


            } else {
                Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                Temp temp1 = new Temp();
                printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp1.toString(), dim[0], String.valueOf(symbol.getDim2())));
                Temp temp2 = new Temp();
                printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp2.toString(), temp1.toString(), dim[1]));
                Temp temp3 = new Temp();
                //String ret = ident + "[" + temp2.toString() + "]";
                printer.addMidCodeList(new Midcode(Operation.GETARR, temp3.toString(), ident, temp2.toString()));
                return temp3.toString();      // 返回寄存器
            }

        }


    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager,  int arrayDim) {   // LVal → Ident {'[' Exp ']'}
        int array_dim = 0;
        String[] dim = new String[2];   // 记录对应数组变量的一维/二维的维数
        for(Node child: children) {
            if(child instanceof Exp) {
                dim[array_dim] = child.genValue(printer, symbolManager);   //
                array_dim++;
            }
        }

        String ident = ((TokenNode)children.get(0)).getToken().getTokenname();

        if(array_dim == 0) {       // 普通变量 a
            if(arrayDim == 0) {   // 函数形参要求的是变量，0维地址
                if(isConst(symbolManager)) {    // 是常量则直接返回值
                    Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                    int value = symbol.getValue();
                    return String.valueOf(value);
                }
            }
            return ident;

        } else if(array_dim == 1) {  // 一维数组
            if(arrayDim == 1) {      // 形参要求int b[], 输入a[1]   【int a[2][2] = {{1,2}, {3,4}}】
                Symbol id_symbol = symbolManager.getSymbolFromCurTable(ident);
                int id_dim2 = id_symbol.getDim2();
                return ident + "[" + dim[0] + "]" + "[" + id_dim2 + "]";   // 返回 a[1][2] 字符串

            } else if(arrayDim == 0) {     // 形参要求int b,  输入b[1]   【int b[2] = {1,2}】
                Temp temp = new Temp();
                // String ret = ident + "[" + dim[0] + "]";
                printer.addMidCodeList(new Midcode(Operation.GETARR, temp.toString(), ident, dim[0]));
                return temp.toString();     // 返回寄存器
            }


        } else {                 // 二维数组 a[3][4]
            Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
            Temp temp1 = new Temp();
            printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp1.toString(), dim[0], String.valueOf(symbol.getDim2())));
            Temp temp2 = new Temp();
            printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp2.toString(), temp1.toString(), dim[1]));
            Temp temp3 = new Temp();
            //String ret = ident + "[" + temp2.toString() + "]";
            printer.addMidCodeList(new Midcode(Operation.GETARR, temp3.toString(), ident, temp2.toString()));
            return temp3.toString();      // 返回寄存器

        }

        return null;
    }






}
