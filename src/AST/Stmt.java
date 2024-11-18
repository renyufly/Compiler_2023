package AST;

import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import PARSER.SyntaxType;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

// Stmt → LVal '=' Exp ';'  | [Exp] ';'  | Block   | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
//        | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt   | 'break' ';' | 'continue' ';'
//        | 'return' [Exp] ';'   | LVal '=' 'getint''('')'';'   | 'printf''('FormatString{','Exp}')'';'
public class Stmt extends Node{
    private boolean is_lval_exp;     // 是否是LVal '=' Exp ';'
    private boolean is_exp;          // 是否是 [Exp] ';'
    private boolean is_block;        // Block
    private boolean is_if_stmt;      // if
    private boolean is_for_stmt;     // for
    private boolean is_break;        // 'break' ';'
    private boolean is_continue;      // 'continue' ';'
    private boolean is_return_stmt;   // 是否是'return' [Exp] ';'   [是return-true]
    private boolean is_getint;      // 是否是LVal '=' 'getint''('')'';'
    private boolean is_printf;      // 'printf''('FormatString{','Exp}')'';'


    private int jump1_label = 0;
    private int jump2_label = 0;

    private int jump_for_label = 0;

    public Stmt(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
        this.is_break = false;
        this.is_continue = false;
        this.is_return_stmt = false;
        this.is_getint = false;
        this.is_printf = false;
        this.is_lval_exp = false;
        this.is_block = false;
        this.is_for_stmt = false;
        this.is_if_stmt = false;
    }

    public void setIs_lval_exp(boolean is_lval_exp) {
        this.is_lval_exp = is_lval_exp;
    }
    public void setIs_exp(boolean is_exp) {
        this.is_exp = is_exp;
    }

    public void setIs_break(boolean is_break) {
        this.is_break = is_break;
    }

    public void setIs_continue(boolean is_continue) {
        this.is_continue = is_continue;
    }

    public void setIs_return_stmt(boolean is_return_stmt) {
        this.is_return_stmt = is_return_stmt;
    }

    public void setIs_getint(boolean is_getint) {
        this.is_getint = is_getint;
    }

    public void setIs_printf(boolean is_printf) {
        this.is_printf = is_printf;
    }

    public void setIs_block(boolean is_block) {
        this.is_block = is_block;
    }

    public void setIs_if_stmt(boolean is_if_stmt) {
        this.is_if_stmt = is_if_stmt;
    }
    public void setIs_for_stmt(boolean is_for_stmt) {
        this.is_for_stmt = is_for_stmt;
    }

