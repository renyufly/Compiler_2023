package AST;

import LEXER.LexType;
import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;
import java.util.regex.Pattern;

// UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp    【UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中】
public class UnaryExp extends Node{
    public UnaryExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {    // 计算表达式的值
        int ret = 0;
        if(children.get(0) instanceof UnaryOp) {
            // UnaryOp → '+' | '−' | '!' 注：'!'仅出现在条件表达式中
            TokenNode tokenNode = (TokenNode) children.get(0).children.get(0);
            if(tokenNode.getToken().getLexType().equals(LexType.PLUS.name())) {
                ret = ret + children.get(1).computeExpValue(symbolManager);
            } else if(tokenNode.getToken().getLexType().equals(LexType.MINU.name())) {
                ret = ret - children.get(1).computeExpValue(symbolManager);
            } else {   // 逻辑运算（!、&&、||），非 0 表示真、0 表示假
                       // 关系运算或逻辑运算的结果用 1 表示真、0 表示假。
                ret = (children.get(1).computeExpValue(symbolManager) == 0) ? 1 : 0;      // '!'对Exp取反
            }
        } else if(children.get(0) instanceof PrimaryExp) {
            ret = children.get(0).computeExpValue(symbolManager);

        }

        return ret;
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        if(children.get(0) instanceof TokenNode) {              // Ident '(' [FuncRParams] ')'   -调用函数

            String tokenname = ((TokenNode)children.get(0)).getToken().getTokenname();
            Symbol func_ident = symbolManager.getSymbolFromCurTable(tokenname);

            // 使用了未定义的标识符
            if(func_ident == null) {
                printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.c);
                super.checkError(printer, symbolManager);
                return;
            }
            // 函数调用语句中，参数个数与函数定义中的参数个数不匹配
            int f_params_cnt = func_ident.getFuncParamsCnt();
            int r_params_cnt = 0;

            ArrayList<Integer> r_params_dims = new ArrayList<>();

            if(children.size() > 2 && children.get(2) instanceof FuncRParams) {   // 忽略掉缺右括号的情况
                r_params_cnt = ((FuncRParams)children.get(2)).getFuncRparamsCnt();
                r_params_dims = ((FuncRParams)children.get(2)).getRparamsDim(symbolManager);
            }
            if(f_params_cnt != r_params_cnt) {
                printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.d);
                super.checkError(printer, symbolManager);
                return;
            }
            // 函数调用语句中，参数类型与函数定义中对应位置的参数类型不匹配(类型一致，是指地址的维数要保持一致。)
            if(children.size() > 2 && children.get(2) instanceof FuncRParams) {
                ArrayList<Integer> f_params_dims = func_ident.getParamsDim();

                for(int i=0; i<f_params_dims.size(); i++) {
                    if(!f_params_dims.get(i).equals(r_params_dims.get(i))) {     // Integer类型判等不要用==，用equals
                        printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.e);
                        super.checkError(printer, symbolManager);
                        return;
                    }
                }
            }



            // super.checkError(printer, symbolManager);
        } else {
            super.checkError(printer, symbolManager);
        }

    }


    @Override
    public int getDim(SymbolManager symbolManager) {
        if(children.get(0) instanceof TokenNode) {
            String ident = ((TokenNode)children.get(0)).getToken().getTokenname();
            Symbol func_symbol = symbolManager.getSymbolFromCurTable(ident);
            if(func_symbol.getReturnType() == SymbolType.INT) {
                return 0;
            }else {
                return -1;
            }
        }

        for(Node child: children) {
            if(child.getDim(symbolManager) != -1) {
                return child.getDim(symbolManager);
            }
        }
        return -1;
    }


    @Override
    public String genValue(Printer printer, SymbolManager symbolManager) {    //
        if(children.get(0) instanceof PrimaryExp) {             //  PrimaryExp
            return children.get(0).genValue(printer, symbolManager);
        } else if(children.get(0) instanceof UnaryOp) {
            String tmp_ret = children.get(1).genValue(printer, symbolManager);    // UnaryExp

            if(((UnaryOp) children.get(0)).getUnaryOp().equals("+")) {
                return tmp_ret;
            } else if(((UnaryOp) children.get(0)).getUnaryOp().equals("-")) {
                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isNumber =  pattern.matcher(tmp_ret).matches();
                if(isNumber) {
                    int tmp_num = Integer.parseInt(tmp_ret);
                    tmp_num = 0 - tmp_num;
                    return String.valueOf(tmp_num);

                } else {
                    Temp temp = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.MINU_OP, temp.toString(), "0", tmp_ret));
                    return temp.toString();   //返回新生成的临时寄存器名
                }

            } else if(((UnaryOp) children.get(0)).getUnaryOp().equals("!")) {   // 只出现在条件表达式中的"!"
                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isNumber =  pattern.matcher(tmp_ret).matches();
                if(isNumber) {       // 是常数字
                    int tmp_num = Integer.parseInt(tmp_ret);
                    if(tmp_num != 0) {     // 非0数字取"!"都变0
                        return String.valueOf(0);
                    } else {               // 数字0取"!"变1
                        return String.valueOf(1);
                    }

                } else {
                    Temp temp = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.EQL, temp.toString(), "0", tmp_ret));  // temp$5 = 0 == a
                    return temp.toString();   //返回新生成的临时寄存器名
                }

            }

        } else {    //  Ident '(' [FuncRParams] ')' -调用函数, 返回函数的返回值
            String tokenname = ((TokenNode)children.get(0)).getToken().getTokenname();
            Symbol func_ident = symbolManager.getSymbolFromCurTable(tokenname);
            // 针对 foo(); 单独的语句
            ArrayList<String> paramList = new ArrayList<>();
            ArrayList<Integer> params_dim = func_ident.getParamsDim();   //
            for(Node child: children) {
                if(child instanceof FuncRParams) {
                    ((FuncRParams) child).genParamMidCode(printer, symbolManager, params_dim);  //
                    paramList = ((FuncRParams) child).getParamList();
                }
            }

            // 把实参push进函数
            if(!paramList.isEmpty()) {   // 实参非空
                for(String parm: paramList) {     // param可以是{"a[1][2]", "a", "temp&1"}
                    printer.addMidCodeList(new Midcode(Operation.PARAM_PUSH, parm));
                }

            }

            // 调用函数
            printer.addMidCodeList(new Midcode(Operation.CALL, tokenname));

            // 函数返回值
            if(func_ident.getReturnTypeStr().equals("void")) {
                return null;
            } else {
                Temp temp = new Temp();
                printer.addMidCodeList(new Midcode(Operation.RETURN_VALUE, temp.toString()));
                return temp.toString();    //
            }


        }
        return null;

    }



    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {     // 单独的仅调用函数 Ident '(' [FuncRParams] ')'
        if(children.get(0) instanceof TokenNode) {
            String tokenname = ((TokenNode)children.get(0)).getToken().getTokenname();
            Symbol func_ident = symbolManager.getSymbolFromCurTable(tokenname);
            // 针对 foo(); 单独的语句
            ArrayList<String> paramList = new ArrayList<>();
            ArrayList<Integer> params_dim = func_ident.getParamsDim();   //
            for(Node child: children) {
                if(child instanceof FuncRParams) {
                    ((FuncRParams) child).genParamMidCode(printer, symbolManager, params_dim);  //
                    paramList = ((FuncRParams) child).getParamList();
                }
            }

            // 把实参push进函数
            if(!paramList.isEmpty()) {   // 实参非空
                for(String parm: paramList) {
                    printer.addMidCodeList(new Midcode(Operation.PARAM_PUSH, parm));
                }

            }

            // 调用函数
            printer.addMidCodeList(new Midcode(Operation.CALL, tokenname));

            // 函数返回值
            if(func_ident.getReturnTypeStr().equals("void")) {
                return;
            } else {
                Temp temp = new Temp();
                printer.addMidCodeList(new Midcode(Operation.RETURN_VALUE, temp.toString()));
            }

        }

    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager, int arrayDim) {     // 专门处理函数实参
        if(children.get(0) instanceof PrimaryExp) {             //  PrimaryExp
            return children.get(0).genRParamValue(printer, symbolManager, arrayDim);
        } else if(children.get(0) instanceof UnaryOp) {
            String tmp_ret = children.get(1).genRParamValue(printer, symbolManager, arrayDim);    // UnaryExp

            if(((UnaryOp) children.get(0)).getUnaryOp().equals("+")) {
                return tmp_ret;
            } else if(((UnaryOp) children.get(0)).getUnaryOp().equals("-")) {
                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isNumber =  pattern.matcher(tmp_ret).matches();
                if(isNumber) {
                    int tmp_num = Integer.parseInt(tmp_ret);
                    tmp_num = 0 - tmp_num;
                    return String.valueOf(tmp_num);

                } else {
                    Temp temp = new Temp();
                    printer.addMidCodeList(new Midcode(Operation.MINU_OP, temp.toString(), "0", tmp_ret));
                    return temp.toString();   //返回新生成的临时寄存器名
                }

            }

        } else {    //  Ident '(' [FuncRParams] ')' -调用函数, 返回函数的返回值
            String tokenname = ((TokenNode)children.get(0)).getToken().getTokenname();
            Symbol func_ident = symbolManager.getSymbolFromCurTable(tokenname);
            // 针对 foo(); 单独的语句
            ArrayList<String> paramList = new ArrayList<>();
            ArrayList<Integer> params_dim = func_ident.getParamsDim();
            for(Node child: children) {
                if(child instanceof FuncRParams) {
                    ((FuncRParams) child).genParamMidCode(printer, symbolManager, params_dim);
                    paramList = ((FuncRParams) child).getParamList();
                }
            }

            // 把实参push进函数
            if(!paramList.isEmpty()) {   // 实参非空
                for(String parm: paramList) {
                    printer.addMidCodeList(new Midcode(Operation.PARAM_PUSH, parm));
                }

            }

            // 调用函数
            printer.addMidCodeList(new Midcode(Operation.CALL, tokenname));

            // 函数返回值
            if(func_ident.getReturnTypeStr().equals("void")) {
                return null;
            } else {
                Temp temp = new Temp();
                printer.addMidCodeList(new Midcode(Operation.RETURN_VALUE, temp.toString()));
                return temp.toString();    //
            }


        }
        return null;
    }



}
