package pt.up.fe.comp2024.optimization.ASTopt;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.HashMap;
import java.util.Map;

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

}