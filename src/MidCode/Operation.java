package MidCode;

public enum Operation {      // 枚举四元式的运算符
    VAR,              // 普通变量定义 , 如 int a;
    ARRAY,            // 数组变量定义（不管常量变量）   int arr[2];
    CONST,            // 常量定义        const int c = 666;

    LABEL,            // 区分不同Block, 带序号
    MAIN_BEGIN,       // main()主函数开始
    MAIN_END,         // main()函数结束，整个程序结束

    FUNC,             // 函数定义
    PARAM_DEF,        // 函数形参定义
    PARAM_PUSH,       // 函数实参传入
    CALL,             // 函数调用
    RETURN,           // 函数定义时应该返回的值
    RETURN_VALUE,     // 函数调用时实际返回的值


    PLUS_OP,   // +
    MINU_OP,   // -
    MULT_OP,   // ×
    DIVI_OP,   // ÷
    MOD_OP,    // %

    ASSIGN,      // '=' 赋值

    PRINT,       // 写语句     printf();
    GETINT,      // 读语句      getint();

    SETARR,      // 给数组元素赋值   a[1] = x;
    GETARR,      // 从数组元素取值   x = a[1];


    BZ,            // be zero 跳转
    BNZ,           // be not zero 跳转
    JUMP_LABEL,    // jump区标签
    GOTO,          // 无条件跳转

    LSS,           //   <     [less than]
    LEQ,           //   <=    [less than or equal to]
    GRE,           //   >     [greater than]
    GEQ,           //   >=    [greater than or equal to]

    EQL,           //   ==    [equal to]
    NEQ,           //   !=    [not equal to]

    SLL,            //  <<  逻辑左移

    SRA,            //  >>  算术右移






}
