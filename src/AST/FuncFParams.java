package AST;

import SYMBOL.SymbolManager;
import SYMBOL.SymbolType;
import tools.Printer;

import java.util.ArrayList;

// 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
public class FuncFParams extends Node{
    public FuncFParams( String syntax_type, ArrayList<Node> children, int end_line) {
        super( syntax_type, children, end_line);
    }

    public ArrayList<SymbolType> getFParamsType() {
        ArrayList<SymbolType> fParam = new ArrayList<>();
        for(Node child: children) {
            if(child instanceof FuncFParam) {
                SymbolType type = ((FuncFParam) child).getParamType();
                fParam.add(type);
            }
        }
        return fParam;
    }

    public ArrayList<Integer> getFParamsDim() {
        ArrayList<Integer> fParamDim = new ArrayList<>();
        for(Node child: children) {
            if(child instanceof FuncFParam) {
                int dim = ((FuncFParam) child).getParamDim();
                fParamDim.add(dim);
            }
        }
        return fParamDim;
    }


    @Override
    public void checkError(Printer printer, SymbolManager symbolManager) {
        super.checkError(printer, symbolManager);
    }







}
