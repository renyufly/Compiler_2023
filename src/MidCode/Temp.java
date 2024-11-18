package MidCode;

public class Temp {
    private static int count = 0;      // 所有使用的临时寄存器数量
    private int curNum;     // 当前临时寄存器序号

    public Temp() {
        this.count ++;
        this.curNum = count;
    }

    @Override
    public String toString() {
        return "temp&" + this.curNum;       // 返回诸如 "temp&2" 来表示临时寄存器
    }



}
