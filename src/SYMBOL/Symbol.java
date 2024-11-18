package SYMBOL;

import java.util.ArrayList;

public class Symbol {              // 对应符号的各种属性

    private String token;    // 当前token名
    private SymbolType type;        // 变量/一维数组/二维数组/函数
    private SymbolType type_const;  // 常量/变量
    private SymbolType type_value;    // 值类型 ： int/void
    private int dim1;         //数组的维数： a[dim1][dim2] dim1 dim2
    private int dim2;
    private ArrayList<Integer> value;       // 变量的值


    // func:
    private SymbolType return_type;   // 函数返回值类型：int型/void型
    private ArrayList<SymbolType> params_type;     // 各参数的形参类型 (int/void)
    private ArrayList<Integer> params_dim;   // 各参数的维数 (0/1/2)

    // Mips
    private int offset;    // 偏移量
    private boolean isPointer = false;    // 是否是指针类型 (涉及数组形参)
    private boolean isVar = false;
    private boolean isArray = false;
    private boolean isGlobalArray = false;
    private int arr_type = 0;   // 数组是一维还是二维


    public Symbol(String token, SymbolType type, SymbolType type_const, SymbolType type_value, int dim1, int dim2, ArrayList<Integer> values) {
        this.token = token;
        this.type = type;
        this.type_const = type_const;
        this.type_value = type_value;
        this.dim1 = dim1;
        this.dim2 = dim2;
        this.value = values;
    }       // 关于变量/数组的创建

    public Symbol(String token, SymbolType return_type) {
        this.token = token;
        this.type = SymbolType.func;
        this.return_type = return_type;
    }      // 关于函数的创建


    // MIPS
    public Symbol(String token, int offset) {
        this.token = token;
        this.offset = offset;
        this.isPointer = false;
    }       // 关于Mips生成过程中符号的创建

    public Symbol(String token, int offset, boolean isPointer) {
        this.token = token;
        this.offset = offset;
        this.isPointer = isPointer;
    }    // 关于Mips生成过程中指针类型的符号创建




    public void completeFuncInfo(ArrayList<SymbolType> params_type, ArrayList<Integer> params_dim) {
        this.params_type = params_type;
        this.params_dim = params_dim;
    }    // 对函数形参信息的补充


    public String getToken() {
        return this.token;
    }

    public SymbolType getType() {
        return this.type;
    }

    public boolean isConstVar() {       // 判断type_const是否是常量
        if(this.type_const == SymbolType.constVar) {
            return true;     // 是常量
        } else {
            return false;    // 是变量
        }
    }


    public int getDim() {           // 获得标识符的维数
        if(this.type == SymbolType.variable) {
            return 0;     // 维数是0
        } else if(this.type == SymbolType.oneDarray) {
            return 1;
        } else if(this.type == SymbolType.twoDarray) {
            return 2;
        }
        return 0;
    }

    public int getDim1() {     // 获得数组第一维的定义时限制维数
        return dim1;
    }

    public int getDim2() {
        return dim2;
    }

    public int getValue() {                          // 返回变量的值
        return this.value.get(0);
    }        // 返回变量的值

    public int getValue(int index1) {                 // 返回一维数组的某个值
        return this.value.get(index1);
    }    // 返回一维数组的某个值 a[index1]

    public int getValue(int index1, int index2) {     // 返回二维数组的某个值    a[index1][index2]
        if(index1 == 0) {
            return this.value.get(index2);
        } else {
            return this.value.get(index1*dim2 + index2);
        }
    }

    public SymbolType getTypeValue() {
        return this.type_value;
    }

    public SymbolType getReturnType() {
        return this.return_type;
    }

    public String getReturnTypeStr() {
        if(this.return_type.equals(SymbolType.INT)) {
            return "int";
        } else if(this.return_type.equals(SymbolType.VOID)) {
            return "void";
        }
        return null;
    }

    public int getFuncParamsCnt() {          // 返回形参个数
        return this.params_type.size();
    }

    public ArrayList<Integer> getParamsDim() {
        return this.params_dim;
    }


    // Mips
    public int getOffset() {
        return this.offset;
    }

    public void setIsVar(boolean isVar) {
        this.isVar = isVar;
    }

    public void setIsArray(boolean isArray) {
        this.isArray = isArray;
    }

    public boolean isVar() {
        return this.isVar;
    }

    public boolean isArray() {
        return this.isArray;
    }

    public void setIsGlobalArray(boolean isGlobalArray1) {
        this.isGlobalArray = isGlobalArray1;
    }

    public boolean isGlobalArray() {
        return this.isGlobalArray;
    }

    public void setArrType(int arr_type) {
        this.arr_type = arr_type;
    }

    public int getArrType(){
        return this.arr_type;
    }

    public boolean isPointer() {
        return this.isPointer;
    }


}
