package pt.up.fe.comp2024.optimization.REGopt;

import org.specs.comp.ollir.*;

import java.util.*;

public class REGLiveness {
    public REGLiveness(){
    }

    public List<REGInstInfo> livenessAnalysis(Method method){
        method.buildCFG();
        Map<Instruction, Set<String>> liveIns = new HashMap<>();
        Map<Instruction, Set<String>> liveOuts = new HashMap<>();

        ArrayList<Instruction> instructions = method.getInstructions();
        for (Instruction instruction : instructions) {
            liveIns.put(instruction, new HashSet<>());
            liveOuts.put(instruction, new HashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (Instruction instruction : instructions) {
                Set<String> newLiveIns = new HashSet<>(liveOuts.get(instruction));
                newLiveIns.removeAll(getDefines(instruction));
                newLiveIns.addAll(getUses(instruction));
                Set<String> newLiveOuts = new HashSet<>();

                List<Node> successors = instruction.getSuccessors();
                for (Node successor : successors) {
                    if (successor.getNodeType()!=NodeType.END) newLiveOuts.addAll(liveIns.get((Instruction) successor));
                }

                if (!newLiveIns.equals(liveIns.get(instruction)) || !newLiveOuts.equals(liveOuts.get(instruction))) {
                    changed = true;
                }

                liveIns.put(instruction, newLiveIns);
                liveOuts.put(instruction, newLiveOuts);
            }
        }while(changed);

        List<REGInstInfo> nodes = new ArrayList<>();
        for (Instruction instruction : method.getInstructions()) {
            REGInstInfo node = new REGInstInfo(instruction, liveIns.get(instruction),liveOuts.get(instruction),getDefines(instruction), getUses(instruction));
            nodes.add(node);
        }

        return nodes;
    }

    private Set<String> getDefines(Instruction instruction) {
        Set<String> defines = new HashSet<>();
        if (instruction instanceof AssignInstruction assignInstruction && assignInstruction.getDest() instanceof Operand operand) {
            defines.add(operand.getName());
        }
        return defines;
    }

    private String getUses(Element operand){
        if (operand instanceof Operand op) return op.getName();
        return "";
    }

    private Set<String> getUses(Instruction instruction) {
        Set<String> uses = new HashSet<>();

        if (instruction instanceof AssignInstruction assignInstruction) {
            uses.addAll(getUses(assignInstruction.getRhs()));
        } else if (instruction instanceof ReturnInstruction returnInstruction) {
            uses.add(getUses(returnInstruction.getOperand()));
        } else if (instruction instanceof BinaryOpInstruction binaryOpInstruction) {
            uses.add(getUses(binaryOpInstruction.getLeftOperand()));
            uses.add(getUses(binaryOpInstruction.getRightOperand()));
        } else if (instruction instanceof UnaryOpInstruction unaryOpInstruction) {
            uses.add(getUses(unaryOpInstruction.getOperand()));
        } else if (instruction instanceof SingleOpInstruction singleOpInstruction) {
            uses.add(getUses(singleOpInstruction.getSingleOperand()));
        } else if (instruction instanceof PutFieldInstruction putFieldInstruction) {
            uses.add(getUses(putFieldInstruction.getValue()));
        } else if (instruction instanceof CallInstruction callInstruction) {
            uses.add(getUses(callInstruction.getCaller()));
            for (Element arg : callInstruction.getArguments()) {
                uses.add(getUses(arg));
            }
        }
        else if (instruction instanceof GetFieldInstruction getFieldInstruction) {
            uses.add(getUses(getFieldInstruction.getField()));
        }

        return uses;
    }


}
