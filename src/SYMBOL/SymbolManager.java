package SYMBOL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class SymbolManager {
    private ArrayList<SymbolTable> symbolTables_list;   // 所有的符号表
    private HashMap<String, Stack<SymbolTable>> token_intdex_symbolTables;  // 单个token存在的嵌套下所有符号表（Empty就是未定义）
    private boolean is_global_decl;    //是否是全局/常量定义
    private Symbol cur_func;         // 当前解析的函数(名)
    private int cur_loop_depth;      // 循环体(while)的循环深度


    public SymbolManager() {
        this.symbolTables_list = new ArrayList<>();
        this.token_intdex_symbolTables = new HashMap<>();
        this.is_global_decl = false;
        this.cur_loop_depth = 0;
    }

    public void pushSymbolTable() {          // 添加一个符号表（在进入不同层作用域时进行）
        SymbolTable symbolTable = new SymbolTable();   // 第一个符号表的id与父id都是0
        this.symbolTables_list.add(symbolTable);      // 创建并添加一个符号表
    }

    public void popSymbolTable() {        // 离开当前作用域
        SymbolTable temp_table = this.symbolTables_list.get(symbolTables_list.size()-1);
        symbolTables_list.remove(symbolTables_list.size()-1);    // 删除一个符号表
        for(String tokenname: temp_table.getDirectory()) {
            this.token_intdex_symbolTables.get(tokenname).pop();    // 弹出并删除token存在的栈顶的符号表
        }

    }

    public void setIsGlobalDecl(boolean is_global_decl) {
        this.is_global_decl = is_global_decl;
    }

    public boolean getIsGlobalDecl() {
        return this.is_global_decl;
    }

    public Symbol getSymbolFromCurTable(String ident) {
        if(!this.token_intdex_symbolTables.containsKey(ident) || this.token_intdex_symbolTables.get(ident).isEmpty()) {
            return null;
        }
        SymbolTable tmp_table = this.token_intdex_symbolTables.get(ident).peek();
        Symbol symbol = tmp_table.getSymbol(ident);
        return symbol;     // 没有就是返回的null
    }

    public boolean addSymbol(Symbol symbol) {    // 向当前符号表添加一个符号（返回值检查是否重复）
        SymbolTable symbolTable = this.symbolTables_list.get(symbolTables_list.size()-1);
        if(symbolTable.getSymbol(symbol.getToken()) != null) {    // 函数名或者变量名在当前作用域下重复定义
            return false;
        }

        symbolTable.addSymbol(symbol);
        if(!this.token_intdex_symbolTables.containsKey(symbol.getToken())) {
            Stack<SymbolTable> symbolTableStack = new Stack<>();
            symbolTableStack.add(symbolTable);
            this.token_intdex_symbolTables.put(symbol.getToken(), symbolTableStack);

        } else {
            this.token_intdex_symbolTables.get(symbol.getToken()).add(symbolTable);

        }
        return true;
    }

    public void setCurFunc(Symbol symbol) {
        this.cur_func = symbol;
    }

    public Symbol getCurFunc() {
        return this.cur_func;
    }

    public int getCurLoopDepth() {
        return this.cur_loop_depth;
    }

    public void addLoopDepth() {
        this.cur_loop_depth++;
    }

    public void subLoopDepth() {
        this.cur_loop_depth--;
    }


    // Mips
    public boolean isGlobalSymbol(String ident) {    // 当前符号是否是全局变量
        if(!this.token_intdex_symbolTables.containsKey(ident) || this.token_intdex_symbolTables.get(ident).isEmpty()) {
            return false;
        }

        for(int i=symbolTables_list.size()-1; i>=0; i--) {
            if(symbolTables_list.get(i).isContainSymbol(ident)) {
                if(i == 0) {
                    return true;   // 当前符号只存在最外层全局层的符号表中
                } else {
                    return false;
                }
            }
        }

        return false;

    }

    public boolean isCurGlobalTable() {
        if(this.symbolTables_list.size() == 1) {
            return true;
        }
        return false;
    }


}
