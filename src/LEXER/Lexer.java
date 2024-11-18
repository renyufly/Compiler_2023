package LEXER;

import java.util.ArrayList;
import java.util.HashMap;


public class Lexer {
    private final String source_str;    //输入编译器的源程序字符串
    private int source_len;           // 源程序字符串的长度
    private int line;    // 行号：当前解析位置在源程序中的行号，默认初始为1
    private int p;   //指向下一个待解析的单词的起始字符，初始化时指向source的第一个字符
    private ArrayList<Token> all_tokens;   //分出的所有词(每个词含对应属性)

    private String curToken;        // 当前词
    private String curLexType;      // 当前词类型
    private int curNumValue;        // 当前词对应的数字值（数字字符串转真实数字）
    private int curLine;            // 当前词所在行号

    private HashMap<String, LexType> reserver_hashmap;  // 存放保留字/关键字便于查找


    public Lexer(String str){
        source_str = str;
        source_len = str.length();
        line = 1;
        p = 0;
        all_tokens = new ArrayList<>();
        reserver_hashmap = new HashMap<>();
        this.reserver_hashmap.put("main", LexType.MAINTK);
        this.reserver_hashmap.put("const", LexType.CONSTTK);
        this.reserver_hashmap.put("int", LexType.INTTK);
        this.reserver_hashmap.put("break", LexType.BREAKTK);
        this.reserver_hashmap.put("continue", LexType.CONTINUETK);
        this.reserver_hashmap.put("if", LexType.IFTK);
        this.reserver_hashmap.put("else", LexType.ELSETK);
        this.reserver_hashmap.put("for", LexType.FORTK);
        this.reserver_hashmap.put("getint", LexType.GETINTTK);
        this.reserver_hashmap.put("printf", LexType.PRINTFTK);
        this.reserver_hashmap.put("return", LexType.RETURNTK);
        this.reserver_hashmap.put("void", LexType.VOIDTK);
    }

    public LexType reserver(String token){    //查找保留字/关键字
        if(this.reserver_hashmap.containsKey(token)) {
            return this.reserver_hashmap.get(token);
        } else {
            return LexType.IDENFR;
        }

    }

