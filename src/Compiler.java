import AST.Node;
import LEXER.Lexer;
import LEXER.Token;
import MIPS.Mips;
import MidCode.Midcode;
import OPTIMIZE.Optimizer;
import PARSER.Parser;
import SYMBOL.SymbolManager;
import tools.Printer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Compiler {

    public static void main(String[] args) throws FileNotFoundException {     /* 编译器程序运行的总入口*/
        String filepath = "./testfile.txt";          // 读入外部txt文件内容
        StringBuilder fulltext = new StringBuilder();

        try {
            File file = new File(filepath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while((line = reader.readLine()) != null){
                fulltext.append(line).append("\n");    // 使用readLine读取文本时，会读取一行文本，但不包括行尾的换行符(\n)和回车符(\r)
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String lexerStr = fulltext.toString();


        Printer printer = new Printer(false, false, true);
                                    // 词法分析输出,            语法分析输出,         错误处理输出,


        // 词法分析程序
        Lexer lexer = new Lexer(lexerStr);

        while(lexer.next()) {
            // pass
        }

        ArrayList<Token> all_tokens = new ArrayList<>();
        all_tokens = lexer.getAll_tokens();

        /* while(lexer.next()) {
            try {
                FileWriter fw = new FileWriter("./output.txt", true);  // 向文件中追加内容 (若文件不存在才会创建)
                if(!lexer.getLexType().equals("NOTE")) {
                    String content = lexer.getLexType() + " " + lexer.getToken() + "\n";    // 换行-Win:\r\n  Linux:\n  Mac:\n
                    fw.write(content);    // 写入内容
                    fw.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } */

        // 语法分析程序
        Parser parser = new Parser(all_tokens, printer);
        Node ret_AST = parser.parseCompUnit();      //


        // 错误处理 & 符号表管理

        SymbolManager err_symbolManager = new SymbolManager();    // 符号表 (错误处理结束后，符号表又变成空)

        ret_AST.checkError(printer, err_symbolManager);                // 层层遍历下降，建符号表&检查语义错误
        printer.printError();                       // 输出错误信息到文件中


        if(printer.isErrorMapEmpty()) {       // true表示没有错误信息
            SymbolManager symbolManager = new SymbolManager();      // 中间代码生成过程中重建符号表

            // 代码优化
            //System.out.println( Optimizer.getInstance().isTurnOn());
            Optimizer.getInstance().setTurnOnOptimizer(true);   // 设置 开启/关闭 优化
            //System.out.println(Optimizer.getInstance().isTurnOn());


            // 中间代码生成（四元式）
            ret_AST.genMidCode(printer, symbolManager);

            printer.setTurnOnMidcode(true);   // 打开中间代码的文件输出
            printer.printMidCode();
            ArrayList<Midcode> midcodesList = printer.getMidCodes();
            ArrayList<String> mipsString = printer.getMipsString();//

            HashMap<String, ArrayList<Integer>> mipsGlobalArr = printer.getMipsGlobalArray();  //

            // 目标代码生成（MIPS）
            // 又要重建符号表

            Mips mips = new Mips(midcodesList, mipsString, mipsGlobalArr, printer, symbolManager);
            mips.computeFuncLength();  //
            mips.genMips();            //

            Printer printer_final = mips.getPrinter();
            printer_final.setTurnOnMips(true);    // 打开Mips文件输出
            printer_final.printMips();







        }




    }





}
