package AST;

import MidCode.Midcode;
import MidCode.Operation;
import MidCode.Temp;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;
import java.util.regex.Pattern;


//  相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp    ==>   RelExp {('==' | '!=') RelExp}
public class EqExp extends Node{
    public EqExp(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }


    @Override
    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label) {
        String ret1 = children.get(0).genCondMidCode(printer, symbolManager, jump_label);   // 求RelExp

        for(int i=2; i<children.size(); i++) {    // 注意 i 的起点
            if(children.get(i) instanceof RelExp) {
                String ret2 = children.get(i).genCondMidCode(printer, symbolManager, jump_label);
                String tokenname = ((TokenNode)children.get(i-1)).getToken().getTokenname();   // "==" or "!="

                Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");   // 判断二者是否都是常数字
                boolean isNumber1 = pattern.matcher(ret1).matches();
                boolean isNumber2 = pattern.matcher(ret2).matches();
                if(isNumber1 && isNumber2) {      // 都是常数字
                    if(tokenname.equals("==")) {
                        int ret = Integer.parseInt(ret1) == Integer.parseInt(ret2) ? 1 : 0;    // 成功是1, 失败是0
                        ret1 = String.valueOf(ret);
                    } else if(tokenname.equals("!=")) {
                        int ret = Integer.parseInt(ret1) != Integer.parseInt(ret2) ? 1 : 0;
                        ret1 = String.valueOf(ret);
                    }

                } else {
                    Temp temp = new Temp();    // 需要临时寄存器来保存值

                    if(tokenname.equals("==")) {
                        printer.addMidCodeList(new Midcode(Operation.EQL, temp.toString(), ret1, ret2));
                    } else if(tokenname.equals("!=")) {
                        printer.addMidCodeList(new Midcode(Operation.NEQ, temp.toString(), ret1, ret2));
                    }

                    ret1 = temp.toString();    //
                }

            }

        }

        return ret1;

    }





}
