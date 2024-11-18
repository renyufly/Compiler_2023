package AST;

import MidCode.Midcode;
import MidCode.Operation;
import PARSER.SyntaxType;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

// ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
public class ConstDef extends Node{
    private Symbol symbol;
    public ConstDef( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }


    public boolean createSymbol(SymbolManager symbolManager) {
        // 创建符号
        String token = ((TokenNode)children.get(0)).getToken().getTokenname();
        SymbolType type_const = SymbolType.constVar;
        int dim = 0;
        int dim1 = 0;
        int dim2 = 0;
        SymbolType type_value = SymbolType.INT;
        for(Node child: children) {
            if(child instanceof ConstExp) {
                dim = dim+1;
                if(dim == 1) {
                    dim1 = child.computeExpValue(symbolManager);   // 需要计算表达式的值
                } else if(dim == 2) {
                    dim2 = child.computeExpValue(symbolManager);
                }
            }
        }
        SymbolType type = null;
        if(dim == 0) {
            type = SymbolType.variable;
        } else if(dim == 1) {
            type = SymbolType.oneDarray;
        } else if(dim == 2) {
            type = SymbolType.twoDarray;
        }

        // 将值赋给Ident
        ArrayList<Integer> values = new ArrayList<>();
        if(!children.get(children.size()-1).getSyntaxType().equals(SyntaxType.ConstInitVal.name())) {
            values = null;
        } else {
            values = ((ConstInitVal)children.get(children.size()-1)).computeValue(dim, symbolManager);
        }

        this.symbol = new Symbol(token, type, type_const, type_value, dim1, dim2, values);

        if(symbolManager.getIsGlobalDecl()) {
            return true;
        } else {
            return false;
        }

    }

    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        createSymbol(symbolManager);   // 创建符号

        super.checkError(printer, symbolManager);

        boolean ans = symbolManager.addSymbol(this.symbol);   // 符号表添加符号
        if(!ans) {
            printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.b);   //
        }


    }


    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {      // Ident { '[' ConstExp ']' } '=' ConstInitVal
        boolean isGlobal = createSymbol(symbolManager);    // 创建符号

        String ident = ((TokenNode)children.get(0)).getToken().getTokenname();
        ConstInitVal constInitVal = null;
        if((children.size() < 2) || !((TokenNode)children.get(1)).getToken().getTokenname().equals("[")) {  // 是非数组
            for(Node child: children) {
                if(child instanceof ConstInitVal) {
                    constInitVal = (ConstInitVal) child;
                    break;
                }
            }
              // 必然赋初值-非数组变量
            ArrayList<Integer> ret = constInitVal.computeValue(0, symbolManager);  // 计算值

            printer.addMidCodeList(new Midcode(Operation.CONST, ident, String.valueOf(ret.get(0))));

        } else {    // 是数组
            for(Node child: children) {
                if(child instanceof ConstInitVal) {
                    constInitVal = (ConstInitVal) child;
                    break;
                }
            }
            int array_dim = 0;
            int[] dim = new int[2];   // 记录对应数组变量的一维/二维的维数
            for(Node child: children) {
                if(child instanceof ConstExp) {
                    dim[array_dim] = child.computeExpValue(symbolManager);   //constExp是保证能求出值的, Exp不保证
                    array_dim++;
                }
            }

            // 先声明数组
            if(array_dim == 1) {     // 一维
                printer.addMidCodeList(new Midcode(Operation.ARRAY, ident, String.valueOf(dim[0])));   // "array int ident[arg1]"
            } else if(array_dim == 2) {     // 二维
                printer.addMidCodeList(new Midcode(Operation.ARRAY, ident, String.valueOf(dim[0]), String.valueOf(dim[1]))); //
            }

            if(array_dim == 1) {     // 一维
                ArrayList<Integer> tmp_Init = constInitVal.computeValue(1, symbolManager);   //

                if(isGlobal) {
                    ArrayList<Integer> arrayList = new ArrayList<>();
                    for(int arr_i=0; arr_i<dim[0]; arr_i++) {
                        arrayList.add(tmp_Init.get(arr_i));
                    }
                    printer.addMipsGlobalArray(ident, arrayList);

                } else {
                    for(int arr_i = 0; arr_i<dim[0]; arr_i++) {   // a[0] = 1;  a[1] = 2;
                        printer.addMidCodeList(new Midcode(Operation.SETARR, ident, String.valueOf(arr_i), String.valueOf(tmp_Init.get(arr_i))));
                    }
                }


            } else if(array_dim == 2) {     // 二维
                ArrayList<Integer> tmp_Init = constInitVal.computeValue(2, symbolManager);   //
                int dim_length = dim[0] * dim[1];

                if(isGlobal) {
                    ArrayList<Integer> arrayList = new ArrayList<>();
                    for(int arr_i=0; arr_i<dim_length; arr_i++) {
                        arrayList.add(tmp_Init.get(arr_i));
                    }
                    printer.addMipsGlobalArray(ident, arrayList);

                }else {
                    for(int arr_i=0; arr_i < dim_length; arr_i++) {
                        printer.addMidCodeList(new Midcode(Operation.SETARR, ident, String.valueOf(arr_i), String.valueOf(tmp_Init.get(arr_i))));
                    }
                }

            }

        }


        boolean ans = symbolManager.addSymbol(this.symbol);   // 符号表添加符号


    }










}
