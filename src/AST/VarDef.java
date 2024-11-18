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

// VarDef → Ident { '[' ConstExp ']' }  | Ident { '[' ConstExp ']' } '=' InitVal
public class VarDef extends Node{
    private Symbol symbol;
    public VarDef( String syntax_type, ArrayList<Node> children, int end_line) {
        super(syntax_type, children, end_line);
    }


    public boolean createSymbol(SymbolManager symbolManager) {
        // 创建符号
        String token = ((TokenNode)children.get(0)).getToken().getTokenname();
        SymbolType type_const = SymbolType.notconstVar;     // 变量
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
        SymbolType type = SymbolType.func;  // 新new一个type

        ArrayList<Integer> values = new ArrayList<>();   // 全局变量赋值

        if(dim == 0) {
            type = SymbolType.variable;
            values.add(0);             // 未显式初始化的全局变量， 其（元素）值均被初始化为 0
        } else if(dim == 1) {
            type = SymbolType.oneDarray;
            for(int i=0; i<dim1; i++) {
                values.add(0);
            }
        } else if(dim == 2) {
            type = SymbolType.twoDarray;
            int dim_i = dim1*dim2;
            for(int i=0; i<dim_i; i++) {
                values.add(0);
            }
        }

        // 将值赋给Ident (只有是全局变量才行，全局变量声明中指定的初值表达式必须是常量表达式[不会用非const变量赋值])

        boolean isGlobal = false;

        if(symbolManager.getIsGlobalDecl()) {    // 只有是全局变量, 才会有值的记录
            isGlobal = true;
            if(children.get(children.size()-1).getSyntaxType().equals(SyntaxType.InitVal.name())) {   // 已显式赋值
                values = ((InitVal)children.get(children.size()-1)).computeValue(dim, symbolManager);
            }
        } else {
            values = null;
        }

        this.symbol = new Symbol(token, type, type_const, type_value, dim1, dim2, values);
        return isGlobal;
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        createSymbol(symbolManager);


        super.checkError(printer, symbolManager);

        boolean ans = symbolManager.addSymbol(this.symbol);   // 符号表添加符号
        if(!ans) {
            printer.addErrorMessage(children.get(0).getEndLine(), ErrorType.b);   //
        }
    }

    @Override
    public void genMidCode(Printer printer, SymbolManager symbolManager) {   //  Ident { '[' ConstExp ']' }  ['=' InitVal]
        boolean isGlobal = createSymbol(symbolManager);      // 创建符号


        String ident = ((TokenNode)children.get(0)).getToken().getTokenname();
        InitVal initVal = null;
        if((children.size() < 2) || !((TokenNode)children.get(1)).getToken().getTokenname().equals("[")) {  // 是非数组
            boolean initial_flag = false;         // 判断定义变量时是否赋有初值
            for(Node child: children) {
                if(child instanceof InitVal) {
                    initVal = (InitVal) child;
                    initial_flag = true;
                    break;
                }
            }
            if(initial_flag) {   // 已赋初值-非数组变量
                String tmp_value = initVal.genMidcodeInitValue(printer, symbolManager);   //
                printer.addMidCodeList(new Midcode(Operation.VAR, ident, tmp_value));
            } else {            // 没赋初值
                printer.addMidCodeList(new Midcode(Operation.VAR, ident));
            }

        } else {    // 是数组
            boolean initial_flag = false;         // 判断定义变量时是否赋有初值
            for(Node child: children) {
                if(child instanceof InitVal) {
                    initVal = (InitVal) child;
                    initial_flag = true;
                    break;
                }
            }

            int array_dim = 0;
            int[] dim = new int[2];   // 记录对应数组变量的一维/二维的维数
            for(Node child: children) {
                if(child instanceof ConstExp) {    //constExp是保证能求出值的, Exp不保证
                    dim[array_dim] = child.computeExpValue(symbolManager);
                    array_dim++;
                }
            }

            // 先声明数组
            if(array_dim == 1) {     // 一维
                if(initial_flag) {
                    printer.addMidCodeList(new Midcode(Operation.ARRAY, ident, String.valueOf(dim[0]), null, true));
                } else {   // 没赋值
                    printer.addMidCodeList(new Midcode(Operation.ARRAY, ident, String.valueOf(dim[0])));   // "array int ident[arg1]"
                    if(isGlobal) {

                        ArrayList<Integer> arrayList = new ArrayList<>();

                        for(int i=0; i<dim[0]; i++) {
                            arrayList.add(0);
                        }
                        printer.addMipsGlobalArray(ident, arrayList);
                    }
                }

            } else if(array_dim == 2) {     // 二维
                if(initial_flag) {
                    printer.addMidCodeList(new Midcode(Operation.ARRAY, ident, String.valueOf(dim[0]), String.valueOf(dim[1]), true));
                } else {
                    printer.addMidCodeList(new Midcode(Operation.ARRAY, ident, String.valueOf(dim[0]), String.valueOf(dim[1]))); //

                    if(isGlobal) {
                        int len = dim[0] * dim[1];
                        ArrayList<Integer> arrayList = new ArrayList<>();
                        for(int i=0; i < len; i++) {
                            arrayList.add(0);
                        }
                        printer.addMipsGlobalArray(ident, arrayList);
                    }

                }

            }


            if(initial_flag) {   // 已赋初值-数组变量
                if(array_dim == 1) {     // 一维
                    ArrayList<String> tmp_Init = initVal.genAllMidcodeInitValue(printer, 1, symbolManager);

                    if(isGlobal) {
                        ArrayList<Integer> arrayList = new ArrayList<>();
                        for(int arr_i=0; arr_i<dim[0]; arr_i++) {
                            arrayList.add(Integer.parseInt(tmp_Init.get(arr_i)));
                        }
                        printer.addMipsGlobalArray(ident, arrayList);

                    } else {
                        for(int arr_i = 0; arr_i<dim[0]; arr_i++) {   // a[0] = 1;  a[1] = 2;
                            printer.addMidCodeList(new Midcode(Operation.SETARR, ident, String.valueOf(arr_i), tmp_Init.get(arr_i)));
                        }
                    }

                } else if(array_dim == 2) {     // 二维
                    ArrayList<String> tmp_Init = initVal.genAllMidcodeInitValue(printer, 2, symbolManager);
                    int dim_length = dim[0] * dim[1];
                    if(isGlobal) {
                        ArrayList<Integer> arrayList = new ArrayList<>();
                        for(int arr_i=0; arr_i<dim_length; arr_i++) {
                            arrayList.add(Integer.parseInt(tmp_Init.get(arr_i)));
                        }
                        printer.addMipsGlobalArray(ident, arrayList);

                    } else {
                        for(int arr_i=0; arr_i < dim_length; arr_i++) {
                            printer.addMidCodeList(new Midcode(Operation.SETARR, ident, String.valueOf(arr_i), tmp_Init.get(arr_i)));
                        }
                    }


                }
            }

        }


        boolean ans = symbolManager.addSymbol(this.symbol);   // 符号表添加符号

    }




}
