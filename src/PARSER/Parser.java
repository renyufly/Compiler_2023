package PARSER;

import AST.*;

import LEXER.LexType;
import LEXER.Token;
import tools.ErrorType;
import tools.Printer;

import java.io.FileNotFoundException;
import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> all_tokens;    // 之前词法分析完的所有一串词
    private int cur_pos;         // 指向当前词序号
    private Token cur_token;
    private int backtrack_pos;    // 单词回溯标记
    private Printer printer;

    public Parser(ArrayList<Token> all_tokens, Printer printer1) {
        this.all_tokens = all_tokens;
        this.cur_pos = 0;          // 全局的，单一对象创建后后续函数都是共用的
        this.backtrack_pos = 0;


        this.printer = printer1;    // 专供输出


    }

    private void getCur_token() {
        if(cur_pos >= this.all_tokens.size()) {     // 如果读到末尾还要读下一个就返回空
            this.cur_token = null;
            cur_pos++;
        } else {
            this.cur_token =  this.all_tokens.get(cur_pos);
            cur_pos++;     // 注意获取当前单词后，cur_pos指针会++；
        }

    }

    private void setCurPosBack() {
        this.cur_pos--;
    }

    private Token getPre_read_token(int pos) {      // 预读取接下来距当前单词pos个位置的单词
        if((cur_pos -1 + pos) >= this.all_tokens.size()) {
            return null;
        }
        return this.all_tokens.get(cur_pos -1 + pos);
    }


    // 【除了<BlockItem>, <Decl>, <BType> 之外】

    // 编译单元 CompUnit  ->  {ConstDecl | VarDecl} {FuncDef} MainFunDef
    public Node parseCompUnit() {
        ArrayList<Node> children = new ArrayList<>();
        getCur_token();
        Node node = null;

        while(true) {
            if(cur_token == null) {
                break;
            } else if(getPre_read_token(1) != null && getPre_read_token(1).getLexType().equals(LexType.MAINTK.name())) {
                node = parseMainFunDef();
            } else if(getPre_read_token(2) != null && getPre_read_token(2).getLexType().equals(LexType.LPARENT.name())){
                node = parseFuncDef();
            } else if(this.cur_token.getLexType().equals(LexType.CONSTTK.name())) {
                node = parseConstDecl();
            } else if(this.cur_token.getLexType().equals(LexType.INTTK.name())) {
                node = parseVarDecl();
            } else {
                break;
            }
            children.add(node);
        }

        printer.printSyntax(SyntaxType.CompUnit.name());      // 向文件中输出内容

        return new CompUnit(SyntaxType.CompUnit.name(), children, getPre_read_token(-1).getLine());   // 生成对应语法成分的对象
    }

    // 变量声明 VarDecl → BType VarDef { ',' VarDef } ';'          【BType → 'int' 不用输出BType，直接INTTK】
    public Node parseVarDecl() {
        ArrayList<Node> children = new ArrayList<>();
        // 建当前树的初始化
        // ↓  开始分析 ↓
        // parse "int"
        printer.printToken(cur_token);       // 向文件输出内容
        Node node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();         //

        // parse VarDef
        node = parseVarDef();   // 每个parse递归完全结束后都会让cur_token指向解析完的下一个词
        children.add(node);

        // parse { ',' VarDef }
        while(cur_token.getLexType().equals(LexType.COMMA.name())) {
            // parse ','
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();       //
            // parse VarDef
            node = parseVarDef();
            children.add(node);

        }

        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();       //
        } else {      // 错误处理

            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.VarDecl.name());
        return new VarDecl(SyntaxType.VarDecl.name(), children, getPre_read_token(-1).getLine());

    }

    // 变量定义 VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    public Node parseVarDef() {
        ArrayList<Node> children = new ArrayList<>();
        // parse Ident
        printer.printToken(cur_token);
        Node node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();      //

        // parse { '[' ConstExp ']' }
        while(cur_token.getLexType().equals(LexType.LBRACK.name())) {
            // parse '['
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();                     //
            // parse ConstExp
            node = parseConstExp();
            children.add(node);
            // parse ']'
            if(cur_token.getLexType().equals(LexType.RBRACK.name())) {
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();            //
            } else {
                printer.addErrorMessage(node.getEndLine(), ErrorType.k);

                Token pseudoToken = new Token(LexType.RBRACK.name(), "]", node.getEndLine());
                node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                children.add(node);

            }

        }

        // parse '=' InitVal       【这是可选操作】
        if(cur_token.getLexType().equals(LexType.ASSIGN.name())) {
            // parse '='
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();              //
            // parse InitVal
            node = parseInitVal();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.VarDef.name());
        return new VarDef(SyntaxType.VarDef.name(), children, getPre_read_token(-1).getLine());

    }

    // 变量初值 InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    public Node parseInitVal() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse '{' [ InitVal { ',' InitVal } ] '}'
        if(cur_token.getLexType().equals(LexType.LBRACE.name())) {
            // parse '{'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
            // parse [ InitVal { ',' InitVal } ]
            if(!cur_token.getLexType().equals(LexType.RBRACE.name())) {
                // parse InitVal
                node = parseInitVal();
                children.add(node);
                // parse { ',' InitVal }
                while(cur_token.getLexType().equals(LexType.COMMA.name())) {
                    // parse ','
                    printer.printToken(cur_token);
                    node = new TokenNode(cur_token.getLine(), cur_token);
                    children.add(node);
                    getCur_token();             //
                    // parse InitVal
                    node = parseInitVal();
                    children.add(node);
                }

            }
            // parse '}'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();                  //

        } else {         // parse Exp
            node = parseExp();
            children.add(node);
        }

        printer.printSyntax(SyntaxType.InitVal.name());
        return new InitVal(SyntaxType.InitVal.name(), children, getPre_read_token(-1).getLine());

    }

    // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'      【BType -> 'int'】
    public Node parseConstDecl() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'const'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                  //
        // parse 'int'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                //
        // parse ConstDef
        node = parseConstDef();
        children.add(node);
        // parse { ',' ConstDef }
        while(cur_token.getLexType().equals(LexType.COMMA.name())) {
            // parse ','
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();                     //
            // parse ConstDef
            node = parseConstDef();
            children.add(node);
        }
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();                    //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);

        }

        printer.printSyntax(SyntaxType.ConstDecl.name());
        return new ConstDecl(SyntaxType.ConstDecl.name(), children, getPre_read_token(-1).getLine());

    }

    // 常数定义 ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    public Node parseConstDef() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse Ident
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();           //
        // parse { '[' ConstExp ']' }
        while(cur_token.getLexType().equals(LexType.LBRACK.name())) {
            // parse '['
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
            // parse ConstExp
            node = parseConstExp();
            children.add(node);
            // parse ']'
            if(cur_token.getLexType().equals(LexType.RBRACK.name())) {
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();              //
            } else {
                printer.addErrorMessage(node.getEndLine(), ErrorType.k);

                Token pseudoToken = new Token(LexType.RBRACK.name(), "]", node.getEndLine());
                node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                children.add(node);
            }

        }
        // parse '='
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                  //
        // parse ConstInitVal
        node = parseConstInitVal();
        children.add(node);

        printer.printSyntax(SyntaxType.ConstDef.name());
        return new ConstDef(SyntaxType.ConstDef.name(), children, getPre_read_token(-1).getLine());

    }

    // 常量初值 ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    public Node parseConstInitVal() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        if(cur_token.getLexType().equals(LexType.LBRACE.name())) {
            // parse '{'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();      //
            // parse [ ConstInitVal { ',' ConstInitVal } ]
            if(!cur_token.getLexType().equals(LexType.RBRACE.name())) {
                // parse ConstInitVal
                node = parseConstInitVal();
                children.add(node);
                // parse { ',' ConstInitVal }
                while(cur_token.getLexType().equals(LexType.COMMA.name())) {
                    // parse ','
                    printer.printToken(cur_token);
                    node = new TokenNode(cur_token.getLine(), cur_token);
                    children.add(node);
                    getCur_token();              //
                    // parse ConstInitVal
                    node = parseConstInitVal();
                    children.add(node);
                }

            }
            // parse '}'
            printer.printToken(cur_token);
           node = new TokenNode(cur_token.getLine(), cur_token);
           children.add(node);
           getCur_token();                    //

        } else {        // parse ConstExp
            node = parseConstExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.ConstInitVal.name());
        return new ConstInitVal(SyntaxType.ConstInitVal.name(), children, getPre_read_token(-1).getLine());

    }

    // 函数定义 FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    public Node parseFuncDef() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse FuncType
        node = parseFuncType();
        children.add(node);
        // parse Ident
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                  //
        // parse '('
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                   //
        // [FuncFParams]
        if(cur_token.getLexType().equals(LexType.INTTK.name())) {
            node = parseFuncFParams();
            children.add(node);
        }
        // parse ')'
        if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.j);

            Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }
        // parse Block
        node = parseBlock();
        children.add(node);

        printer.printSyntax(SyntaxType.FuncDef.name());
        return new FuncDef(SyntaxType.FuncDef.name(), children, getPre_read_token(-1).getLine());

    }

    // 函数类型 FuncType → 'void' | 'int'
    public Node parseFuncType() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'void' | 'int'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();       //

        printer.printSyntax(SyntaxType.FuncType.name());
        return new FuncType(SyntaxType.FuncType.name(), children, getPre_read_token(-1).getLine());

    }

    // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
    public Node parseFuncFParams() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse FuncFParam
        node = parseFuncFParam();
        children.add(node);
        // parse { ',' FuncFParam }
        while(cur_token.getLexType().equals(LexType.COMMA.name())) {
            // parse ','
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();       //
            // parse FuncFparam
            node = parseFuncFParam();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.FuncFParams.name());
        return new FuncFParams(SyntaxType.FuncFParams.name(), children, getPre_read_token(-1).getLine());

    }

    // 函数形参 FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    public Node parseFuncFParam() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'int'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();      //
        // parse Ident
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();      //
        // parse ['[' ']' { '[' ConstExp ']' }]
        if(cur_token.getLexType().equals(LexType.LBRACK.name())) {
            // parse '['
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();          //
            // parse ']'
            if(cur_token.getLexType().equals(LexType.RBRACK.name())) {
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();             //
            } else {
                printer.addErrorMessage(node.getEndLine(), ErrorType.k);

                Token pseudoToken = new Token(LexType.RBRACK.name(), "]", node.getEndLine());
                node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                children.add(node);
            }
            // parse { '[' ConstExp ']' }
            while(cur_token.getLexType().equals(LexType.LBRACK.name())) {
                // parse '['
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();      //
                // parse ConstExp
                node = parseConstExp();
                children.add(node);
                // parse ']'
                if(cur_token.getLexType().equals(LexType.RBRACK.name())) {
                    printer.printToken(cur_token);
                    node = new TokenNode(cur_token.getLine(), cur_token);
                    children.add(node);
                    getCur_token();               //
                } else {
                    printer.addErrorMessage(node.getEndLine(), ErrorType.k);

                    Token pseudoToken = new Token(LexType.RBRACK.name(), "]", node.getEndLine());
                    node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                    children.add(node);
                }

            }

        }

        printer.printSyntax(SyntaxType.FuncFParam.name());
        return new FuncFParam(SyntaxType.FuncFParam.name(), children, getPre_read_token(-1).getLine());

    }

    // 主函数定义 MainFuncDef → 'int' 'main' '(' ')' Block
    public Node parseMainFunDef() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'int'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();             //
        // parse 'main'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                  //
        // parse '('
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();              //
        // parse ')'
        if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();       //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.j);

            Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }
        // parse Block
        node = parseBlock();
        children.add(node);

        printer.printSyntax(SyntaxType.MainFuncDef.name());
        return new MainFuncDef(SyntaxType.MainFuncDef.name(), children, getPre_read_token(-1).getLine());

    }

    // 语句块 Block → '{' { BlockItem } '}'         【BlockItem → ConstDecl | VarDecl | Stmt】
    public Node parseBlock() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse '{'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();          //
        // parse { BlockItem }
        while (!cur_token.getLexType().equals(LexType.RBRACE.name())) {
            if(cur_token.getLexType().equals(LexType.CONSTTK.name())) {        // parse ConstDecl
                node = parseConstDecl();
                children.add(node);
            } else if(cur_token.getLexType().equals(LexType.INTTK.name())) {   // parse VarDecl
                node = parseVarDecl();
                children.add(node);
            } else {                                                           // parse Stmt

                if(cur_token.getLexType().equals(LexType.RBRACE.name())) {     // '}'
                    int endLine = getPre_read_token(-1).getLine();
                    cur_token = new Token(LexType.SEMICN.name(), ";", endLine);
                    setCurPosBack();
                    printer.addErrorMessage(endLine, ErrorType.i);        //
                }
                node = parseStmt();
                children.add(node);
            }

        }
        // parse '}'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                     //

        printer.printSyntax(SyntaxType.Block.name());
        return new Block(SyntaxType.Block.name(), children, getPre_read_token(-1).getLine());

    }

    // 语句 Stmt → LVal '=' Exp ';' | [Exp] ';' | Block | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]       【多产生式选择问题】
    //        | 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt | 'break' ';' | 'continue' ';'
    //        | 'return' [Exp] ';' | LVal '=' 'getint''('')'';' | 'printf''('FormatString{','Exp}')'';'
    public Node parseStmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;


        if(cur_token.getLexType().equals(LexType.LBRACE.name())) {          // parse Block
            return block_Stmt();
        } else if(cur_token.getLexType().equals(LexType.IFTK.name())) {     // parse 'if' ...
            return if_Stmt();
        } else if(cur_token.getLexType().equals(LexType.FORTK.name())) {    // parse 'for' ...
            return for_Stmt();
        } else if(cur_token.getLexType().equals(LexType.BREAKTK.name())) {  // parse 'break' ';'
            return break_Stmt();
        } else if(cur_token.getLexType().equals(LexType.CONTINUETK.name())) {   // parse 'continue' ';'
            return continue_Stmt();
        } else if(cur_token.getLexType().equals(LexType.RETURNTK.name())) {    // parse 'return' ...
            return return_Stmt();
        } else if(cur_token.getLexType().equals(LexType.PRINTFTK.name())) {    // parse 'printf' ...
            return printf_Stmt();
        } else if(cur_token.getLexType().equals(LexType.SEMICN.name())) {    // parse [Exp] ';'中没有[Exp]的情况
            return exp_Stmt();
        }

        /* LVal '=' Exp ';' | [Exp] ';' | LVal '=' 'getint''('')'';' 右部的FIRST集存在交集{Ident}。∵ Exp → ... → LVal
           ∴ 记录cur_token位置(做好回溯准备)，先统一用Exp的解析，若后面的token是'='再进一步判断是否'getint',然后回溯初始位，
           重新进行对应assign或getint的解析；否则就正常按Exp的解析，不用回溯。
           【回溯期间不能向输出文件打印东西】
        */
        this.backtrack_pos = this.cur_pos;     // 回溯标记位置
        printer.setIsprint(false);             // 关闭打印  【回溯期间不打印】
        parseExp();                            // 该解析的结果并不会被记录
        printer.setIsprint(true);              // 打开

        if(cur_token.getLexType().equals(LexType.ASSIGN.name())) {
            if(getPre_read_token(1).getLexType().equals(LexType.GETINTTK.name())) {  // parse LVal '=' 'getint' ...
                this.cur_pos = this.backtrack_pos-1;
                getCur_token();       //  返回回溯点

                return getint_Stmt();

            } else {                                         // parse LVal '=' Exp ';'
                this.cur_pos = this.backtrack_pos-1;
                getCur_token();       //  返回回溯点

                return assign_Stmt();

            }
        } else {                                             // parse [Exp] ';'
            this.cur_pos = this.backtrack_pos-1;
            getCur_token();         // 返回回溯点

            return exp_Stmt();

        }

    }

    // Stmt -> Block
    public Node block_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse Block
        node = parseBlock();
        children.add(node);

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_block(true);
        return stmt;
    }

    // Stmt -> 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    public Node if_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'if'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                 //
        // parse '('
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                //
        // parse Cond
        node = parseCond();
        children.add(node);
        // parse ')'
        if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.j);

            Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        // parse Stmt
        if(cur_token.getLexType().equals(LexType.RBRACE.name())) {     // '}'
            int endLine = getPre_read_token(-1).getLine();
            cur_token = new Token(LexType.SEMICN.name(), ";", endLine);
            setCurPosBack();
            printer.addErrorMessage(endLine, ErrorType.i);      //
        }

        node = parseStmt();
        children.add(node);
        // parse [ 'else' Stmt ]
        if(cur_token.getLexType().equals(LexType.ELSETK.name())) {
            // parse 'else'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();                  //
            // parse Stmt
            if(cur_token.getLexType().equals(LexType.RBRACE.name())) {     // '}'
                int endLine = getPre_read_token(-1).getLine();
                cur_token = new Token(LexType.SEMICN.name(), ";", endLine);
                setCurPosBack();
                printer.addErrorMessage(endLine, ErrorType.i);     //
            }

            node = parseStmt();
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_if_stmt(true);
        return stmt;

    }

    // parse 'for' '(' [ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    public Node for_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'for'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();              //
        // parse '('
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                  //
        // parse [ForStmt]
        if(!cur_token.getLexType().equals(LexType.SEMICN.name())) {
            node = parseForStmt();
            children.add(node);
        }
        // parse ';'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();              //
        // parse [Cond]
        if(!cur_token.getLexType().equals(LexType.SEMICN.name())) {
            node = parseCond();
            children.add(node);
        }
        // parse ';'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();             //
        // parse [ForStmt]
        if(!cur_token.getLexType().equals(LexType.RPARENT.name())) {
            node = parseForStmt();
            children.add(node);
        }
        // parse ')'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();                  //
        // parse Stmt
        if(cur_token.getLexType().equals(LexType.RBRACE.name())) {     // '}'
            int endLine = getPre_read_token(-1).getLine();
            cur_token = new Token(LexType.SEMICN.name(), ";", endLine);
            setCurPosBack();
            printer.addErrorMessage(endLine, ErrorType.i);     //
        }

        node = parseStmt();
        children.add(node);

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_for_stmt(true);
        return stmt;

    }

    // parse 'break' ';'
    public Node break_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'break'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();          //
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();           //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);


        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_break(true);
        return stmt;

    }

    // parse 'continue' ';'
    public Node continue_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'continue'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();         //
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();        //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children,getPre_read_token(-1).getLine());
        stmt.setIs_continue(true);
        return stmt;

    }

    // parse 'return' [Exp] ';'
    public Node return_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'return'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();         //
        // parse [Exp]              【终结：'(' IntConst Ident '+' '-' '!'】
        if(cur_token.getLexType().equals(LexType.LPARENT.name()) || cur_token.getLexType().equals(LexType.INTCON.name()) ||
           cur_token.getLexType().equals(LexType.IDENFR.name())  || cur_token.getLexType().equals(LexType.PLUS.name()) ||
           cur_token.getLexType().equals(LexType.MINU.name()) || cur_token.getLexType().equals(LexType.NOT.name())) {
            node = parseExp();
            children.add(node);

        }
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();              //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_return_stmt(true);
        return stmt;

    }

    // parse 'printf''('FormatString{','Exp}')'';'
    public Node printf_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse 'printf'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();            //
        // parse '('
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();             //
        // parse FormatString
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();

        // parse {','Exp}
        while(cur_token.getLexType().equals(LexType.COMMA.name())) {
            // parse ','
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();          //
            // parse Exp
            node = parseExp();
            children.add(node);

        }
        // parse ')'
        if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();        //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.j);

            Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();          //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_printf(true);
        return stmt;

    }

    // parse [Exp] ';'
    public Node exp_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse [Exp]              【终结：'(' IntConst Ident '+' '-' '!'】
        if(cur_token.getLexType().equals(LexType.LPARENT.name()) || cur_token.getLexType().equals(LexType.INTCON.name()) ||
                cur_token.getLexType().equals(LexType.IDENFR.name())  || cur_token.getLexType().equals(LexType.PLUS.name()) ||
                cur_token.getLexType().equals(LexType.MINU.name()) || cur_token.getLexType().equals(LexType.NOT.name())) {
            node = parseExp();
            children.add(node);

        }
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();            //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_exp(true);
        return stmt;

    }

    // parse LVal '=' 'getint''('')'';'
    public Node getint_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse LVal
        node = parseLVal();
        children.add(node);
        // parse '='
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();              //
        // parse 'getint'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();            //
        // parse '('
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();              //
        // parse ')'
        if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();            //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.j);


            Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();             //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_getint(true);
        return stmt;

    }

    // parse LVal '=' Exp ';'
    public Node assign_Stmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse LVal
        node = parseLVal();
        children.add(node);
        // parse '='
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();           //
        // parse Exp
        node = parseExp();
        children.add(node);
        // parse ';'
        if(cur_token.getLexType().equals(LexType.SEMICN.name())) {
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();             //
        } else {
            printer.addErrorMessage(node.getEndLine(), ErrorType.i);

            Token pseudoToken = new Token(LexType.SEMICN.name(), ";", node.getEndLine());
            node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
            children.add(node);
        }

        printer.printSyntax(SyntaxType.Stmt.name());
        Stmt stmt = new Stmt(SyntaxType.Stmt.name(), children, getPre_read_token(-1).getLine());
        stmt.setIs_lval_exp(true);
        return stmt;

    }

    // 语句 ForStmt → LVal '=' Exp
    public Node parseForStmt() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse LVal
        node = parseLVal();
        children.add(node);
        // parse '='
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();            //
        // parse Exp
        node = parseExp();
        children.add(node);

        printer.printSyntax(SyntaxType.ForStmt.name());
        return new ForStmt(SyntaxType.ForStmt.name(), children, getPre_read_token(-1).getLine());

    }

    // 表达式 Exp → AddExp
    public Node parseExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse AddExp
        node = parseAddExp();
        children.add(node);

        printer.printSyntax(SyntaxType.Exp.name());
        return new Exp(SyntaxType.Exp.name(), children, getPre_read_token(-1).getLine());

    }

    // 条件表达式 Cond → LOrExp
    public Node parseCond() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse LOrExp
        node = parseLOrExp();
        children.add(node);

        printer.printSyntax(SyntaxType.Cond.name());
        return new Cond(SyntaxType.Cond.name(), children, getPre_read_token(-1).getLine());

    }

    // 左值表达式 LVal → Ident {'[' Exp ']'}
    public Node parseLVal() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse Ident
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();               //
        // parse {'[' Exp ']'}
        while(cur_token.getLexType().equals(LexType.LBRACK.name()) ) {
            // parse '['
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();           //
            // parse Exp
            node = parseExp();
            children.add(node);
            // parse ']'
            if(cur_token.getLexType().equals(LexType.RBRACK.name())) {
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();            //
            } else {
                printer.addErrorMessage(node.getEndLine(), ErrorType.k);

                Token pseudoToken = new Token(LexType.RBRACK.name(), "]", node.getEndLine());
                node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                children.add(node);
            }

        }

        printer.printSyntax(SyntaxType.LVal.name());
        return new LVal(SyntaxType.LVal.name(), children, getPre_read_token(-1).getLine());

    }

    // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number
    public Node parsePrimaryExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;

        if(cur_token.getLexType().equals(LexType.INTCON.name())) {          // parse Number
            // parse Number
            node = parseNumber();
            children.add(node);
        } else if(cur_token.getLexType().equals(LexType.LPARENT.name())) {   // parse '(' Exp ')'
            // parse '('
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();             //
            // parse Exp
            node = parseExp();
            children.add(node);
            // parse ')'
            if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();           //
            } else {
                printer.addErrorMessage(node.getEndLine(), ErrorType.j);     //

                Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
                node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                children.add(node);
            }
        } else {                         // parse LVal
            node = parseLVal();
            children.add(node);
        }

        printer.printSyntax(SyntaxType.PrimaryExp.name());
        return new PrimaryExp(SyntaxType.PrimaryExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 数值 Number → IntConst
    public Node parseNumber() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;

        // parse IntConst
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();           //

        printer.printSyntax(SyntaxType.Number.name());
        return new Number_AST(SyntaxType.Number.name(), children, getPre_read_token(-1).getLine());

    }

    // 一元表达式 UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    public Node parseUnaryExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;

        // Ident '(' [FuncRParams] ')'
        if(cur_token.getLexType().equals(LexType.IDENFR.name()) &&
           getPre_read_token(1).getLexType().equals(LexType.LPARENT.name())) {
            // parse Ident
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();             //
            // parse '('
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();              //
            // parse [FuncRParams]    【终结：'(' IntConst Ident '+' '-' '!'】
            if(cur_token.getLexType().equals(LexType.LPARENT.name()) || cur_token.getLexType().equals(LexType.INTCON.name()) ||
               cur_token.getLexType().equals(LexType.IDENFR.name())  || cur_token.getLexType().equals(LexType.PLUS.name()) ||
               cur_token.getLexType().equals(LexType.MINU.name()) || cur_token.getLexType().equals(LexType.NOT.name())) {
                node = parseFuncRParams();
                children.add(node);
            }
            // parse ')'
            if(cur_token.getLexType().equals(LexType.RPARENT.name())) {
                printer.printToken(cur_token);
                node = new TokenNode(cur_token.getLine(), cur_token);
                children.add(node);
                getCur_token();               //
            } else {
                printer.addErrorMessage(node.getEndLine(), ErrorType.j);

                Token pseudoToken = new Token(LexType.RPARENT.name(), ")", node.getEndLine());
                node  = new TokenNode(pseudoToken.getLine(), pseudoToken);
                children.add(node);
            }

        } else if(cur_token.getLexType().equals(LexType.PLUS.name()) || cur_token.getLexType().equals(LexType.MINU.name()) ||
                  cur_token.getLexType().equals(LexType.NOT.name())) {    // parse UnaryOp UnaryExp  【UnaryOp → '+' | '−' | '!'】
            // parse UnaryOp
            node = parseUnaryOp();
            children.add(node);
            // parse UnaryExp
            node = parseUnaryExp();
            children.add(node);

        } else {                         // parse PrimaryExp
            node = parsePrimaryExp();
            children.add(node);
        }

        printer.printSyntax(SyntaxType.UnaryExp.name());
        return new UnaryExp(SyntaxType.UnaryExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 单目运算符 UnaryOp → '+' | '−' | '!'
    public Node parseUnaryOp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse '+' | '−' | '!'
        printer.printToken(cur_token);
        node = new TokenNode(cur_token.getLine(), cur_token);
        children.add(node);
        getCur_token();              //

        printer.printSyntax(SyntaxType.UnaryOp.name());
        return new UnaryOp(SyntaxType.UnaryOp.name(), children, getPre_read_token(-1).getLine());

    }

    // 函数实参表 FuncRParams → Exp { ',' Exp }
    public Node parseFuncRParams() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse Exp
        node = parseExp();
        children.add(node);
        // parse { ',' Exp }
        while(cur_token.getLexType().equals(LexType.COMMA.name())) {
            // parse ','
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();            //
            // parse Exp
            node = parseExp();
            children.add(node);
        }


        printer.printSyntax(SyntaxType.FuncRParams.name());
        return new FuncRParams(SyntaxType.FuncRParams.name(), children, getPre_read_token(-1).getLine());

    }

    // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp   【左递归问题】
    // 改写成 UnaryExp { ('*' | '/' | '%') UnaryExp}  【每次解析{}里前, 先将之前已解析出的若干个<↓>合成一个<↑>，并输出一次<↑>】

    public Node parseMulExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse UnaryExp
        node = parseUnaryExp();
        children.add(node);
        // parse { ('*' | '/' | '%') UnaryExp}
        while(cur_token.getLexType().equals(LexType.MULT.name()) || cur_token.getLexType().equals(LexType.DIV.name()) ||
              cur_token.getLexType().equals(LexType.MOD.name())) {
            printer.printSyntax(SyntaxType.MulExp.name());    // 先输出一次
            // parse '*' | '/' | '%'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
            // parse UnaryExp
            node = parseUnaryExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.MulExp.name());
        return new MulExp(SyntaxType.MulExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp    【左递归问题】
    // 改写成 MulExp { ('+' | '−') MulExp }
    public Node parseAddExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse MulExp
        node = parseMulExp();
        children.add(node);
        // parse { ('+' | '−') MulExp }
        while (cur_token.getLexType().equals(LexType.PLUS.name()) || cur_token.getLexType().equals(LexType.MINU.name())) {
            printer.printSyntax(SyntaxType.AddExp.name());    // 先输出一次
            // parse '+' | '-'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
            // parse MulExp
            node = parseMulExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.AddExp.name());
        return new AddExp(SyntaxType.AddExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp   【左递归式】
    // 改写成： AddExp { ('<' | '>' | '<=' | '>=') AddExp }
    public Node parseRelExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse AddExp
        node = parseAddExp();
        children.add(node);
        // parse { ('<' | '>' | '<=' | '>=') AddExp }
        while (cur_token.getLexType().equals(LexType.LSS.name()) || cur_token.getLexType().equals(LexType.GRE.name()) ||
               cur_token.getLexType().equals(LexType.LEQ.name()) || cur_token.getLexType().equals(LexType.GEQ.name())) {
            printer.printSyntax(SyntaxType.RelExp.name());       //
            // parse '<' | '>' | '<=' | '>='
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();             //
            // parse AddExp
            node = parseAddExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.RelExp.name());
        return new RelExp(SyntaxType.RelExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp      【左递归式】
    // 改写成： RelExp { ('==' | '!=') RelExp }
    public Node parseEqExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse RelExp
        node = parseRelExp();
        children.add(node);
        // parse { ('==' | '!=') RelExp }
        while (cur_token.getLexType().equals(LexType.EQL.name()) || cur_token.getLexType().equals(LexType.NEQ.name())) {
            printer.printSyntax(SyntaxType.EqExp.name());       //
            // parse '==' | '!='
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();              //
            // parse RelExp
            node = parseRelExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.EqExp.name());
        return new EqExp(SyntaxType.EqExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp      【左递归式】
    // 改写成：  EqExp { '&&' EqExp }
    public Node parseLAndExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse EqExp
        node = parseEqExp();
        children.add(node);
        // parse { '&&' EqExp }
        while (cur_token.getLexType().equals(LexType.AND.name())) {
            printer.printSyntax(SyntaxType.LAndExp.name());      //
            // parse '&&'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();               //
            // parse EqExp
            node = parseEqExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.LAndExp.name());
        return new LAndExp(SyntaxType.LAndExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp     【左递归式】
    // 改写成:  LAndExp { '||' LAndExp }
    public Node parseLOrExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse LAndExp
        node = parseLAndExp();
        children.add(node);
        // parse { '||' LAndExp }
        while (cur_token.getLexType().equals(LexType.OR.name())) {
            printer.printSyntax(SyntaxType.LOrExp.name());       //
            // parse '||'
            printer.printToken(cur_token);
            node = new TokenNode(cur_token.getLine(), cur_token);
            children.add(node);
            getCur_token();                    //
            // parse LAndExp
            node = parseLAndExp();
            children.add(node);

        }

        printer.printSyntax(SyntaxType.LOrExp.name());
        return new LOrExp(SyntaxType.LOrExp.name(), children, getPre_read_token(-1).getLine());

    }

    // 常量表达式 ConstExp → AddExp
    public Node parseConstExp() {
        ArrayList<Node> children = new ArrayList<>();
        Node node = null;
        // parse AddExp
        node = parseAddExp();
        children.add(node);

        printer.printSyntax(SyntaxType.ConstExp.name());
        return new ConstExp(SyntaxType.ConstExp.name(), children, getPre_read_token(-1).getLine());

    }




}
