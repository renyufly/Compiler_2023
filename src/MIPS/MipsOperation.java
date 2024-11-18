package MIPS;

/*
add、addi和sub建议全换成addu、addiu和subu，不然竞速有测试点会ObjectCodeError
 */

public enum MipsOperation {
    add,     //  [add $t1, $t2, $t3  :  $t1 = $t2+$t3]
    addu,
    addi,
    addiu,
    sub,
    subu,
    mult,      // [mult $t1, $t2 :  乘积的高32位存$HI, 低32位存$LO]
    multu,

    div,
    divu,

    mflo,       // move from LO register     [mflo $t1]
    mfhi,       // move from HI register

    move,      //   [move $t1, $t2  :  把$t1的值设置为$t2值]

    sll,        // 逻辑左移  Shift Word Left Logical      [sll $t1, $t2, 10  : 将$t2值左移10位后存在$t1]
    srl,        //  逻辑右移

    bgez,       // Branch if Greater Than or Equal to Zero     [beqz $t1, label1]
    sra,        // 算术右移  Shift Word Right Arithmetic   [sra $t1, $t2, 10]

    sle,        // set less or equal
    sne,        // set not equal
    sgt,        // set greater than
    sge,        // set greater or equal
    slt,        // set less than     [slt $t1,$t2,$t3  : $t1 = ($t2 < $t3) ? 1 : 0]
    slti,
    seq,        // set equal   [seq $t1,$t2,$t3  : $t1 = ($t2==$t3) ? 1 : 0]

    beq,        // branch if equal     [beq $t1, $t2, label1]
    //bne,

   // bgt,
   // bge,
   // blt,
    //ble,


    li,        //   [li $t1, 100]
    la,        //    [la $t1, $t2] [la $t1, 100($t2)]

    lw,        //   [lw $t1, 50($t2)  :  将50+$t2后所得地址的值存在$t1]
    sw,        //   [sw $t1, 50($t2)]

    j,        //   [j target]
    jal,      //   [jal target  :  $ra存返回地址]
    jr,       //   [jr $t1]

    label,      // [label: ]

    syscall,    // 执行系统调用(根据$v0中的值)

    asciiz,    // .asciiz  : 将字符串存在data段

    data,    // .data
    text,    // .text

    word,    // .word




}
