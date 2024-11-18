package MIPS;

import java.util.ArrayList;

public class MipsCode {
    private MipsOperation op;
    private String ret;
    private String arg1;
    private String arg2;
    private int immediateNum;    // 立即数

    private long longImmedi;
    private boolean isLong = false;

    private ArrayList<Integer> elements = new ArrayList<>();
    private boolean isInitVal = false;

    public MipsCode(MipsOperation op, String ret) {
        this.op = op;
        this.ret = ret;
        this.arg1 = null;
        this.arg2 = null;
    }

    public MipsCode(MipsOperation op, String ret, String arg1) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = null;

    }

    public MipsCode(MipsOperation op, String ret, String arg1, String arg2) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public MipsCode(MipsOperation op, String ret, String arg1, String arg2, int immediateNum) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.immediateNum = immediateNum;
    }


    //
    public MipsCode(MipsOperation op, String ret, String arg1,  String arg2, long immediateNum, boolean isLong) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.longImmedi = immediateNum;
        this.isLong = isLong;
    }

    public MipsCode(MipsOperation op, String ret, String arg1, ArrayList<Integer> elements) {
        this.op = op;
        this.ret = ret;
        this.arg1 = arg1;
        this.elements = elements;
    }




    @Override
    public String toString() {     // 注意字符串中的空格
        switch (this.op) {
            case add:
                return "add " + ret +"," + arg1 + "," + arg2;
            case addu:
                return "addu " + ret +"," + arg1 + "," + arg2;
            case addi:
                return "addi " + ret + "," + arg1 + "," + immediateNum;
            case addiu:
                return "addiu " + ret + "," + arg1 + "," + immediateNum;

            case sub:
                return "sub " + ret +"," + arg1 + "," + arg2;
            case subu:
                return "subu " + ret + "," + arg1 + "," + arg2;
            case mult:
                return "mult " +ret + "," + arg1;
            case multu:
                return "multu " + ret + "," + arg1;
            case div:
                return "div " + ret + "," + arg1;

            case move:
                return "move " + ret + "," + arg1;

            case sll:
                return "sll " + ret + "," + arg1 + "," + immediateNum;    // 最后一个是立即数
            case srl:
                return "srl " + ret + "," + arg1 + "," + immediateNum;
            case sra:
                return "sra " + ret + "," + arg1 + "," + immediateNum;


            case bgez:
                return "bgez " + ret + "," + arg1;

            case sle:
                return "sle " + ret + "," + arg1 + "," + arg2;
            case sne:
                return "sne " + ret + "," + arg1 + "," + arg2;
            case sgt:
                return "sgt " + ret + "," + arg1 + "," + arg2;
            case sge:
                return "sge " + ret + "," + arg1 + "," + arg2;
            case slt:
                return "slt " + ret + "," + arg1 + "," + arg2;
            case slti:
                return "slti " + ret + "," + arg1 + "," + immediateNum;

            case seq:
                return "seq " + ret + "," + arg1 + "," + arg2;

            case beq:
                return "beq " + arg1 + "," + arg2 + "," + ret;  // 注意顺序


            case li:
                if(isLong) {
                    return "li " + ret + "," + longImmedi;
                } else {
                    return "li " + ret + "," + immediateNum;
                }

            case la:
                return "la " + ret + "," + arg1;

            case mflo:
                return "mflo " + ret;
            case mfhi:
                return "mfhi " + ret;

            case lw:
                if(arg2 != null) {
                    return "lw " + ret + "," + immediateNum + "(" + arg1 + ")";
                } else {
                    return "lw " + ret + "," + arg1;
                }

            case sw:
                if(arg2 != null) {
                    return "sw " + ret + "," + immediateNum + "(" + arg1 + ")";
                } else {
                    return "sw " + ret + "," + arg1;
                }


            case j:
                return "j " + ret;
            case jal:
                return "jal " + ret;
            case jr:
                return "jr " + ret;

            case label:
                return "\n" + ret + ":";

            case syscall:
                return "syscall";

            case asciiz:
                return ret + ": .asciiz \"" + arg1 + "\"";       // 引号打起来   str_1: .asciiz "hello"

            case data:
                return ".data";
            case text:
                return "\n" + ".text";

            case word:
                boolean flag = false;
                for(int i=0; i<elements.size(); i++) {
                    if(i != 0) {
                        flag = true;
                        break;
                    }
                }
                if(flag) {    // 不是全0赋值 (有不含0的数)
                    StringBuilder sb = new StringBuilder();
                    for(int j=0; j<elements.size(); j++) {
                        sb.append(elements.get(j));
                        sb.append(",");
                    }
                    sb.delete(sb.length()-1, sb.length());    // 删除最后一个","
                    return ret + ": .word " + sb.toString();


                } else {
                    return ret + ": .word 0:" + arg1;       // arr.a: .word 0:1000
                }






            default:
                return null;
        }

    }





}
