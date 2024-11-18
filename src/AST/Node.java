package AST;

import MidCode.Midcode;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Node {     // 树的节点 (非终结符)

    protected String syntax_type;         // [private类型会禁止子类访问]
    protected ArrayList<Node> children;
    protected int end_line;       // 非终结符产生的终结符号串的最后一个符号所在行号

    protected static int jump_cnt = 0;   //   jump label序号标记 (要从1开始, 故先加加)
    protected static Stack<Integer> loop_stack = new Stack<>();   // for循环使用的栈
    protected static HashMap<Integer, ForStmt> loop_forstmt_map = new HashMap<>();   // for循环的for (;;赋值语句)



    public Node(String syntax_type, ArrayList<Node> children, int end_line){
        this.syntax_type = syntax_type;
        this.children = children;
        this.end_line = end_line;
    }

    public int getEndLine() {
        return this.end_line;
    }

    public ArrayList<Node> getChildren() {
        return this.children;
    }

    public String getSyntaxType() {
        return this.syntax_type;
    }

    public void checkError(Printer printer, SymbolManager symbolManager) {             // 需要被继承者重写或调用
        if(children.size() == 0) {     // 子树向下遍历
            return;
        } else {
            for(Node children_i: children) {
                children_i.checkError(printer, symbolManager);
            }
        }
    }

    public int computeExpValue(SymbolManager symbolManager) {    // 计算表达式的值
        return 0;
    }

    public int getDim(SymbolManager symbolManager) {    // 获得维数(0/1/2)
        return -1;
    }



    // ↓ 中间代码生成部分  ↓

    public String genValue(Printer printer, SymbolManager symbolManager) {
        return null;
    }



    public void genMidCode(Printer printer, SymbolManager symbolManager) {         // 递归生成四元式
        if(children == null || children.size() == 0) {
            return;
        } else {
            for(Node child:children) {
                child.genMidCode(printer, symbolManager);
            }
        }

    }

    public String genRParamValue(Printer printer, SymbolManager symbolManager, int arrayDim) {return  null;}



    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label) {  // 针对if中条件表达式
        return null;
    }


    public String genCondMidCode(Printer printer, SymbolManager symbolManager, int jump_label, boolean is_for) {  // 针对for中条件表达式
        return null;
    }


}

