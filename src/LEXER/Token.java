package LEXER;

public class Token {
    private String lexType;  // 类别码
    private String tokenname;  // 单词 (以字符串形式表示)
    private int line;        // 行号
    private int value;       // 对应值 （如是数字，则value就是其真正的数字值）

    public Token(String lextype, String tokenname, int line){
        this.lexType = lextype;
        this.tokenname = tokenname;
        this.line = line;
    }

    public Token(String lextype, String tokenname, int line, int value){
        this.lexType = lextype;
        this.tokenname = tokenname;
        this.line = line;
        this.value = value;
    }

    public String getLexType() {
        return this.lexType;
    }

    public String getTokenname() {
        return this.tokenname;
    }

    public int getLine() {
        return this.line;
    }

    public int getValue() {
        return this.value;
    }

}