    public boolean next(){     //处理下一个单词
        if(p >= source_len){
            return false;     // 读到整个字符串的末尾了,结束
        }

        StringBuilder token_sb = new StringBuilder();   // 当前存放单词的字符串

        while(source_str.charAt(p) == ' ' || source_str.charAt(p) == '\t' || source_str.charAt(p) == '\r' ||
              source_str.charAt(p) == '\n') {      // 读取字符，跳过空格，\n，\r，\t
            if(source_str.charAt(p) == '\n') {
                line++;        // 换行后行号+1
            }
            this.p++;
            if(p >= source_len) {
                return false;
            }
        }

        char ch = source_str.charAt(this.p);    // ch是当前循环下判断的第一个字符

        if(Character.isUpperCase(ch) || Character.isLowerCase(ch) || ch == '_') {    // 判断关键字or标识符
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = source_str.charAt(this.p);
            }

            while((p < source_len) && (Character.isDigit(ch) || Character.isLetter(ch) || ch == '_')) {
                token_sb.append(ch);
                this.p++;
                if(p < source_len) {
                    ch = source_str.charAt(this.p);
                }
            }
            // 此时对标识符/关键字的提取结束，p指向下一个带解析起始位
            LexType type = reserver(token_sb.toString());
            this.curLexType = type.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);

        } else if(Character.isDigit(ch)) {     // 判断是数值常量
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);
            }
            while(Character.isDigit(ch) && (p<source_len)) {      // ※ 注意之后的错误处理可能出现对前导0数的错误的特判 ※
                token_sb.append(ch);
                this.p++;
                if(p < source_len) {
                    ch = this.source_str.charAt(this.p);
                }

            }

            this.curLexType = LexType.INTCON.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            this.curNumValue = Integer.parseInt(this.curToken);
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine, this.curNumValue);
            this.all_tokens.add(tmp_token);

        } else if(ch == '"') {                // 判断是字符串
            token_sb.append(ch);
            this.p++;
            while((p < source_len) && (this.source_str.charAt(p) != '"')){
                ch = this.source_str.charAt(this.p);
                token_sb.append(ch);
                this.p++;
            }
            // 此时要么文件结束要么p指向字符"
            if(p < source_len) {
                token_sb.append(this.source_str.charAt(p));
                this.p++;
            }

            this.curLexType = LexType.STRCON.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);

        } else if(ch == '/'){        // 判断是 /-除法、/*-注释、//-注释   【需要预读：要保证p不会溢出】
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);
            }
            if((p < source_len) && (ch == '/')) {           // 判断是 //-单行注释:直到换行符结束，不包括换行符。
                token_sb.append(ch);
                this.p++;
                while((p < source_len) && (this.source_str.charAt(this.p) != '\n')) {
                    this.p++;                              // 注释内容直接忽略
                }
                if(p < source_len) {
                    this.p++;
                    this.line++;
                }

                this.curLexType = LexType.NOTE.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);

            } else if((p < source_len) && (ch == '*')) {    // 判断是 /* - 跨行注释:直到第一次出现 ‘*/’ 时结束
                token_sb.append(ch);
                this.p++;
                while(p < source_len) {
                    while((p < source_len) && (this.source_str.charAt(p) != '*')) {    // 非*字符
                        if(this.source_str.charAt(p) == '\n') {
                            this.line++;
                        }
                        this.p++;
                    }
                    // 此时要么文件结束要么指向字符*
                    while((p < source_len) && (this.source_str.charAt(p) == '*')) {
                        // 状态机中遇到*不停循环，非*非/就转移其他状态
                        this.p++;
                    }
                    if((p < source_len) && (this.source_str.charAt(p) == '/')) {
                        // 跨行注释状态机循环结束
                        this.p++;
                        break;
                    }
                }
                this.curLexType = LexType.NOTE.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);

            } else {                  // 判断是 /-除号
                this.curLexType = LexType.DIV.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }

        } else if(ch == '!') {       // 判断是 !、!=   【需要预读】
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);   // 需要提前预读下一个
            }

            if((p < source_len) && (ch == '=')) {          // 判断是 !=
                token_sb.append(ch);
                this.p++;
                this.curLexType = LexType.NEQ.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            } else {                 // 判断是 !
                this.curLexType = LexType.NOT.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }
        } else if(ch == '&') {     // 判断是 &&
            token_sb.append(ch);
            this.p++;
            if(p<source_len) {
                ch = this.source_str.charAt(this.p);   // 需要提前预读下一个
            }

            if((p < source_len) && (ch == '&')) {
                token_sb.append(ch);
                this.p++;
                this.curLexType = LexType.AND.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }

        } else if(ch == '|') {    // 判断是 ||
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);   // 需要提前预读下一个
            }

            if((p < source_len) && (ch == '|')) {
                token_sb.append(ch);
                this.p++;
                this.curLexType = LexType.OR.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }
        } else if(ch == '+') {    // 判断是 +
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.PLUS.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '-') {       // 判断是 -
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.MINU.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '*') {     // 判断是 *
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.MULT.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '%') {     // 判断是 %
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.MOD.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '<') {        // 判断是 <、<=   【注意预读下一个符号】
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);   // 需要提前预读下一个
            }

            if((p < source_len) && (ch == '=')) {         // 判断是 <=
                token_sb.append(ch);
                this.p++;
                this.curLexType = LexType.LEQ.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            } else {                // 判断是 <
                this.curLexType = LexType.LSS.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }
        } else if(ch == '>') {         // 判断是 >、>=  【需要预读】
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);   // 需要提前预读下一个
            }

            if((p < source_len) && (ch == '=')) {         // 判断是 >=
                token_sb.append(ch);
                this.p++;
                this.curLexType = LexType.GEQ.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            } else {                // 判断是 >
                this.curLexType = LexType.GRE.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }
        } else if(ch == '=') {       // 判断是 =、==   【需要预读】
            token_sb.append(ch);
            this.p++;
            if(p < source_len) {
                ch = this.source_str.charAt(this.p);   // 需要提前预读下一个
            }

            if((p < source_len) && (ch == '=')) {         // 判断是 ==
                token_sb.append(ch);
                this.p++;
                this.curLexType = LexType.EQL.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            } else {                // 判断是 =
                this.curLexType = LexType.ASSIGN.name();
                this.curToken = token_sb.toString();
                this.curLine = this.line;
                Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
                this.all_tokens.add(tmp_token);
            }
        } else if(ch == ';') {      // 判断是 ;
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.SEMICN.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == ',') {      // 判断是 ,
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.COMMA.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '(') {      // 判断是 (
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.LPARENT.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == ')') {      // 判断是 )
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.RPARENT.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '[') {      // 判断是 [
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.LBRACK.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == ']') {     // 判断是 ]
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.RBRACK.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '{') {     // 判断是 {
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.LBRACE.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else if(ch == '}') {    // 判断是 }
            token_sb.append(ch);
            this.p++;
            this.curLexType = LexType.RBRACE.name();
            this.curToken = token_sb.toString();
            this.curLine = this.line;
            Token tmp_token = new Token(this.curLexType, this.curToken, this.curLine);
            this.all_tokens.add(tmp_token);
        } else {         // 留给以后错误处理

        }


        return true;

    }

    public String getToken(){  //获得读取的单词值(非数值，而是字符串形式)
        return this.curToken;
    }

    public String getLexType() {   //获得读取的单词类型
        return this.curLexType;
    }

    public ArrayList<Token> getAll_tokens(){
        for(int i=0;i < this.all_tokens.size(); i++) {     // 把存的注释类型删除
            if(this.all_tokens.get(i).getLexType().equals("NOTE")) {
                this.all_tokens.remove(i);
                i--;
            }
        }

        return this.all_tokens;
    }

}
