package SYMBOL;

import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {

    private HashMap<String, Symbol> directory;   // <token的字符串，token对应符号>


    public SymbolTable() {
        this.directory = new HashMap<>();
    }

    public ArrayList<String> getDirectory() {     // 返回存的所有token的name
        ArrayList<String> ret = new ArrayList<>();
        for(String name: this.directory.keySet()) {
            ret.add(name);
        }
        return ret;
    }

    public Symbol getSymbol(String ident) {
        if(this.directory.containsKey(ident)) {
            return this.directory.get(ident);
        } else {
            return null;    //
        }
    }

    public void addSymbol(Symbol symbol){
        this.directory.put(symbol.getToken(), symbol);
    }

    public boolean isContainSymbol(String ident) {    // 符号表是否含有对应符号
        return this.directory.containsKey(ident);
    }


}
