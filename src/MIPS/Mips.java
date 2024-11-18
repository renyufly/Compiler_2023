package MIPS;

import MidCode.Midcode;
import MidCode.Operation;
import OPTIMIZE.Optimizer;
import OPTIMIZE.Register;
import SYMBOL.Symbol;
import SYMBOL.SymbolManager;
import tools.Printer;

import javax.management.RuntimeErrorException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
sp寄存器会保存栈顶的地址.
fp寄存器属于通用寄存器,但是有时利用它保存栈底的地址。
*/

public class Mips {

    private ArrayList<Midcode> midcodesList = new ArrayList<>();   // 生成的中间代码
    private ArrayList<String> mipsString = new ArrayList<>();    // 要保存在.data处的各字符串

    private HashMap<String, ArrayList<Integer>> mipsGlobalArray = new HashMap<>();    // <数组名, arr.数组名> 要保存在.data处的全局数组

    private HashMap<String, String> mipsStringMap = new HashMap<>();  // <"字符串", "str.1"> 记录存在数据段字符串的标签

    private HashMap<String, Integer> funcLengthMap = new HashMap<>();   // <函数名, 函数整体长度>
    private ArrayList<Midcode> paramPushList = new ArrayList<>();   // 函数调用前,push进的所有实参

    private Register register;
    private int funcPointer = 0;
    private boolean isInFunc;     // 是否进入函数
    private boolean isInMain;     // 是否进入main主函数


    private Printer printer;
    private SymbolManager symbolManager;

    public Mips(ArrayList<Midcode> midcodesList, ArrayList<String> mipsString,
                HashMap<String, ArrayList<Integer>> mipsGlobalArr,
                Printer printer, SymbolManager symbolManager) {

        this.midcodesList = midcodesList;
        this.mipsString = mipsString;

        this.mipsGlobalArray = mipsGlobalArr;

        this.register = new Register();

        this.isInFunc = false;
        this.isInMain = false;

        this.printer = printer;
        this.symbolManager = symbolManager;
    }

    public Printer getPrinter() {
        return this.printer;
    }

    public void computeFuncLength() {   // 计算函数长度
        String funcName = null;   //
        int cnt = 0;
        int i = 0;    //
        for(i=0; i<midcodesList.size(); i++) {
            if(midcodesList.get(i).getOp() == Operation.FUNC || midcodesList.get(i).getOp() == Operation.MAIN_BEGIN) {
                break;
            }
        }

        for( ; i<midcodesList.size(); i++) {
            Midcode midcode = midcodesList.get(i);
            if(midcode.getOp() == Operation.FUNC || midcode.getOp() == Operation.MAIN_BEGIN) {  // 是声明函数 （包括main函数）
                if(funcName != null) {    // 把上一个函数的长度存起来
                    funcLengthMap.put(funcName, cnt);
                }
                funcName = midcode.getRet();   // 获取函数名
                cnt = 0;      // 计数清零
            }
            if(midcode.getOp() == Operation.ARRAY) {     // 声明数组
                int dim = Integer.parseInt(midcode.getArg1());   // 一维维度
                if(midcode.getArg2() != null) {
                    int dim2 = Integer.parseInt(midcode.getArg2());
                    dim = dim * dim2;      // 总维数是 一维*二维
                }

                cnt = cnt + dim;    //

            }

            cnt = cnt+2;


        }

        funcLengthMap.put(funcName, cnt);    // 把最后一个函数(一定是main)存起来

    }


