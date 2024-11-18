package AST;

import MidCode.Midcode;
import MidCode.Operation;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

// MainFuncDef → 'int' 'main' '(' ')' Block
public class MainFuncDef extends Node{
    private Symbol symbol;
    public MainFuncDef( String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }

    public void createSymbol(SymbolManager symbolManager) {
        symbolManager.setIsGlobalDecl(false);
        // 创建main函数的符号
        String tokenname = ((TokenNode)children.get(1)).getToken().getTokenname();
        SymbolType return_type = SymbolType.INT;
        this.symbol = new Symbol(tokenname, return_type);

        //
        symbolManager.setCurFunc(symbol);
        symbolManager.pushSymbolTable();

    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        createSymbol(symbolManager);

        for(Node child: children) {
            if(child instanceof Block) {
                // 补充信息
                ArrayList<SymbolType> fParamType = new ArrayList<>();
                ArrayList<Integer> fParamDim = new ArrayList<>();

                this.symbol.completeFuncInfo(fParamType, fParamDim);
            }
            child.checkError(printer, symbolManager);
        }


        //
        symbolManager.setCurFunc(null);
        symbolManager.popSymbolTable();

        // 检查函数末尾是否存在return语句
        Node block = children.get(children.size()-1);   // Block → '{' { BlockItem } '}' // BlockItem → ConstDecl | VarDecl | Stmt
        Node lastnode = block.getChildren().get(block.getChildren().size()-2);   // '}'的前一个符
        if(symbol.getReturnType() != SymbolType.VOID) {
            if(!(lastnode instanceof Stmt) || ((lastnode instanceof Stmt) && !(((Stmt) lastnode).isReturnStmt())) ) {
                printer.addErrorMessage(end_line, ErrorType.g);
            }
        }

    }

    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {
        createSymbol(symbolManager);    // 符号表


        int order = Block.getBlockOrderNum();

        printer.addMidCodeList(new Midcode(Operation.LABEL, String.valueOf(order), "begin"));  // 生成带序号的block的

        printer.addMidCodeList(new Midcode(Operation.MAIN_BEGIN, "main"));     // 生成main()主函数

        for(Node child: children) {
            if(child instanceof Block) {
                //  符号表补充信息
                ArrayList<SymbolType> fParamType = new ArrayList<>();
                ArrayList<Integer> fParamDim = new ArrayList<>();
                this.symbol.completeFuncInfo(fParamType, fParamDim);

                ((Block) child).genReturnBlockMidCode(printer, symbolManager, order);    //
            }
        }

        printer.addMidCodeList(new Midcode(Operation.MAIN_END, "main"));

        //
        symbolManager.setCurFunc(null);
        symbolManager.popSymbolTable();

    }


}
