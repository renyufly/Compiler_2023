package AST;

import LEXER.LexType;
import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import OPTIMIZE.Optimizer;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;
import java.util.regex.Pattern;

// MulExp -> UnaryExp { ('*' | '/' | '%') UnaryExp}
public class MulExp extends Node{
    public MulExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {    // 计算表达式的值
        int ret = children.get(0).computeExpValue(symbolManager);
        for(int i=1; i<children.size(); i++) {
            if(children.get(i) instanceof TokenNode) {
                if(((TokenNode) children.get(i)).getToken().getLexType().equals(LexType.MULT.name())) {
                    i = i+1;
                    ret = ret * children.get(i).computeExpValue(symbolManager);
                } else if(((TokenNode) children.get(i)).getToken().getLexType().equals(LexType.DIV.name())) {
                    i = i+1;
                    ret = ret / children.get(i).computeExpValue(symbolManager);
                } else {
                    i = i+1;
                    ret = ret % children.get(i).computeExpValue(symbolManager);
                }
            }
        }
        return ret;
    }

    @Override
    public int getDim(SymbolManager symbolManager) {
        for(Node child: children) {
            if(child.getDim(symbolManager) != -1) {
                return child.getDim(symbolManager);
            }
        }
        return -1;
    }

    @Override
    public String genValue(Printer printer, SymbolManager symbolManager) {        // 生成中间代码
        if(children.size()>1) {    // UnaryExp {('*' | '/' | '%') UnaryExp}
            String ret1 = children.get(0).genValue(printer, symbolManager);
            for(int i=1; i<children.size(); i++) {            // i指着中间运算符的位置
                String ret2 = children.get(i+1).genValue(printer, symbolManager);

                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isret1Number = pattern.matcher(ret1).matches();
                boolean isret2Number = pattern.matcher(ret2).matches();
                if(isret1Number && isret2Number) {    // 都是数字就可以直接计算，不用寄存器
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("*")) {
                        int tmp_ret = Integer.parseInt(ret1) * Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    } else if(token.equals("/")) {
                        int tmp_ret = Integer.parseInt(ret1) / Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    } else if(token.equals("%")) {
                        int tmp_ret = Integer.parseInt(ret1) % Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    }

                    i++;

                } else {
                    Temp temp = new Temp();
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("*")) {
                        if(Optimizer.getInstance().isTurnOn()) {  // 开启优化
                            multiOptimize(printer, temp, ret1, ret2);

                        } else {
                            printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
                        }

                    } else if(token.equals("/")) {
                        printer.addMidCodeList(new Midcode(Operation.DIVI_OP, temp.toString(), ret1, ret2));
                    } else if(token.equals("%")) {
                        printer.addMidCodeList(new Midcode(Operation.MOD_OP, temp.toString(), ret1, ret2));
                    }
                    i++;
                    ret1 = temp.toString();   // 更新ret1
                }

            }

            return ret1;

        } else {     // 单个UnaryExp
            return children.get(0).genValue(printer, symbolManager);
        }
    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager, int arrayDim) {
        if(children.size()>1) {    // UnaryExp {('*' | '/' | '%') UnaryExp}
            String ret1 = children.get(0).genRParamValue(printer, symbolManager, arrayDim);
            for(int i=1; i<children.size(); i++) {            // i指着中间运算符的位置
                String ret2 = children.get(i+1).genRParamValue(printer, symbolManager, arrayDim);

                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isret1Number = pattern.matcher(ret1).matches();
                boolean isret2Number = pattern.matcher(ret2).matches();
                if(isret1Number && isret2Number) {    // 都是数字就可以直接计算，不用寄存器
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("*")) {
                        int tmp_ret = Integer.parseInt(ret1) * Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    } else if(token.equals("/")) {
                        int tmp_ret = Integer.parseInt(ret1) / Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    } else if(token.equals("%")) {
                        int tmp_ret = Integer.parseInt(ret1) % Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    }

                    i++;

                } else {
                    Temp temp = new Temp();
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("*")) {
                        if(Optimizer.getInstance().isTurnOn()) {  // 开启优化
                            multiOptimize(printer, temp, ret1, ret2);

                        } else {
                            printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
                        }

                    } else if(token.equals("/")) {
                        printer.addMidCodeList(new Midcode(Operation.DIVI_OP, temp.toString(), ret1, ret2));
                    } else if(token.equals("%")) {
                        printer.addMidCodeList(new Midcode(Operation.MOD_OP, temp.toString(), ret1, ret2));
                    }
                    i++;
                    ret1 = temp.toString();   // 更新ret1
                }

            }

            return ret1;

        } else {     // 单个UnaryExp
            return children.get(0).genRParamValue(printer, symbolManager, arrayDim);
        }
    }


    //  乘法优化
    public void multiOptimize(Printer printer, Temp temp, String ret1, String ret2) {
        Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
        boolean isret1Number = pattern.matcher(ret1).matches();
        boolean isret2Number = pattern.matcher(ret2).matches();
        if(isret1Number) {
            int a = Integer.parseInt(ret1);
            if(a < 0) {    // 是负数
                int a_abs = -a;
                int num = a_abs & (a_abs-1);
                if(a_abs > 0 && num == 0) {  // 是2的次幂
                    int index = 0;
                    while( a_abs > 1) {    // 确定具体是几次幂
                        a_abs = a_abs >> 1;
                        index += 1;
                    }

                    printer.addMidCodeList(new Midcode(Operation.MINU_OP, temp.toString(), "0", ret2));
                    printer.addMidCodeList(new Midcode(Operation.ASSIGN, ret2, temp.toString()));   // 将ret2取负

                    printer.addMidCodeList(new Midcode(Operation.SLL, temp.toString(), ret2, String.valueOf(index)));

                } else {
                    printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
                }

            } else {
                int num = a & (a - 1);
                if(a > 0 && num == 0) {  // 是2的次幂
                    int index = 0;
                    while( a > 1) {
                        a = a >> 1;
                        index += 1;
                    }

                    printer.addMidCodeList(new Midcode(Operation.SLL, temp.toString(), ret2, String.valueOf(index)));

                } else {
                    printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
                }
            }
        } else if(isret2Number) {
            int b = Integer.parseInt(ret2);
            if(b < 0) {    // 是负数
                int b_abs = -b;
                int num = b_abs & (b_abs-1);
                if(b_abs > 0 && num == 0) {  // 是2的次幂
                    int index = 0;
                    while( b_abs > 1) {
                        b_abs = b_abs >> 1;
                        index += 1;
                    }

                    printer.addMidCodeList(new Midcode(Operation.MINU_OP, temp.toString(), "0", ret1));
                    printer.addMidCodeList(new Midcode(Operation.ASSIGN, ret1, temp.toString()));   // 将ret1取负

                    printer.addMidCodeList(new Midcode(Operation.SLL, temp.toString(), ret1, String.valueOf(index)));

                } else {
                    printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
                }

            } else {
                int num = b & (b - 1);
                if(b > 0 && num == 0) {  // 是2的次幂
                    int index = 0;
                    while( b> 1) {
                        b = b >> 1;
                        index += 1;
                    }

                    printer.addMidCodeList(new Midcode(Operation.SLL, temp.toString(), ret1, String.valueOf(index)));

                } else {
                    printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
                }
            }
        } else {
            printer.addMidCodeList(new Midcode(Operation.MULT_OP, temp.toString(), ret1, ret2));
        }

    }


    // 除法优化



    // 取模优化




}
