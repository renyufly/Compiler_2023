package AST;

import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.Printer;

import java.util.ArrayList;

// 函数实参表 FuncRParams → Exp { ',' Exp }
public class FuncRParams extends Node{
    private ArrayList<String> param_list = new ArrayList<>();

    public FuncRParams( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    public int getFuncRparamsCnt() {           // 返回实参个数
        int cnt = 0;
        for(Node child: children) {
            if(child instanceof Exp) {
                cnt++;
            }
        }
        return cnt;
    }

    public ArrayList<Integer> getRparamsDim(SymbolManager symbolManager) {         // 返回各实参对应维数(0/1/2)
        ArrayList<Integer> ret = new ArrayList<>();
        for(Node child: children) {
            if(child instanceof Exp) {
                ret.add(child.getDim(symbolManager));
            }
        }

        return ret;
    }



    public void genParamMidCode(Printer printer, SymbolManager symbolManager, ArrayList<Integer> params_dim) {  //   函数实参表 FuncRParams → Exp { ',' Exp }  【变量、数组、函数】
        int cnt = 0;
        for(Node child: children) {
            if(child instanceof Exp) {
                this.param_list.add(child.genRParamValue(printer, symbolManager, params_dim.get(cnt)));
                cnt++;
            }

        }

    }




    public ArrayList<String> getParamList() {
        return this.param_list;
    }


}
