package pt.up.fe.comp2024.optimization.ASTopt;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PostorderJmmVisitor;
import static pt.up.fe.comp2024.ast.Kind.*;

public class ASTConstantFolder extends PostorderJmmVisitor<Void, Boolean> {
    public ASTConstantFolder() {
    }

    @Override
    protected void buildVisitor() {
        addVisit(BINARY_EXPR, this::visitBinaryExpr);
        addVisit(COMPARISON_EXPR, this::visitBinaryExpr);
        addVisit(BOOLEAN_EXPR, this::visitBinaryExpr);
        addVisit(UNARY_OP, this::visitUnaryOp);
        setDefaultVisit(this::defaultVisitor);
    }

    private Boolean defaultVisitor(JmmNode node, Void unused){
        return false; // no changes done for anything else
    }

    private Boolean visitBinaryExpr(JmmNode node, Void unused) {
        JmmNode lhs = node.getChild(0);
        JmmNode rhs = node.getChild(1);
        String op = node.get("op");

        while (lhs.getKind().equals(PAREN_EXPR.toString())) {
            lhs = lhs.getChild(0);
        }
        while (rhs.getKind().equals(PAREN_EXPR.toString())) {
            rhs = rhs.getChild(0);
        }

        JmmNode newNode;
        if (lhs.getKind().equals(INTEGER_LITERAL.toString()) && rhs.getKind().equals(INTEGER_LITERAL.toString())) {
            int left = Integer.parseInt(lhs.get("value"));
            int right = Integer.parseInt(rhs.get("value"));
            int result;

            switch (op) {
                case "+" -> result = left + right;
                case "-" -> result = left - right;
                case "*" -> result = left * right;
                case "/" -> result = left / right;
                case "<" -> result = left < right ? 1 : 0;
                default -> throw new UnsupportedOperationException("Unsupported operator: " + op);
            }

            node.removeChild(lhs);
            node.removeChild(rhs);

            if (op.equals("<")){
                newNode = new JmmNodeImpl(BOOLEAN.toString());
                newNode.put("value", result==1 ? "true" : "false");
            }
            else{
                newNode = new JmmNodeImpl(INTEGER_LITERAL.toString());
                newNode.put("value", Integer.toString(result));
            }

            node.replace(newNode);

            return true;
        }
        else if (lhs.getKind().equals(BOOLEAN.toString()) && rhs.getKind().equals(BOOLEAN.toString())) {
            boolean left = lhs.get("value").equals("true");
            boolean right = rhs.get("value").equals("true");
            boolean result = left && right;

            node.removeChild(lhs);
            node.removeChild(rhs);

            newNode = new JmmNodeImpl(BOOLEAN.toString());
            newNode.put("value", result ? "true" : "false");
            node.replace(newNode);

            return true;
        }

        return false;
    }

    private Boolean visitUnaryOp(JmmNode node, Void unused) {
        JmmNode expr = node.getChild(0);

        while (expr.getKind().equals(PAREN_EXPR.toString())) {
            expr = expr.getChild(0);
        }

        if (expr.getKind().equals(BOOLEAN.toString())) {
            String value = expr.get("value").equals("true") ? "false" : "true";
            JmmNode newNode;

            node.removeChild(expr);

            newNode = new JmmNodeImpl(BOOLEAN.toString());
            newNode.put("value", value);
            node.replace(newNode);

            return true;
        }

        return false;
    }
}