    public void genMips() {      // 生成mips代码

        printer.addMipsCode(new MipsCode(MipsOperation.data, ""));  // 生成 .data
        for(int i=0; i<mipsString.size(); i++) {

            if( !this.mipsStringMap.containsKey(mipsString.get(i))) {   //
                printer.addMipsCode(new MipsCode(MipsOperation.asciiz, "str." + i, mipsString.get(i)));  // 将字符串存在全局数据段，并带标签
                this.mipsStringMap.put(mipsString.get(i), "str." + i);
            }

        }

        for(String ident: this.mipsGlobalArray.keySet()) {
            printer.addMipsCode(new MipsCode(MipsOperation.word, "arr."+ident,
                    String.valueOf(this.mipsGlobalArray.get(ident).size()), this.mipsGlobalArray.get(ident)));
        }


        printer.addMipsCode(new MipsCode(MipsOperation.text, ""));   // 生成 .text

        symbolManager.pushSymbolTable();    // 为最外层的全局创建符号表

        for(int i=0; i<midcodesList.size(); i++) {
            Midcode midcode = midcodesList.get(i);

            switch (midcode.getOp()) {
                case VAR:            // "var int a"   "var int a = 3"   ret = arg1;  ret;
                    if(midcode.getArg1() != null) {  // 有赋初始值
                        String addr = getValue(midcode.getArg1(), "$t0", false);
                        saveValue(midcode.getRet(), addr, true);   // 把声明的变量存起来
                    } else {
                        Symbol varsymbol = new Symbol(midcode.getRet(), funcPointer);
                        varsymbol.setIsVar(true);
                        boolean success = symbolManager.addSymbol(varsymbol);
                        if(success) {
                            funcPointer = funcPointer +1;   //
                        }
                    }
                    continue;
                case ARRAY:     // "array int a[5]"  "array int b[2][3]"   ret[arg1][arg2]
                    int arr_type = 1;
                    int dim = Integer.parseInt(midcode.getArg1());    // 一维维数
                    if(midcode.getArg2() != null) {
                        arr_type = 2;
                        int dim2 = Integer.parseInt(midcode.getArg2());
                        dim = dim * dim2;         // 求出二维维数
                    }
                    createSymbolWithLen(midcode.getRet(), dim, midcode.getIsInit(), arr_type);    // 添加符号
                    continue;
                case CONST:       // "const int a = 2"    对数组的常量声明转成"array int b[2]"
                    String addr = getValue(midcode.getArg1(), "$t0", false);
                    saveValue(midcode.getRet(), addr, true);   // 存起来
                    continue;

                case LABEL:      // "label 1 begin"    "label 1 end"    label ret arg1
                    if(midcode.getArg1().equals("begin")) {
                        symbolManager.pushSymbolTable();   // 创建符号表
                    } else if(midcode.getArg1().equals("end")) {
                        symbolManager.popSymbolTable();   // 退出并删除当层符号表
                        register.clear();    //
                    }
                    continue;

                case MAIN_BEGIN:       // "main_begin"
                    if(!isInFunc) {
                        printer.addMipsCode(new MipsCode(MipsOperation.j, "main"));   // "j main"
                        isInFunc = true;
                    }
                    isInMain = true;
                    printer.addMipsCode(new MipsCode(MipsOperation.label, "main"));
                    funcPointer = 0;   //
                    int len = funcLengthMap.get("main");
                    printer.addMipsCode(new MipsCode(MipsOperation.move, "$fp", "$sp"));  // copy from sp to fp
                    printer.addMipsCode(new MipsCode(MipsOperation.addiu, "$sp", "$sp", "", -4*len-8));

                    continue;
                case MAIN_END:
                    // 直接结束
                    continue;

                case FUNC:     // "void foo()"  "int foo()"      arg1 ret()
                    if(!isInFunc) {
                        printer.addMipsCode(new MipsCode(MipsOperation.j, "main"));
                        isInFunc = true;
                    }
                    printer.addMipsCode(new MipsCode(MipsOperation.label, midcode.getRet()));
                    funcPointer = 0;     //
                    continue;
                case PARAM_DEF:     // "param_def int a"  "param_def int a[]"  "param int_def a[][20]"
                    if(midcode.getArg1().equals("0")) {    // 普通变量
                        Symbol varsymbol = new Symbol(midcode.getRet(), funcPointer);
                        varsymbol.setIsVar(true);
                        boolean success = symbolManager.addSymbol(varsymbol);
                        if(success) {
                            funcPointer = funcPointer +1;   //
                        }
                    } else {    // 数组形参 (实际传的指针)   "param_def int a[]"  "param int_def a[][20]"
                        Symbol arraysymbol = new Symbol(midcode.getRet(), funcPointer, true);
                        arraysymbol.setIsArray(true);
                        boolean success = symbolManager.addSymbol(arraysymbol);
                        if(success) {
                            funcPointer = funcPointer +1;   //
                        }
                    }
                    continue;
                case PARAM_PUSH:      // "param_push a"  "param_push b[1][2]"
                    paramPushList.add(midcode);
                    continue;
                case CALL:            // "call foo"     call ret
                    for(int j1=0; j1<paramPushList.size(); j1++) {    // 遍历传入的所有实参    Ret可以是a, b[1][2]
                        Midcode rparamMidcode = paramPushList.get(j1);
                        if( !rparamMidcode.getRet().contains("[")) {   //  "param_push a"

                            Symbol arr_symbol = symbolManager.getSymbolFromCurTable(rparamMidcode.getRet());
                            if(arr_symbol != null && arr_symbol.isGlobalArray()) {
                                printer.addMipsCode(new MipsCode(MipsOperation.la, "$t0", "arr."+rparamMidcode.getRet()));
                                printer.addMipsCode(new MipsCode(MipsOperation.sw, "$t0", "$sp", "", -4*j1));  // 把全局的数组地址存进去
                            } else {
                                String addr1 = getAddr(rparamMidcode.getRet(), "$t0");
                                printer.addMipsCode(new MipsCode(MipsOperation.sw, addr1, "$sp", "", -4*j1));
                            }

                        } else {             //  "param_push b[1][2]"  "param_push a[i][3]"

                            Pattern re = Pattern.compile("([^\\[]*)\\[([^\\[]*)]\\[([^\\[]*)]");
                            Matcher matcher = re.matcher(rparamMidcode.getRet());  // 利用RE把b,1,2分开
                            if(matcher.find()) {
                                String param_ret = matcher.group(1);
                                String param_arg1 = matcher.group(2);
                                String param_arg2 = matcher.group(3);

                                Symbol arr_symbol = symbolManager.getSymbolFromCurTable(param_ret);
                                if(arr_symbol != null && arr_symbol.isGlobalArray()) {   // 是全局数组
                                    int array_type = arr_symbol.getArrType();
                                    String addr1 =getValue(param_arg1, "$t1", false);
                                    printer.addMipsCode(new MipsCode(MipsOperation.li, "$t2", "", "", array_type ));
                                    printer.addMipsCode(new MipsCode(MipsOperation.mult, "$t2", addr1, ""));
                                    printer.addMipsCode(new MipsCode(MipsOperation.mflo, "$t2"));
                                    printer.addMipsCode(new MipsCode(MipsOperation.sll, "$t2", "$t2", "", 2));
                                    printer.addMipsCode(new MipsCode(MipsOperation.la, "$t0", "arr."+param_ret));
                                    printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t0", "$t0", "$t2"));

                                    printer.addMipsCode(new MipsCode(MipsOperation.sw, "$t0", "$sp", "", -4*j1));


                                } else {
                                    getAddr(param_ret, "$t0");
                                    String addr1 =getValue(param_arg1, "$t1", false);
                                    printer.addMipsCode(new MipsCode(MipsOperation.li, "$t2", "", "", Integer.parseInt(param_arg2) * 4));
                                    printer.addMipsCode(new MipsCode(MipsOperation.mult, "$t2", addr1, ""));
                                    printer.addMipsCode(new MipsCode(MipsOperation.mflo, "$t2"));
                                    printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t0", "$t0", "$t2"));
                                    printer.addMipsCode(new MipsCode(MipsOperation.sw, "$t0", "$sp", "", -4*j1));
                                }


                            }

                        }

                    }

                    paramPushList.clear();   // 清空实参存储的数组

                    ArrayList<String> curUseRegList = register.getCurUseRegList();
                    int listlen = curUseRegList.size();
                    int funclen = funcLengthMap.get(midcode.getRet());     // 获取所将调用函数的长度
                    // 将执行函数, 临时保存现场
                    printer.addMipsCode(new MipsCode(MipsOperation.addiu, "$sp", "$sp", "", -4*funclen -8 -4*listlen ));
                    printer.addMipsCode(new MipsCode(MipsOperation.sw, "$ra", "$sp", "", 4));
                    printer.addMipsCode(new MipsCode(MipsOperation.sw, "$fp", "$sp", "", 8));

                    for(int k1=0; k1 < listlen; k1++) {   // 把正在使用的临时寄存器现场也保存起来
                        printer.addMipsCode(new MipsCode(MipsOperation.sw, curUseRegList.get(k1), "$sp", "", 12 + 4*k1));
                    }

                    // 移动fp
                    printer.addMipsCode(new MipsCode(MipsOperation.addiu, "$fp", "$sp", "", 4*funclen +8 +4*listlen ));

                    printer.addMipsCode(new MipsCode(MipsOperation.jal, midcode.getRet()));   // 跳转到所调用的函数标签,执行函数

                    // 函数执行完,恢复现场
                    for(int k1 = listlen-1; k1>=0; k1--) {   // 恢复正在使用的临时寄存器现场
                        printer.addMipsCode(new MipsCode(MipsOperation.lw, curUseRegList.get(k1), "$sp", "", 12 + 4*k1 ));
                    }
                    // 恢复现场
                    printer.addMipsCode(new MipsCode(MipsOperation.lw, "$fp", "$sp", "", 8));
                    printer.addMipsCode(new MipsCode(MipsOperation.lw, "$ra", "$sp", "", 4));
                    printer.addMipsCode(new MipsCode(MipsOperation.addiu, "$sp", "$sp", "", 4*funclen + 8 + 4*listlen ));

                    continue;
                case RETURN:      // "RETURN x"  "RETURN null"   RETURN ret
                    if(isInMain) {
                        printer.addMipsCode(new MipsCode(MipsOperation.li, "$v0", "", "", 10));
                        printer.addMipsCode(new MipsCode(MipsOperation.syscall, ""));
                    } else {
                        if(midcode.getRet() != null) {   //  "RETURN x"
                            if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&'){  // 是寄存器名 "temp&1"
                                printer.addMipsCode(new MipsCode(MipsOperation.move, "$v0", register.getCurUseReg(midcode.getRet())));
                            } else {
                                getValue(midcode.getRet(), "$v0", false);
                            }
                        }

                        printer.addMipsCode(new MipsCode(MipsOperation.jr, "$ra"));
                    }

                    continue;
                case RETURN_VALUE:      //  "ret_value temp$1"   ret_value ret
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), "$v0"));
                    } else {
                        saveValue(midcode.getRet(), "$v0", false);
                    }
                    continue;

                case PLUS_OP:        // "a = b + c"    ret = arg1 + arg2
                    String arg1_pl =getValue(midcode.getArg1(), "$t0", false);
                    String arg2_pl = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.addu, register.allocReg(midcode.getRet()), arg1_pl, arg2_pl));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t2", arg1_pl, arg2_pl));
                        saveValue(midcode.getRet(), "$t2", false);
                    }

                    continue;
                case MINU_OP:     // "a = b - c"    ret = arg1 - arg2
                    String arg1_mi =getValue(midcode.getArg1(), "$t0", false);
                    String arg2_mi = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.subu, register.allocReg(midcode.getRet()), arg1_mi, arg2_mi));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.subu, "$t2", arg1_mi, arg2_mi));
                        saveValue(midcode.getRet(), "$t2", false);
                    }

                    continue;
                case MULT_OP:    //  ret = arg1 * arg2
                    String arg1_mu =getValue(midcode.getArg1(), "$t0", false);
                    String arg2_mu = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.mult, arg1_mu, arg2_mu, ""));
                        printer.addMipsCode(new MipsCode(MipsOperation.mflo, register.allocReg(midcode.getRet())));

                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.mult, "$t0", "$t1", ""));
                        printer.addMipsCode(new MipsCode(MipsOperation.mflo, "$t2"));
                        saveValue(midcode.getRet(), "$t2", false);
                    }

                    continue;
                case DIVI_OP:   // 除法是需要优化的 ※   ret = arg1 / arg2
                    divideOp(midcode, false);

                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), "$t2"));
                    } else {
                        saveValue(midcode.getRet(), "$t2", false);
                    }

                    continue;
                case MOD_OP:   // ret = arg1 % arg2   -->  a % b = a - a / b * b
                    divideOp(midcode, false);

                    String arg2_mod = getValue(midcode.getArg2(), "$t1", false);
                    printer.addMipsCode(new MipsCode(MipsOperation.mult, arg2_mod, "$t2", ""));
                    printer.addMipsCode(new MipsCode(MipsOperation.mflo, "$t2"));
                    printer.addMipsCode(new MipsCode(MipsOperation.subu, "$t2", "$t0", "$t2"));

                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), "$t2"));
                    } else {
                        saveValue(midcode.getRet(), "$t2", false);
                    }

                    continue;

                case ASSIGN:      // "a = x"     ret = arg1
                    String addr_assign = getValue(midcode.getArg1(), "$t0", false);

                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), addr_assign));
                    } else {
                        saveValue(midcode.getRet(), addr_assign, false);
                    }

                    continue;

                case PRINT:   // "prinf "hello""   "printf a"   printf ret
                    if(midcode.getArg1().equals("str")) {
                        String addr_str = mipsStringMap.get(midcode.getRet());

                        //printer.addMipsCode(new MipsCode(MipsOperation.li, "$a0", "", "", 0)); //
                        printer.addMipsCode(new MipsCode(MipsOperation.la, "$a0", addr_str));
                        printer.addMipsCode(new MipsCode(MipsOperation.li, "$v0", "", "", 4));  // 打印字符串
                        printer.addMipsCode(new MipsCode(MipsOperation.syscall, ""));

                    } else if(midcode.getArg1().equals("digit")){
                        if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {     // temp&1
                            //printer.addMipsCode(new MipsCode(MipsOperation.li, "$a0", "", "", 0)); //
                            printer.addMipsCode(new MipsCode(MipsOperation.move, "$a0", register.getCurUseReg(midcode.getRet())));
                        } else {
                            //printer.addMipsCode(new MipsCode(MipsOperation.li, "$a0", "", "", 0)); //
                            getValue(midcode.getRet(), "$a0", false);
                        }

                        printer.addMipsCode(new MipsCode(MipsOperation.li, "$v0", "", "", 1));  // 打印整数
                        printer.addMipsCode(new MipsCode(MipsOperation.syscall, ""));

                    }

                    continue;
                case GETINT:    // "getint a"      getint ret
                    printer.addMipsCode(new MipsCode(MipsOperation.li, "$v0", "", "", 5));
                    printer.addMipsCode(new MipsCode(MipsOperation.syscall, ""));

                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                        printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), "$v0"));
                    } else {
                        saveValue(midcode.getRet(), "$v0", false);
                    }

                    continue;

                case SETARR:     // "a[2] = x"    ret[arg1] = arg2
                    Symbol arr_symbol = symbolManager.getSymbolFromCurTable(midcode.getRet());

                    if(arr_symbol.isGlobalArray()) {    // 是全局数组
                        String addr_val =getValue(midcode.getArg2(), "$t0", false);    // 待赋值
                        String addr_dim = getValue(midcode.getArg1(), "$t1", false);   // 数组序号
                        printer.addMipsCode(new MipsCode(MipsOperation.sll, "$t1", addr_dim, "", 2));
                        printer.addMipsCode(new MipsCode(MipsOperation.la, "$t2", "arr." + midcode.getRet()));
                        printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t2", "$t1", "$t2"));

                        printer.addMipsCode(new MipsCode(MipsOperation.sw, addr_val, "$t2", "", 0));


                    } else {
                        String addr_val =getValue(midcode.getArg2(), "$t0", false);    // 待赋值
                        String addr_dim = getValue(midcode.getArg1(), "$t1", false);   // 数组序号
                        printer.addMipsCode(new MipsCode(MipsOperation.sll, "$t1", addr_dim, "", 2));


                        if(arr_symbol != null && arr_symbol.isPointer()) {
                            getValue(midcode.getRet(), "$t2", false);   //
                            printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t2", "$t2", "$t1"));
                            printer.addMipsCode(new MipsCode(MipsOperation.sw, addr_val, "$t2", "", 0));

                        } else {
                            int offset_arr = arr_symbol.getOffset();
                            if(!symbolManager.isGlobalSymbol(midcode.getRet())) {   // 非全局数组
                                printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t1", "$fp"));
                                printer.addMipsCode(new MipsCode(MipsOperation.sw, addr_val, "$t1", "", -4*offset_arr));  //

                            }
                            /*
                             printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t1", "$gp"));
                                printer.addMipsCode(new MipsCode(MipsOperation.sw, addr_val, "$t1", "", 4*offset_arr));  //

                             */
                        }
                    }


                    continue;
                case GETARR:    // "x = a[1]"    ret = arg1[arg2]
                    Symbol arr_symbol_1 = symbolManager.getSymbolFromCurTable(midcode.getArg1());
                    if(arr_symbol_1.isGlobalArray()) {    // 是全局数组
                        String addr_dim_1 =getValue(midcode.getArg2(), "$t0", false);  // 数组序号
                        printer.addMipsCode(new MipsCode(MipsOperation.sll, "$t0", addr_dim_1, "", 2));
                        printer.addMipsCode(new MipsCode(MipsOperation.la, "$t1", "arr." + midcode.getArg1()));
                        printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t0", "$t1"));


                        printer.addMipsCode(new MipsCode(MipsOperation.lw, "$t2", "$t1", "", 0));

                        if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                            printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), "$t2"));
                        } else {
                            saveValue(midcode.getRet(), "$t2", false);
                        }

                    } else {
                        String addr_dim_1 =getValue(midcode.getArg2(), "$t0", false);
                        printer.addMipsCode(new MipsCode(MipsOperation.sll, "$t0", addr_dim_1, "", 2));

                        if(arr_symbol_1 != null && arr_symbol_1.isPointer()) {
                            getValue(midcode.getArg1(), "$t1", false);
                            printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t1", "$t0"));
                            printer.addMipsCode(new MipsCode(MipsOperation.lw, "$t2", "$t1", "", 0));
                        } else {
                            int offset_arr_1 = arr_symbol_1.getOffset();
                            if(symbolManager.isGlobalSymbol(midcode.getArg1())) {
                                printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t0", "$gp"));
                                printer.addMipsCode(new MipsCode(MipsOperation.lw, "$t2", "$t1", "", 4*offset_arr_1));
                            } else {
                                printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t0", "$fp"));
                                printer.addMipsCode(new MipsCode(MipsOperation.lw, "$t2", "$t1", "", -4*offset_arr_1));
                            }

                        }

                        if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {
                            printer.addMipsCode(new MipsCode(MipsOperation.move, register.allocReg(midcode.getRet()), "$t2"));
                        } else {
                            saveValue(midcode.getRet(), "$t2", false);
                        }
                    }



                    continue;

                case BZ:       // if arg1 == 0 then goto ret
                    String addr_bz = getValue(midcode.getArg1(), "$t0", false);
                    //printer.addMipsCode(new MipsCode(MipsOperation.li, "$t1", "", "", 0));
                    printer.addMipsCode(new MipsCode(MipsOperation.li, "$t1", "", "", 0));
                    printer.addMipsCode(new MipsCode(MipsOperation.beq, midcode.getRet(), addr_bz, "$t1"));   // 与0比较
                    continue;
                case BNZ:
                    continue;
                case JUMP_LABEL:      //  ret:
                    printer.addMipsCode(new MipsCode(MipsOperation.label, midcode.getRet()));
                    continue;
                case GOTO:     // goto Jump_ret:
                    printer.addMipsCode(new MipsCode(MipsOperation.j, midcode.getRet()));
                    continue;

                case LSS:      // ret = arg1 < arg2
                    String addr_lss1 = getValue(midcode.getArg1(), "$t0", false);
                    String addr_lss2 = getValue(midcode.getArg2(), "$t1", false);   // 分清arg1arg2
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.slt, register.allocReg(midcode.getRet()), addr_lss1, addr_lss2));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.slt, "$t2", addr_lss1, addr_lss2));
                        saveValue(midcode.getRet(), "$t2", true);
                    }

                    continue;
                case LEQ:    // ret = arg1 <= arg2
                    String addr_leq1 = getValue(midcode.getArg1(), "$t0", false);
                    String addr_leq2 = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.sle, register.allocReg(midcode.getRet()), addr_leq1, addr_leq2));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.sle, "$t2", addr_leq1, addr_leq2));
                        saveValue(midcode.getRet(), "$t2", true);
                    }
                    continue;
                case GRE:   // ret = arg1 > arg2
                    String addr_gre1 = getValue(midcode.getArg1(), "$t0", false);
                    String addr_gre2 = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.sgt, register.allocReg(midcode.getRet()), addr_gre1, addr_gre2));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.sgt, "$t2", addr_gre1, addr_gre2));
                        saveValue(midcode.getRet(), "$t2", true);
                    }
                    continue;
                case GEQ:   // ret = arg1 >= arg2
                    String addr_geq1 = getValue(midcode.getArg1(), "$t0", false);
                    String addr_geq2 = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.sge, register.allocReg(midcode.getRet()), addr_geq1, addr_geq2));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.sge, "$t2", addr_geq1, addr_geq2));
                        saveValue(midcode.getRet(), "$t2", true);
                    }
                    continue;

                case EQL:   // ret = arg1 == arg2
                    String addr_eql1 = getValue(midcode.getArg1(), "$t0", false);
                    String addr_eql2 = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.seq, register.allocReg(midcode.getRet()), addr_eql1, addr_eql2));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.seq, "$t2", addr_eql1, addr_eql2));
                        saveValue(midcode.getRet(), "$t2", true);
                    }
                    continue;
                case NEQ:   // ret = arg1 != arg2
                    String addr_neq1 = getValue(midcode.getArg1(), "$t0", false);
                    String addr_neq2 = getValue(midcode.getArg2(), "$t1", false);
                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.sne, register.allocReg(midcode.getRet()), addr_neq1, addr_neq2));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.sne, "$t2", addr_neq1, addr_neq2));
                        saveValue(midcode.getRet(), "$t2", true);
                    }
                    continue;

                case SLL:    // ret = arg1 << 4
                    String addr_sll1 = getValue(midcode.getArg1(), "$t0", false);

                    if(midcode.getRet().length() >= 5 && midcode.getRet().charAt(4) == '&') {   // 是否是寄存器
                        printer.addMipsCode(new MipsCode(MipsOperation.sll, register.allocReg(midcode.getRet()), addr_sll1, "", Integer.parseInt(midcode.getArg2())));
                    } else {
                        printer.addMipsCode(new MipsCode(MipsOperation.sll, "$t2", addr_sll1, "", Integer.parseInt(midcode.getArg2())));
                        saveValue(midcode.getRet(), "$t2", true);
                    }


                    continue;



                default:
                    continue;

            }


        }



    }



    public void divideOp(Midcode midcode, boolean isMod) {    // 有符号除法
        String arg1_div =getValue(midcode.getArg1(), "$t0", false);   // 被除数
        printer.addMipsCode(new MipsCode(MipsOperation.move, "$t0", arg1_div));

        if(Optimizer.getInstance().isTurnOn() && !isMod) {     // 开启优化
            Pattern pattern = Pattern.compile("^[-\\+]*[0-9]*$");
            boolean isNumber =  pattern.matcher(midcode.getArg2()).matches();
            if(isNumber) {    // 除数是常数

                int divisor_1 = Integer.parseInt(midcode.getArg2());

                 //x÷d
                int divisor = Math.abs(divisor_1);

                // choose multipilier
                final int N = 32;
                int l = (int) Math.ceil(Math.log(divisor) / Math.log(2));
                int sh_post = l;
                long m_low = (long) Math.floor(Math.pow(2, N + l) / divisor);
                long m_high = (long) Math.floor((Math.pow(2, N + l) + Math.pow(2, N + l - 31)) / divisor);

                while ((Math.floor(m_low / 2) < Math.floor(m_high / 2)) && sh_post > 0) {
                    m_low = (long) Math.floor(m_low / 2);
                    m_high = (long) Math.floor(m_high / 2);
                    sh_post -= 1;
                }
                //

                if(divisor == 1) {
                    printer.addMipsCode(new MipsCode(MipsOperation.move, "$t2", "$t0"));
                } else if(divisor > 0 && (divisor & (divisor - 1)) == 0) {    // 2的幂次
                    int mi = (int) (Math.log(divisor) / Math.log(2));   //
                    printer.addMipsCode(new MipsCode(MipsOperation.sra, "$t1", "$t0", "", (mi - 1)));
                    printer.addMipsCode(new MipsCode(MipsOperation.srl, "$t1", "$t1", "", (32 - mi)));
                    printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t1", "$t0"));
                    printer.addMipsCode(new MipsCode(MipsOperation.sra, "$t2", "$t1", "", mi));

                } else if(m_high < Math.pow(2, 31)) {
                    printer.addMipsCode(new MipsCode(MipsOperation.li, "$t1", "", "", m_high, true));
                    printer.addMipsCode(new MipsCode(MipsOperation.mult, "$t0", "$t1"));
                    printer.addMipsCode(new MipsCode(MipsOperation.mfhi, "$t1"));
                    printer.addMipsCode(new MipsCode(MipsOperation.sra, "$t1", "$t1", "", sh_post));
                    printer.addMipsCode(new MipsCode(MipsOperation.slti, "$t2", "$t0", "", 0));
                    printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t2", "$t2", "$t1"));

                } else {
                    printer.addMipsCode(new MipsCode(MipsOperation.li, "$t1", "", "", (int) (m_high - Math.pow(2, 32))));
                    printer.addMipsCode(new MipsCode(MipsOperation.mult, "$t0", "$t1"));
                    printer.addMipsCode(new MipsCode(MipsOperation.mfhi, "$t1"));
                    printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t1", "$t1", "$t0"));
                    printer.addMipsCode(new MipsCode(MipsOperation.sra, "$t1", "$t1", "", sh_post));
                    printer.addMipsCode(new MipsCode(MipsOperation.slti, "$t2", "$t0", "", 0));
                    printer.addMipsCode(new MipsCode(MipsOperation.addu, "$t2", "$t2", "$t1"));
                }

                if(divisor_1 < 0) {
                    printer.addMipsCode(new MipsCode(MipsOperation.subu, "$t2", "$zero", "$t2"));
                }



                return;
            } else {     // 除数不是常数
                String arg2_div = getValue(midcode.getArg2(), "$t1", false);
                printer.addMipsCode(new MipsCode(MipsOperation.div, "$t0", arg2_div, ""));
                printer.addMipsCode(new MipsCode(MipsOperation.mflo, "$t2"));
                return;
            }


        }

        String arg2_div = getValue(midcode.getArg2(), "$t1", false);
        printer.addMipsCode(new MipsCode(MipsOperation.div, "$t0", arg2_div, ""));
        printer.addMipsCode(new MipsCode(MipsOperation.mflo, "$t2"));

        return;


    }



    public String getValue(String varName, String regName, boolean isCreateSymbol) {   // (从内存)取值
        if(varName.length() >= 5 && varName.charAt(4) == '&'){  // 是寄存器名 "temp&1"
            String addr = this.register.getCurUseReg(varName);
            return addr;       // 返回temp&1占用的临时寄存器
        }

        if(Character.isDigit(varName.charAt(0)) || varName.charAt(0) == '-') {   // 是数字 (包含负号)
            printer.addMipsCode(new MipsCode(MipsOperation.li, regName, "", "", Integer.parseInt(varName)));
            return regName;     // 给指定寄存器传值并返回该寄存器
        } else {
            if(isCreateSymbol) {      // 需要创建符号（是变量名）
                Symbol newsymbol = new Symbol(varName, funcPointer);
                newsymbol.setIsVar(true);
                boolean success = symbolManager.addSymbol(newsymbol);
                if(success) {
                    funcPointer = funcPointer +1;   //
                }
            }

            int offset = symbolManager.getSymbolFromCurTable(varName).getOffset();

            if(symbolManager.isGlobalSymbol(varName)) {
                // 是全局变量的取值
                printer.addMipsCode(new MipsCode(MipsOperation.lw, regName, "$gp", "", 4*offset));
            } else {
                printer.addMipsCode(new MipsCode(MipsOperation.lw, regName, "$fp", "", -4*offset));
            }

            return regName;
        }
    }


    public String getAddr(String varName, String regName) {     // 获取地址
        if(Character.isDigit(varName.charAt(0)) || varName.charAt(0) == '-') {   // 是数字 (包含负号)
            printer.addMipsCode(new MipsCode(MipsOperation.li, regName, "", "", Integer.parseInt(varName)));
            return regName;
        }

        Symbol symbol = symbolManager.getSymbolFromCurTable(varName);

        if(symbol != null && symbol.isArray()) {   // 可能遇到临时寄存器temp&5
            if(symbol.isPointer()) {  // 函数形参要求int a[][2], 此时传入实参 b    <int [1][2] b>
                getValue(varName, regName, false);
            } else {
                int offset = symbol.getOffset();
                if(symbolManager.isGlobalSymbol(varName)) {
                    printer.addMipsCode(new MipsCode(MipsOperation.addiu, regName, "$gp", "", 4*offset));
                } else {
                    printer.addMipsCode(new MipsCode(MipsOperation.addiu, regName, "$fp", "", -4*offset));
                }
                return regName;
            }

        } else {
            String addr = getValue(varName, regName, false);
            return addr;
        }

        String addr = getValue(varName, regName, false);
        return addr;
    }


    public void saveValue(String varName, String regName, boolean isCreateSymbol) {      // (向内存)存值
        if(isCreateSymbol) {   // 向符号表添加符号
            Symbol newsymbol = new Symbol(varName, funcPointer);
            newsymbol.setIsVar(true);
            boolean success = symbolManager.addSymbol(newsymbol);
            if(success) {
                funcPointer = funcPointer +1;   //
            }
        }

        int offset = symbolManager.getSymbolFromCurTable(varName).getOffset();

        if(symbolManager.isGlobalSymbol(varName)) {
            // 是全局变量的存值
            printer.addMipsCode(new MipsCode(MipsOperation.sw, regName, "$gp", "", 4*offset));
        } else {
            printer.addMipsCode(new MipsCode(MipsOperation.sw, regName, "$fp", "", -4*offset));
        }

    }


    public void createSymbolWithLen(String ident, int len, boolean isInit, int arr_type) {     // 创建符号 (带有长度, 适用于数组)
        if(symbolManager.isCurGlobalTable()) {    // 是全局数组

            Symbol arraysymbol = new Symbol(ident, this.funcPointer);
            arraysymbol.setIsArray(true);    //
            arraysymbol.setIsGlobalArray(true);   //
            arraysymbol.setArrType(arr_type);     //

            symbolManager.addSymbol(arraysymbol);
            //this.funcPointer = funcPointer + len;


        } else {
            funcPointer = funcPointer + (len-1);
            Symbol arraysymbol = new Symbol(ident, this.funcPointer);
            arraysymbol.setIsArray(true);    //
            arraysymbol.setIsGlobalArray(false);  //
            arraysymbol.setArrType(arr_type);   //


            symbolManager.addSymbol(arraysymbol);
            funcPointer = funcPointer + 1;   //
        }

    }





















}
