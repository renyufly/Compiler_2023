package OPTIMIZE;
// 单例模式
public class Optimizer {
    private static Optimizer instance = new Optimizer();
    private boolean is_turn_optimizer;
    private Optimizer(){
        this.is_turn_optimizer = false;
    }

    public static Optimizer getInstance(){
        return instance;
    }

    public void setTurnOnOptimizer(boolean bool) {
        this.is_turn_optimizer = bool;
    }

    public boolean isTurnOn() {
        return this.is_turn_optimizer;
    }



}
