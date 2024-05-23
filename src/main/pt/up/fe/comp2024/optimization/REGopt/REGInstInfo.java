package pt.up.fe.comp2024.optimization.REGopt;

import org.specs.comp.ollir.Instruction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class REGInstInfo {
    private final Instruction instruction;
    private final Set<String> ins;
    private final Set<String> outs;
    private final Set<String> defines;
    private final Set<String> uses;


    public REGInstInfo(Instruction instruction, Set<String> ins, Set<String> outs, Set<String> defines, Set<String> uses){
        this.instruction = instruction;
        this.ins = ins;
        this.outs = outs;
        this.defines = defines;
        this.uses = uses;
    }
    public Set<String> getIns(){
        return ins;
    }

    public Set<String> getOuts(){
        return outs;
    }

    public Set<String> getDefines(){
        return defines;
    }

    public Set<String> getUses(){
        return uses;
    }

    public Instruction getInstruction(){
        return instruction;
    }
}
