package AST;

import MidCode.Midcode;
import MidCode.Operation;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

// FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
public class FuncDef extends Node{
    private Symbol symbol;

    public FuncDef( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);

    }


    public void createSymbol(SymbolManager symbolManager) {
        symbolManager.setIsGlobalDecl(false);    // 不再全局定义

        String tokenname = ((TokenNode)children.get(1)).getToken().getTokenname();
        SymbolType return_type = null;
        if(((FuncType)children.get(0)).getFuncType().equals("void")) {
            return_type = SymbolType.VOID;
        } else {
            return_type = SymbolType.INT;
        }

        this.symbol = new Symbol(tokenname, return_type);
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        createSymbol(symbolManager);

        // 检查是否函数名或者变量名在当前作用域下重复定义
        boolean ans = symbolManager.addSymbol(this.symbol);
        if(!ans) {
            printer.addErrorMessage(children.get(1).getEndLine(), ErrorType.b);
        }

        //
        symbolManager.setCurFunc(symbol);
        symbolManager.pushSymbolTable();      //

        // 检查形参error与block的error [函数的剩余信息需在进入block检查前就补充完]
        for(Node child: children) {
            if(child instanceof Block) {
               // 补充信息
                ArrayList<SymbolType> fParamType = new ArrayList<>();
                ArrayList<Integer> fParamDim = new ArrayList<>();
                if(children.get(3) instanceof FuncFParams) {     // 遇到形参表
                    fParamType = ((FuncFParams) children.get(3)).getFParamsType();
                    fParamDim = ((FuncFParams) children.get(3)).getFParamsDim();
                }
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
    public void genMidCode(Printer printer, SymbolManager symbolManager) {    // FuncType Ident '(' [FuncFParams] ')' Block
        createSymbol(symbolManager);     // 创建符号
        boolean ans = symbolManager.addSymbol(this.symbol);
        symbolManager.setCurFunc(symbol);
        symbolManager.pushSymbolTable();     //


        int order = Block.getBlockOrderNum();

        printer.addMidCodeList(new Midcode(Operation.LABEL, String.valueOf(order), "begin"));  // 生成带序号的block

        // 定义的函数
        printer.addMidCodeList(new Midcode(Operation.FUNC, this.symbol.getToken(), this.symbol.getReturnTypeStr()));
        for(Node child: children) {
            if(child instanceof FuncFParams) {
                child.genMidCode(printer, symbolManager);
            } else if(child instanceof Block) {
                // 符号表补充信息
                ArrayList<SymbolType> fParamType = new ArrayList<>();
                ArrayList<Integer> fParamDim = new ArrayList<>();
                if(children.get(3) instanceof FuncFParams) {     // 遇到形参表
                    fParamType = ((FuncFParams) children.get(3)).getFParamsType();
                    fParamDim = ((FuncFParams) children.get(3)).getFParamsDim();
                }
                this.symbol.completeFuncInfo(fParamType, fParamDim);

                ((Block) child).genReturnBlockMidCode(printer, symbolManager, order);
            }
        }


        symbolManager.setCurFunc(null);
        symbolManager.popSymbolTable();

    }



}
