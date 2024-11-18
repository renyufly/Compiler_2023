package AST;

import LEXER.LexType;
import LEXER.Token;
import PARSER.SyntaxType;
import SYMBOL.SymbolManager;
import tools.ErrorType;
import tools.Printer;

import java.util.ArrayList;

public class TokenNode extends Node{     // 树的节点 (终结符)
    private Token token;

    public TokenNode(int startLine, Token token) {
        super(SyntaxType.token.name(), null, token.getLine());
        this.token = token;
    }        // 已经是叶节点，没有子树

    public Token getToken() {
        return token;
    }

    public int getFormatCharCnt() {
        if(this.token.getLexType().equals(LexType.STRCON.name())) {
            int len = this.token.getTokenname().length();
            String str = this.token.getTokenname().substring(1, len-1);   // 把前后的双引号去掉
            int cnt = 0;
            for(int i=0; i<str.length(); i++) {
                char ch = str.charAt(i);
                if(ch == '%' && (i+1) < str.length() && str.charAt(i+1) == 'd') {
                    cnt++;
                }
            }
            return cnt;
        }

        return 0;
    }

    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        if(this.token.getLexType().equals(LexType.STRCON.name())) {   // error: a
            int len = this.token.getTokenname().length();
            String str = this.token.getTokenname().substring(1, len-1);   // 把前后的双引号去掉
            for(int i=0; i<str.length(); i++) {
                char ch = str.charAt(i);
                 if((ch == '%' && (i+1) >= str.length()) || (ch == '%' && (i+1) < str.length() && str.charAt(i+1) != 'd')) {
                    printer.addErrorMessage(this.token.getLine(), ErrorType.a);
                    return;
                } else if((ch == 92 && (i+1) >= str.length()) || (ch == 92 && str.charAt(i+1) != 'n')) {     // 此时为'\'
                    printer.addErrorMessage(this.token.getLine(), ErrorType.a);
                    return;
                } else if((ch < 32) || (ch > 33 && ch < 40 && ch != '%') || (ch > 126)) {
                    printer.addErrorMessage(this.token.getLine(), ErrorType.a);
                    return;
                }
            }
        }


    }






}
