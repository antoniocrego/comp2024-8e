package pt.up.fe.comp2024.optimization.ASTopt;

import pt.up.fe.comp.jmm.ast.JmmNode;

class VarInfo{
    private JmmNode node;
    private String value;
    private int timesUsed;

    public VarInfo(JmmNode node, String value){
        this.node = node;
        this.value = value;
        this.timesUsed = 0;
    }

    public JmmNode getNode(){
        return node;
    }

    public String getValue(){
        return value;
    }

    public void setValue(String value){
        this.value = value;
    }

    public void setNode(JmmNode node){
        this.node = node;
    }

    public void incrementTimesUsed(){
        timesUsed++;
    }

    public boolean isNotUsed(){
        return timesUsed == 0;
    }
}
