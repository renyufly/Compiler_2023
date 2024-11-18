package MidCode;

public class Midcode {
    // 四元式是形如(op, arg1, arg2, result)的指令格式，其中op是运算符，arg1和arg2是操作数，result是运算结果

    private Operation op;
    private String arg1;
    private String arg2;
    private String ret;
    private boolean isInit = false;

    // op, , , ret
    public Midcode(Operation op, String ret) {
        this.op = op;
        this.ret = ret;
        this.arg1 = null;
        this.arg2 = null;
    }

    // op, arg1, , ret
    public Midcode(Operation op, String ret, String arg1) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = null;
    }

    // op, arg1, arg2, ret
    public Midcode(Operation op, String ret, String arg1, String arg2) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    //  针对变量定义是否已赋值
    public Midcode(Operation op, String ret, String arg1, String arg2, boolean isInit) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.isInit = isInit;
    }


    public Operation getOp() {
        return this.op;
    }

    public String getRet() {
        return this.ret;
    }

    public String getArg1() {
        return this.arg1;
    }

    public String getArg2() {
        return this.arg2;
    }

    public boolean getIsInit() {
        return this.isInit;
    }

    @Override
    public String toString() {
        switch (this.op) {
            case VAR:       // "var int a"    "var int a = x"
                if(arg1 == null) {
                    return "var int " + ret;
                } else {
                    return "var int " + ret + " = " + arg1;
                }
            case ARRAY:     // "array int a[2]"  "array int b[2][3]"
                if(this.isInit) {
                    if(arg2 == null) {
                        return "array int " + ret + "[" + arg1 + "]" + " <has InitVal>";
                    } else {
                        return "array int " + ret + "[" + arg1 + "][" + arg2 + "]" + " <has InitVal>";
                    }
                } else {
                    if(arg2 == null) {
                        return "array int " + ret + "[" + arg1 + "]";
                    } else {
                        return "array int " + ret + "[" + arg1 + "][" + arg2 + "]";
                    }
                }

            case CONST:     // "const int a = 233"
                return "const int " + ret + " = " + arg1;

            case LABEL:     //  " <label> 1 start"   " <label> 1 end"
                return "    <label> " + ret + " " + arg1 + ":";
            case MAIN_BEGIN:    //  " main_begin"
                return " main_begin";
            case MAIN_END:      // " main_end"
                return " main_end";

            case FUNC:            // "void foo()"  "int func()"
                return arg1 + " " + ret + "()";
            case PARAM_DEF:       // "param_def int a" "param_def int b[]"  "param_def int c[][2]"
                if(arg1.equals("0")) {
                    return "param_def int " + ret;
                } else if(arg1.equals("1")) {
                    return "param_def int " + ret + "[]";
                } else {
                    return "param_def int " + ret + "[][" + arg2 + "]";
                }
            case PARAM_PUSH:      //   "param_push a"  "param_push b[1][2]"
                if(arg1 == null) {
                    return "param_push " + ret;
                } else {
                    return "param_push " + ret + "[" + arg1 + "][" + arg2 + "]";
                }
            case CALL:        // "call foo"
                return "call " + ret;
            case RETURN:       // "RETURN x"  "RETURN null"
                if(ret == null) {
                    return "RETURN null";
                } else {
                    return "RETURN " + ret;
                }
            case RETURN_VALUE:     //  "ret_value temp$1"
                return "ret_value " + ret;

            case PLUS_OP:       // "a = b + c"
                return ret + " = " + arg1 + " + " + arg2;
            case MINU_OP:       // "a = b - c"
                return ret + " = " + arg1 + " - " + arg2;
            case MULT_OP:       // "a = b * c"
                return ret + " = " + arg1 + " * " + arg2;
            case DIVI_OP:       // "a = b / c"
                return ret + " = " + arg1 + " / " + arg2;
            case MOD_OP:        // "a = b % c"
                return ret + " = " + arg1 + " % " + arg2;

            case ASSIGN:        // "a = x"
                return ret + " = " + arg1;

            case PRINT:         // "prinf "hello""   "printf a"
                if(arg1.equals("str")) {
                    return "printf " + "\"" + ret + "\"";
                } else {
                    return "printf " + ret;
                }
            case GETINT:       // "getint a"
                return "getint " + ret;

            case SETARR:       // "a[2] = x"
                return ret + "[" + arg1 + "] = " + arg2;
            case GETARR:       // "x = a[1]"
                return ret + " = " + arg1 + "[" + arg2 + "]";


            case BZ:           // be zero 跳转
                return "if " + arg1 + " == 0 then goto " + ret;
            case BNZ:
                return "if " + arg1 + " != 0 then goto " + ret;
            case JUMP_LABEL:
                return "  <" + ret + ">:";
            case GOTO:
                return "goto " + ret;

            case LSS:          // <
                return ret + " = " + arg1 + " < " + arg2;
            case LEQ:          // <=
                return ret + " = " + arg1 + " <= " + arg2;
            case GRE:          // >
                return ret + " = " + arg1 + " > " + arg2;
            case GEQ:          // >=
                return ret + " = " + arg1 + " >= " + arg2;

            case EQL:          // ==
                return ret + " = " + arg1 + " == " + arg2;
            case NEQ:          // !=
                return ret + " = " + arg1 + " != " + arg2;


            case SLL:        //  ret = arg1 << arg2
                return ret + " = " + arg1 + " << " + arg2;



            default:
                return null;
        }


    }



}
