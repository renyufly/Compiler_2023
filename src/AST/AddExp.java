package AST;

import LEXER.LexType;
import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;
import java.util.regex.Pattern;

// AddExp → MulExp | AddExp ('+' | '−') MulExp   ==>  AddExp -> MulExp { ('+' | '−') MulExp }
public class AddExp extends Node{
    public AddExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }

    @Override
    public int computeExpValue(SymbolManager symbolManager) {    // 计算表达式的值
        int ret = children.get(0).computeExpValue(symbolManager);
        for(int i=1; i<children.size(); i++) {
            if(children.get(i) instanceof TokenNode) {
                if(((TokenNode) children.get(i)).getToken().getLexType().equals(LexType.PLUS.name())) {   // '+'
                    i = i+1;
                    ret = ret + children.get(i).computeExpValue(symbolManager);
                } else {                                                                           // '-'
                    i = i+1;
                    ret = ret - children.get(i).computeExpValue(symbolManager);
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
    public String genValue(Printer printer, SymbolManager symbolManager) {   // 返回AddExp的结果(以字符串形式)
        if(children.size() > 1) {  // 有多个MulExp      MulExp { ('+' | '−') MulExp }

            String ret1 = children.get(0).genValue(printer,symbolManager);

            for(int i=1; i<children.size(); i++) {            // i指着中间运算符的位置
                String ret2 = children.get(i+1).genValue(printer,symbolManager);

                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isret1Number = pattern.matcher(ret1).matches();
                boolean isret2Number = pattern.matcher(ret2).matches();
                if(isret1Number && isret2Number) {
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("+")) {
                        int tmp_ret = Integer.parseInt(ret1) + Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    } else if(token.equals("-")) {
                        int tmp_ret = Integer.parseInt(ret1) - Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    }

                    i++;

                } else {
                    Temp temp = new Temp();
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("+")) {
                        printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp.toString(), ret1, ret2));
                    } else if(token.equals("-")) {
                        printer.addMidCodeList(new Midcode(Operation.MINU_OP, temp.toString(), ret1, ret2));
                    }

                    i++;
                    ret1 = temp.toString();      // 更新ret1
                }

            }

            return ret1;

        } else {       // 单一MulExp
            return children.get(0).genValue(printer, symbolManager);
        }
    }


    @Override
    public String genRParamValue(Printer printer, SymbolManager symbolManager,  int arrayDim) {
        if(children.size() > 1) {  // 有多个MulExp      MulExp { ('+' | '−') MulExp }

            String ret1 = children.get(0).genRParamValue(printer, symbolManager, arrayDim);

            for(int i=1; i<children.size(); i++) {            // i指着中间运算符的位置
                String ret2 = children.get(i+1).genRParamValue(printer, symbolManager, arrayDim);

                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
                boolean isret1Number = pattern.matcher(ret1).matches();
                boolean isret2Number = pattern.matcher(ret2).matches();
                if(isret1Number && isret2Number) {
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("+")) {
                        int tmp_ret = Integer.parseInt(ret1) + Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    } else if(token.equals("-")) {
                        int tmp_ret = Integer.parseInt(ret1) - Integer.parseInt(ret2);
                        ret1 = String.valueOf(tmp_ret);
                    }

                    i++;

                } else {
                    Temp temp = new Temp();
                    String token = ((TokenNode)children.get(i)).getToken().getTokenname();
                    if(token.equals("+")) {
                        printer.addMidCodeList(new Midcode(Operation.PLUS_OP, temp.toString(), ret1, ret2));
                    } else if(token.equals("-")) {
                        printer.addMidCodeList(new Midcode(Operation.MINU_OP, temp.toString(), ret1, ret2));
                    }

                    i++;
                    ret1 = temp.toString();      // 更新ret1
                }

            }

            return ret1;

        } else {       // 单一MulExp
            return children.get(0).genRParamValue(printer, symbolManager, arrayDim);
        }
    }



}
