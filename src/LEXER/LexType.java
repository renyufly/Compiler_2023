package LEXER;

public enum LexType {        // 词法分析-终结符类别码-枚举类
    IDENFR,    // Ident
    INTCON,    // IntConst
    STRCON,    // FormatString
    MAINTK,    // main
    CONSTTK,   // const
    INTTK,     // int
    BREAKTK,   // break
    CONTINUETK, // continue
    IFTK,     // if
    ELSETK,   // else
    NOT,       // !
    AND,      // &&
    OR,        // ||
    FORTK,     // for
    GETINTTK, // getint
    PRINTFTK, // printf
    RETURNTK, // return
    PLUS,     // +
    MINU,     // -
    VOIDTK,   // void
    MULT,     // *
    DIV,      // /
    MOD,      // %
    LSS,      // <
    LEQ,      // <=
    GRE,      // >
    GEQ,      // >=
    EQL,      // ==
    NEQ,      // !=
    ASSIGN,   // =
    SEMICN,   // ;
    COMMA,    // ,
    LPARENT,  // (
    RPARENT,  // )
    LBRACK,   // [
    RBRACK,   // ]
    LBRACE,   // {
    RBRACE,   // }

    NOTE;     // 注释

}
