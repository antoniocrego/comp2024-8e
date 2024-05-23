package pt.up.fe.comp2024.optimization.ASTopt;

import jas.Var;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTConstantPropagation extends AJmmVisitor<Void, Boolean> {
    private final Map<String, VarInfo> variables = new HashMap<>();

    public ASTConstantPropagation() {

    }

    @Override
    protected void buildVisitor() {
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(METHOD_DECL, this::visitMethod);
        addVisit(IF_STMT, this::visitIfStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        setDefaultVisit(this::visitChildren);
    }

    private Boolean visitMethod(JmmNode node, Void unused){
        boolean ret = visitChildren(node, unused);
        for (var variable : variables.values()){
            if (variable.isNotUsed()){
                variable.getNode().getParent().removeChild(variable.getNode());
            }
        }
        variables.clear();
        return ret;
    }

    private Boolean visitChildren(JmmNode node, Void unused){
        boolean result = false;

        for (var child : node.getChildren())
            result = result | visit(child, unused);

        return result;
    }

    private Boolean visitAssignStmt(JmmNode node, Void unused) {
        JmmNode lhs = node.getChild(0);
        JmmNode rhs = node.getChild(1);

        while (rhs.getKind().equals(PAREN_EXPR.toString())){
            rhs = rhs.getChild(0);
        }

        String varName = lhs.get("name");
        VarInfo var = variables.get(varName);

        boolean ret = visit(rhs, unused); // check if any more propagation will be done on rhs

        if (var != null && var.isNotUsed()){
            var.getNode().getParent().removeChild(var.getNode()); // variable is assigned but reassigned without use, destroy it
            variables.remove(varName);
            var = null;
        }
        if (var==null){
            if (rhs.getKind().equals(INTEGER_LITERAL.toString()) || rhs.getKind().equals(BOOLEAN.toString())){
                var = new VarInfo(lhs.getParent(), rhs.get("value"));
                variables.put(varName, var);
            }
            else if (rhs.getKind().equals(VAR_REF_EXPR.toString())){
                VarInfo rhsVar = variables.get(rhs.get("name"));
                if (rhsVar != null && rhsVar.getValue() != null){
                    var = new VarInfo(lhs.getParent(), rhsVar.getValue());
                    variables.put(varName, var);
                }
            }
        }

        return ret;
    }

    private Boolean visitVarRefExpr(JmmNode node, Void unused) {
        String varName = node.get("name");
        VarInfo var = variables.get(varName);

        if (var != null && var.getValue() != null){
            var newNode = new JmmNodeImpl(INTEGER_LITERAL.toString());
            newNode.put("value", var.getValue());
            node.replace(newNode);
            var.incrementTimesUsed();
            return true;
        }

        return false;
    }

    public Boolean visitIfStmt(JmmNode node, Void unused){
        boolean condition = visit(node.getChild(0), unused); // propagate to condition

        Set<String> modifiedVariables = visitAssignsAndDecls(node.getChild(1), unused); // get all variables modified inside the if
        Set<String> modifiedVariablesElse = visitAssignsAndDecls(node.getChild(2), unused); // get all variables modified inside the else

        Map<String, VarInfo> copy = new HashMap<>(variables);

        copy.forEach((key, value) -> {
            if (modifiedVariables.contains(key) || modifiedVariablesElse.contains(key)) {
                variables.remove(key); // this variable is no longer acceptable for optimization
            }
        });

        return condition | visit(node.getChild(1), unused) | visit(node.getChild(2), unused);
    }

    public Boolean visitWhileStmt(JmmNode node, Void unused){
        boolean condition = visit(node.getChild(0), unused); // propagate to condition

        Set<String> modifiedVariables = visitAssignsAndDecls(node.getChild(1), unused); // get all variables modified inside the while

        Map<String, VarInfo> copy = new HashMap<>(variables);

        copy.forEach((key, value) -> {
            if (modifiedVariables.contains(key)) {
                variables.remove(key); // this variable is no longer acceptable for optimization
            }
        });

         return condition | visit(node.getChild(1), unused);
    }

    public Set<String> visitAssignsAndDecls(JmmNode node){
        Set<String> modifiedVariables = new HashSet<>();

        for (var child : node.getChildren()){
            if (child.getKind().equals(ASSIGN_STMT.toString())){
                JmmNode lhs = child.getChild(0);
                String varName = lhs.get("name");
                modifiedVariables.add(varName);
            }
            else if (child.getKind().equals(VAR_DECL.toString())){ // this assumes that semantic impedes this variable from being accessed out of scope
                JmmNode lhs = child.getChild(1);
                String varName = lhs.get("name");
                modifiedVariables.add(varName);
            }
        }

        return modifiedVariables;
    }

}