package AST;

import LEXER.LexType;
import MidCode.Midcode;
import MidCode.Operation;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

// 函数形参 FuncFParam → 'int' Ident ['[' ']' { '[' ConstExp ']' }]
public class FuncFParam extends Node{
    private Symbol symbol;
    public FuncFParam( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    public SymbolType getParamType() {
        return this.symbol.getTypeValue();
    }

    public int getParamDim() {
        return this.symbol.getDim();
    }


    public void createSymbol(SymbolManager symbolManager) {
        // 创建符号
        String tokenname = ((TokenNode)children.get(1)).getToken().getTokenname();
        SymbolType type_value = null;
        if(((TokenNode)children.get(0)).getToken().getLexType().equals(LexType.INTTK.name())) {
            type_value = SymbolType.INT;
        }
        int dim = 0;
        ArrayList<Integer> dim_len = new ArrayList<>();
        for(int i=0; i<children.size(); i++) {
            if(children.get(i) instanceof TokenNode && ((TokenNode)children.get(i)).getToken().getLexType().equals(LexType.LBRACK.name())) {
                dim++;
                if(children.get(i+1) instanceof ConstExp) {
                    dim_len.add(children.get(i+1).computeExpValue(symbolManager));
                } else {
                    dim_len.add(-1);   // 形参第一维是[]缺失
                }
            }
        }
        SymbolType type = null;
        if(dim == 0) {
            type = SymbolType.variable;
            dim_len.add(-1);
            dim_len.add(-1);    //
        } else if(dim == 1) {
            type = SymbolType.oneDarray;
            dim_len.add(-1);
        } else {
            type = SymbolType.twoDarray;
        }
        this.symbol = new Symbol(tokenname, type, SymbolType.notconstVar, type_value, dim_len.get(0), dim_len.get(1), null);
    }

    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        createSymbol(symbolManager);       // 创建符号

        super.checkError(printer, symbolManager);

        // 检查函数名或者变量名在当前作用域下重复定义
        boolean ans = symbolManager.addSymbol(symbol);     // 添加符号
        if(!ans) {
            printer.addErrorMessage(children.get(1).getEndLine(), ErrorType.b);
        }

    }


    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {   // FuncFParam → 'int' Ident ['[' ']' { '[' ConstExp ']' }]
        createSymbol(symbolManager);       // 创建符号
        symbolManager.addSymbol(symbol);   // 添加符号

        String ident = ((TokenNode)children.get(1)).getToken().getTokenname();

        if(this.symbol.getType() == SymbolType.variable) {
            printer.addMidCodeList(new Midcode(Operation.PARAM_DEF, ident, "0"));
        } else if(this.symbol.getType() == SymbolType.oneDarray) {
            printer.addMidCodeList(new Midcode(Operation.PARAM_DEF, ident, "1"));
        } else if(this.symbol.getType() == SymbolType.twoDarray) {
            printer.addMidCodeList(new Midcode(Operation.PARAM_DEF, ident, "2", String.valueOf(symbol.getDim2())));
        }



    }


}