    public boolean isReturnStmt() {
        return this.is_return_stmt;
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        if(this.is_lval_exp) {     // LVal '=' Exp ';'
            LVal lVal = (LVal) children.get(0);
            // <LVal>为常量时，不能对其修改
            if(lVal.isConst(symbolManager)) {
                printer.addErrorMessage(lVal.getEndLine(), ErrorType.h);
            }
            super.checkError(printer, symbolManager);
        } else if((this.is_break) || (this.is_continue)) {     //  Stmt -> 'break' ';' | 'continue' ';'
            // 在非循环块中使用break和continue语句
            if(symbolManager.getCurLoopDepth() <= 0) {
                printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.m);
            }
            super.checkError(printer, symbolManager);
        } else if(this.is_return_stmt) {                    // 'return' [Exp] ';'
            // 无返回值的函数存在不匹配的return语句
            if(children.size() > 2 && children.get(1) instanceof Exp) {
                Symbol cur_func = symbolManager.getCurFunc();
                if(cur_func.getReturnType() == SymbolType.VOID) {
                    printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.f);
                }
            }
            super.checkError(printer, symbolManager);
        } else if(this.is_getint) {                       // LVal '=' 'getint''('')'';'
            // <LVal>为常量时，不能对其修改
            LVal lVal = (LVal) children.get(0);
            if(lVal.isConst(symbolManager)) {
                printer.addErrorMessage(lVal.getEndLine(), ErrorType.h);
            }
            super.checkError(printer, symbolManager);
        } else if(this.is_printf) {                      //  'printf''('FormatString{,Exp}')'';'
            // printf中格式字符与表达式个数不匹配
            int cnt = ((TokenNode)children.get(2)).getFormatCharCnt();
            int exp_cnt = 0;
            for(Node child: children) {
                if(child instanceof Exp) {
                    exp_cnt++;
                }
            }
            if(exp_cnt != cnt) {
                printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.l);
            }

            super.checkError(printer, symbolManager);
        } else if(this.is_block) {
            symbolManager.pushSymbolTable();     // 符号表
            super.checkError(printer, symbolManager);
            symbolManager.popSymbolTable();
        } else if(this.is_for_stmt) {
            symbolManager.addLoopDepth();      // 符号表
            symbolManager.pushSymbolTable();
            super.checkError(printer, symbolManager);
            symbolManager.popSymbolTable();
            symbolManager.subLoopDepth();
        } else {
            super.checkError(printer, symbolManager);
        }


    }


    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {
        if(is_lval_exp) {         // 是 赋值语句 LVal '=' Exp ';'
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

        } else if(is_block) {     // 是 Block
            symbolManager.pushSymbolTable();   // 添加符号表
            for(Node child: children) {
                child.genMidCode(printer, symbolManager);
            }
            symbolManager.popSymbolTable();    // 删除符号表
        } else if(is_return_stmt) {     // 是return[Exp]; 语句
            if(children.size() > 2) {    // 有Exp
                String ret = children.get(1).genValue(printer, symbolManager);
                printer.addMidCodeList(new Midcode(Operation.RETURN, ret));
            } else {
                printer.addMidCodeList(new Midcode(Operation.RETURN, null));
            }
        } else if(is_printf) {    // 是printf语句    'printf' '(' FormatString {','Exp} ')' ';'
            String formatStr = ((TokenNode)children.get(2)).getToken().getTokenname();   // 获得打印的字符串
            int first_str = 1;
            ArrayList<String> subFormatStr = new ArrayList<>();
            ArrayList<Exp> expList = new ArrayList<>();    // 存Exp
            for(Node child: children) {
                if(child instanceof Exp) {
                    expList.add((Exp) child);
                }
            }

            for(int i = 1; i<formatStr.length()-1; i++) {   // 处理分割字符串（略去首尾的引号）
                if(i < formatStr.length()-2 && formatStr.charAt(i) == '%' && formatStr.charAt(i+1) == 'd') {
                    // 以"%d"为分割
                    if(first_str != i) {
                        subFormatStr.add(formatStr.substring(first_str, i));     // 不包括末尾
                    }
                    subFormatStr.add("%d");

                    first_str = i+2;
                    i++;   // 略去%d

                }
            }
            if(first_str != formatStr.length()-1) {
                subFormatStr.add(formatStr.substring(first_str, formatStr.length()-1));
            }

            int digit_cnt = 0;
            //

            ArrayList<String> digitArr = new ArrayList<>();
            //  先把printf里面的参数求一下
            for(String str: subFormatStr) {
                if(str.equals("%d")) {
                    String ret = expList.get(digit_cnt).genValue(printer, symbolManager);
                    digit_cnt++;
                    digitArr.add(ret);
                }
            }
            digit_cnt = 0;
            for(String str: subFormatStr) {
                if(!str.equals("%d")) {     // 正常字符串

                    printer.addMidCodeList(new Midcode(Operation.PRINT, str, "str"));

                    printer.addMipsString(str);    //
                } else {        // 要打印%d
                    /*
                    String ret = expList.get(digit_cnt).genValue(printer, symbolManager);
                    digit_cnt++;
                    */
                    String ret = digitArr.get(digit_cnt);
                    digit_cnt++;

                    printer.addMidCodeList(new Midcode(Operation.PRINT, ret, "digit"));
                }

            }

        } else if(is_getint) {    // LVal '=' 'getint''('')'';'   【LVal → Ident {'[' Exp ']'}】
            String ident = ((TokenNode)children.get(0).getChildren().get(0)).getToken().getTokenname();
            LVal lVal = (LVal) children.get(0);
            if(lVal.getChildren().size() > 1) {   // 是数组变量
                Temp temp = new Temp();
                printer.addMidCodeList(new Midcode(Operation.GETINT, temp.toString()));   //

                int array_dim = 0;
                String[] dim = new String[2];   // 记录对应数组变量的一维/二维的维数
                for(Node child: lVal.getChildren()) {
                    if(child instanceof Exp) {
                        dim[array_dim] = child.genValue(printer, symbolManager);   //
                        array_dim++;
                    }
                }

                if(array_dim == 1) {  // 一维数组 a[3] = getint();
                    printer.addMidCodeList(new Midcode(Operation.SETARR, ident, dim[0], temp.toString()));

                } else {             // 二维数组 a[3][4]
                    Symbol symbol = symbolManager.getSymbolFromCurTable(ident);
                    Temp temp1 = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp1.toString(), dim[0], String.valueOf(symbol.getDim2())));
                    Temp temp2 = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp2.toString(), temp1.toString(), dim[1]));

                    printer.addMidCodeList(new Midcode(Operation.SETARR, ident, temp2.toString(), temp.toString()));

                }

            } else {   // 非数组变量
                printer.addMidCodeList(new Midcode(Operation.GETINT, ident));
            }

        } else if(is_exp) {   // 是 [Exp] ';'    如果是调用函数则会生成对应中间代码
            if(children.size() == 0) {
                return;
            } else {
                for(Node child:children) {
                    child.genMidCode(printer, symbolManager);
                }
            }
        } else if(is_if_stmt) {      // 是 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            boolean is_has_else = false;
            Cond cond = null;
            Stmt stmt1 = null;
            Stmt stmt2 = null;
            for(Node child: children) {
                if(child instanceof Cond) {
                    cond = (Cond) child;     //

                }

                if(child instanceof Stmt ) {
                    if(!is_has_else) {
                        stmt1 = (Stmt) child;
                    } else {
                        stmt2 = (Stmt) child;
                    }

                }

                if(child instanceof TokenNode && ((TokenNode) child).getToken().getTokenname().equals("else") ) {
                    is_has_else = true;
                }

            }

            if(is_has_else) {     // if语句有else
                jump_cnt++;
                this.jump1_label = jump_cnt;
                jump_cnt++;
                this.jump2_label = jump_cnt;

                cond.genCondMidCode(printer, symbolManager, jump1_label);

                stmt1.genMidCode(printer, symbolManager);

                printer.addMidCodeList(new Midcode(Operation.GOTO, "Jump_" + this.jump2_label));
                printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Jump_" + this.jump1_label));

                stmt2.genMidCode(printer, symbolManager);

                printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Jump_" + this.jump2_label));

            } else {            // if语句 无 else
                jump_cnt++;
                this.jump1_label = jump_cnt;
                cond.genCondMidCode(printer, symbolManager, jump1_label);

                stmt1.genMidCode(printer, symbolManager);

                printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Jump_" + this.jump1_label));

            }


        } else if(is_for_stmt) {     // 是 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
            symbolManager.addLoopDepth();
            symbolManager.pushSymbolTable();  // 符号表

            jump_cnt++;
            this.jump_for_label = jump_cnt;
            loop_stack.push(this.jump_for_label);     //

            ForStmt forStmt1 = null;
            ForStmt forStmt2 = null;
            Cond cond_for = null;
            Stmt stmt_for = null;
            int cnt_for = 0;
            for(Node child: children) {
                if(child instanceof ForStmt) {
                    if(cnt_for == 0) {
                        forStmt1 = (ForStmt) child;
                        cnt_for++;
                    } else {
                        forStmt2 = (ForStmt) child;
                        cnt_for++;
                    }
                } else if(child instanceof Cond) {
                    cond_for = (Cond) child;
                    cnt_for++;
                } else if(child instanceof Stmt) {
                    stmt_for = (Stmt) child;
                } else if(child instanceof TokenNode && ((TokenNode) child).getToken().getTokenname().equals(";")) {
                    cnt_for++;
                }
            }

            if(forStmt2 != null) {
                loop_forstmt_map.put(this.jump_for_label, forStmt2);   //
            }


            if(forStmt1 != null) {   // 存在第一个for内赋值
                forStmt1.genMidCode(printer, symbolManager);
            }

            printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Loop_" + this.jump_for_label + "_begin")); //

            if(cond_for != null) {
                cond_for.genCondMidCode(printer, symbolManager, this.jump_for_label, true);
            }

            if(stmt_for != null) {
                stmt_for.genMidCode(printer, symbolManager);
            }

            if(forStmt2 != null) {      //
                forStmt2.genMidCode(printer, symbolManager);
            }

            printer.addMidCodeList(new Midcode(Operation.GOTO, "Loop_" + this.jump_for_label + "_begin"));

            printer.addMidCodeList(new Midcode(Operation.JUMP_LABEL, "Loop_" + this.jump_for_label + "_end"));

            loop_forstmt_map.remove(this.jump_for_label);
            loop_stack.pop();   //

            symbolManager.popSymbolTable();   // 符号表
            symbolManager.subLoopDepth();

        } else if(is_break) {        // 'break' ';'
            int loop_order = loop_stack.peek();    // 返回栈顶的元素但不移除
            printer.addMidCodeList(new Midcode(Operation.GOTO, "Loop_" + loop_order + "_end"));   // 跳转到Loop的结尾

        } else if(is_continue) {     //  'continue' ';'
            int loop_order = loop_stack.peek();
            if(loop_forstmt_map.get(loop_order) != null) {    // continue前需执行for的第二个赋值语句
                loop_forstmt_map.get(loop_order).genMidCode(printer, symbolManager);
            }

            printer.addMidCodeList(new Midcode(Operation.GOTO, "Loop_" + loop_order + "_begin"));  // 跳转到Loop的begin

        }



    }








}
