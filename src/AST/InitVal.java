package AST;

import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;

// InitVal ==>  Exp | '{' [InitVal {',' InitVal}]'}'     ==>  Exp {',' Exp}
public class InitVal extends Node{
    private ArrayList<Integer> initValue = new ArrayList<>();
    public InitVal(String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    public ArrayList<Integer> computeValue(int dim, SymbolManager symbolManager) {    // 计算值
        ArrayList<Integer> ret = new ArrayList<>();
        if(dim == 0) {
            ret.add(((Exp)children.get(0)).computeExpValue(symbolManager));
        } else {       // 数组形式的赋值
            for(Node child: children) {
                if(child instanceof InitVal) {
                    ArrayList<Integer> tmp = ((InitVal) child).computeValue(dim-1, symbolManager);
                    ret.addAll(tmp);
                }
            }
        }

        this.initValue = ret;
        return ret;
    }

    public ArrayList<Integer> getInitValue() {
        return this.initValue;
    }


    public String genMidcodeInitValue(Printer printer, SymbolManager symbolManager) {   // 只返回单个Exp的情况

        return children.get(0).genValue(printer, symbolManager);   // 返回的是 数值"6" or 单一变量名"a" or 临时寄存器"temp&1"

    }

    public ArrayList<String> genAllMidcodeInitValue(Printer printer, int dim, SymbolManager symbolManager) {   // 返回含多个Exp的情况
        ArrayList<String> ret = new ArrayList<>();
        if(dim == 1) {    // 一维 InitVal -> '{' InitVal, InitVal,...'}'
            for(Node child: children){
                if(child instanceof InitVal) {
                    String tmp_ret = ((InitVal) child).genMidcodeInitValue(printer, symbolManager);
                    ret.add(tmp_ret);
                }
            }
            return ret;
        } else if(dim == 2) {   // 二维 InitVal -> {InitVal, } -> {InitVal}
            for (Node child : children) {
                if (child instanceof InitVal) {
                    ArrayList<String> tmp_list = new ArrayList<>();
                    tmp_list = ((InitVal) child).genAllMidcodeInitValue(printer, 1, symbolManager);
                    for (int i = 0; i < tmp_list.size(); i++) {
                        String tmp_ret = tmp_list.get(i);
                        ret.add(tmp_ret);
                    }
                }
            }
            return ret;
        }

        return ret;
    }


}
