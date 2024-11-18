package tools;

import LEXER.Token;
import MidCode.Midcode;
import MIPS.MipsCode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Printer {

    private FileOutputStream outfile;
    private FileOutputStream error_outfile;
    private FileOutputStream mips_outfile;

    private boolean isprint;         // 是否输出文件打印内容 [true-是  false-否]

    private boolean turn_on_lexer;    // 是否打开词法分析的文件输出
    private boolean turn_on_parser;   // 是否打开语法分析的文件输出

    private boolean turn_on_error;    // 是否打开错误处理的文件输出
    private boolean turn_on_midcode;   // 是否打开中间代码的文件输出
    private boolean turn_on_mips;      // 是否打开Mips代码的文件输出


    private HashMap<Integer, String> error_map;

    private ArrayList<Midcode> midcodesList;     // 生成的所有中间代码
    private ArrayList<String> mips_string;       // printf时会打印的除"%d"外的被分隔开的所有字符串

    private HashMap<String, ArrayList<Integer>> mips_global_array = new HashMap<>();

    private ArrayList<MipsCode> mipsCodesList;   // 生成的Mips代码

    public Printer(boolean is_turn_on_lexer, boolean is_turn_on_parser, boolean is_turn_on_error) throws FileNotFoundException{
        this.outfile = new FileOutputStream("output.txt");
        this.error_outfile = new FileOutputStream("error.txt");
        this.mips_outfile = new FileOutputStream("mips.txt");

        this.isprint = true;

        this.turn_on_lexer = is_turn_on_lexer;
        this.turn_on_parser = is_turn_on_parser;
        this.turn_on_error = is_turn_on_error;
        this.turn_on_midcode = false;     // 默认关闭
        this.turn_on_mips = false;        // 默认关闭

        this.error_map = new HashMap<>();

        this.midcodesList = new ArrayList<>();
        this.mips_string = new ArrayList<>();
        this.mipsCodesList = new ArrayList<>();
    }

    public void setIsprint(boolean bool_value){
        this.isprint = bool_value;
    }



    public void setTurnOnMidcode(boolean bool_value) {
        this.turn_on_midcode = bool_value;
    }

    public void setTurnOnMips(boolean bool_value) {
        this.turn_on_mips = bool_value;
    }

    // 语法分析-指定输出内容 （直接输出终结符）
    public void printToken(Token token) {
        if(this.isprint && this.turn_on_parser) {
            String content = token.getLexType() + " " + token.getTokenname() + "\n";
            try {
                outfile.write((content).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    // 语法分析-指定输出内容（对于非终结符要输出 <语法成分>）
    public void printSyntax(String syntax_type) {
        if(this.isprint && this.turn_on_parser && !this.turn_on_lexer) {
            String content = "<" + syntax_type + ">\n";
            try {
                outfile.write((content).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }

    // 错误处理-添加待输出内容（等待最后按行号从小到大排序输出）
    public void addErrorMessage(int line, ErrorType error_type) {
        if(this.turn_on_error) {
            this.error_map.put(line, error_type.name());
        }
    }

    public boolean isErrorMapEmpty() {
        if(this.error_map.isEmpty()) {
            return true;        // true表示没有错误信息
        } else {
            System.out.println("There are errors in the testfile!");
            return false;
        }
    }

    // 错误处理-排序后真正向文件打印内容
    public void printError() {
        if(this.turn_on_error) {
            Object[] arr = error_map.keySet().toArray();
            Arrays.sort(arr);             // 行号从小到大
            for (Object line_i : arr) {
                String content = line_i + " " + error_map.get((Integer) line_i) + "\n";
                try {
                    this.error_outfile.write((content).getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    //
    public void addMidCodeList(Midcode midcode) {    //
        this.midcodesList.add(midcode);
    }

    public void printMidCode() {             // 输出产生的中间代码
        if(this.turn_on_midcode) {
            for(Midcode midcode: midcodesList) {
                String content = midcode.toString() + "\n";
                try{
                    this.outfile.write((content).getBytes());    // 向"output.txt"输出
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

        }

    }

    public ArrayList<Midcode> getMidCodes() {
        return this.midcodesList;
    }

    public void addMipsString(String str) {
        this.mips_string.add(str);
    }


    public ArrayList<String> getMipsString() {    //
        return this.mips_string;
    }


    public void addMipsGlobalArray(String ident, ArrayList<Integer> elements) {
        this.mips_global_array.put(ident, elements);
    }

    public HashMap<String, ArrayList<Integer>> getMipsGlobalArray() {
        return this.mips_global_array;
    }


    //
    public void addMipsCode(MipsCode mipsCode) {
        this.mipsCodesList.add(mipsCode);
    }

    public void printMips() {             // 输出mips文件
        if(this.turn_on_mips) {
            for(MipsCode mipsCode: this.mipsCodesList) {
                String content = mipsCode.toString() + "\n";     //
                try{
                    this.mips_outfile.write((content).getBytes());    // 向"mips.txt"输出
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }

        }


    }


}
