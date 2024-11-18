package OPTIMIZE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
/*    MIPS一共使用32个寄存器
$0-$zero;  [$t0-$t7: $8-$15]; [$t8-$t9: $24-$25]; [$s0-$s7: $16-$23]  [$v0-$v1];  [$a0-$a3];   [$k0-$k1];  <$hi; $lo; $pc>
$gp: $28;    $sp:$29;     $fp: $30;      $ra:$31
*/
public class Register {
    private ArrayList<String> alloc_reg_list;   // 当前可分配的空寄存器
    private ArrayList<String> cur_use_reg_list;    // 已被分配被占用的寄存器
    private HashMap<String, String> var_use_reg_map = new HashMap<>();    //  <"varname", "$t3">  <变量名, 使用的寄存器>

    public Register() {
        this.alloc_reg_list = new ArrayList<>(Arrays.asList("$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9",
                                                             "$s1", "$s2", "$s3", "$s4", "$s5", "$s6"));
        this.cur_use_reg_list = new ArrayList<>();

    }

        public String getCurUseReg(String varname) {    // 获取当前变量正使用的寄存器
        String reg = var_use_reg_map.get(varname);
        cur_use_reg_list.remove(reg);
        alloc_reg_list.add(reg);

        return reg;
    }

    public String allocReg(String varName) {      // 给当前变量分配寄存器
        if(alloc_reg_list.isEmpty()) {
            System.out.println("Alloc register fail !");
            return null;
        } else {
            String regName = alloc_reg_list.get(0);    // 从待分配空寄存器列表中取出第一个
            alloc_reg_list.remove(0);
            cur_use_reg_list.add(regName);            // 将当前变量分配给该寄存器
            var_use_reg_map.put(varName, regName);
            return regName;
        }

    }


    public void clear() {    // 重置清零
        this.alloc_reg_list = new ArrayList<>(Arrays.asList("$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9",
                                                            "$s1", "$s2", "$s3", "$s4", "$s5", "$s6"));
        this.cur_use_reg_list = new ArrayList<>();
    }

    public ArrayList<String> getAllocRegList() {
        return this.alloc_reg_list;
    }

    public ArrayList<String> getCurUseRegList() {      
        return this.cur_use_reg_list;
    }



}
