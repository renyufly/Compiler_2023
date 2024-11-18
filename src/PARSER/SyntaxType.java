package PARSER;

public enum SyntaxType {    // 语法分析-语法变量的枚举类
    CompUnit,
    Decl,             //
    BType,            //
    BlockItem,        //
    VarDecl,
    VarDef,
    InitVal,
    ConstDecl,
    ConstDef,
    ConstInitVal,
    FuncDef,
    FuncType,
    FuncFParams,
    FuncFParam,
    MainFuncDef,
    Block,
    Stmt,
    ForStmt,
    Exp,
    Cond,
    LVal,
    PrimaryExp,
    Number,
    UnaryExp,
    UnaryOp,
    FuncRParams,
    MulExp,
    AddExp,
    RelExp,
    EqExp,
    LAndExp,
    LOrExp,
    ConstExp,

    token   // 表示是“终结符”，具体类别码在对应Token类中查看

}
