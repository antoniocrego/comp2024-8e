package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class BooleanConditionCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_STMT, this::visitBooelanCond);
        addVisit(Kind.WHILE_STMT, this::visitBooelanCond);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBooelanCond(JmmNode booleanExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        try{
            var condition = booleanExpr.getChild(0);
            var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
            megaTable.addAll(table.getParameters(currentMethod));
            megaTable.addAll(table.getFields());
            while (condition.getKind().equals(Kind.PAREN_EXPR.toString())){
                condition = condition.getChild(0);
            }
            if (condition.getKind().equals(Kind.VAR_REF_EXPR.toString())){
                var varRefName = condition.get("name");
                for (var element : megaTable){
                    if (element.getName().equals(varRefName)){
                        if (element.getType().isArray() || !element.getType().getName().equals("boolean")){
                            var message = "";
                            if (booleanExpr.getKind().equals(Kind.IF_STMT.toString())) message += "If";
                            else if (booleanExpr.getKind().equals(Kind.WHILE_STMT.toString())) message += "While";
                            message += " condition with variable '%s' of type '%s'";
                            if (element.getType().isArray()) message += " array";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(booleanExpr),
                                    NodeUtils.getColumn(booleanExpr),
                                    String.format(message, varRefName, element.getType().getName()),
                                    null)
                            );
                            return null;
                        }
                    }
                }
            }
            else if (condition.getKind().equals(Kind.FUNC_CALL.toString())){
                var methodVariable = condition.getChild(0).get("name");
                var methodName = condition.get("id");
                Type methodCallerType = new Type("", false);
                if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
                else{
                    for (var element : megaTable) {
                        if (element.getName().equals(methodVariable)) {
                            methodCallerType = element.getType();
                            break;
                        }
                    }
                }
                if (!methodCallerType.isArray() && !methodCallerType.getName().equals(table.getClassName())) return null;
                var returnType = table.getReturnType(methodName);
                if (!returnType.getName().equals("boolean") || returnType.isArray()){
                    var message = "";
                    if (booleanExpr.getKind().equals(Kind.IF_STMT.toString())) message += "If";
                    else if (booleanExpr.getKind().equals(Kind.WHILE_STMT.toString())) message += "While";
                    message += " condition with function '%s' returning type '%s'";
                    if (returnType.isArray()) message += " array";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(booleanExpr),
                            NodeUtils.getColumn(booleanExpr),
                            String.format(message, condition.getChild(1).get("id"), returnType.getName()),
                            null)
                    );
                    return null;
                }
            }
            else if (!(condition.getKind().equals(Kind.BOOLEAN.toString()) || condition.getKind().equals(Kind.BOOLEAN_EXPR.toString()) || condition.getKind().equals(Kind.COMPARISON_EXPR.toString()) || condition.getKind().equals(Kind.UNARY_OP.toString()))){
                var message = "";
                if (booleanExpr.getKind().equals(Kind.IF_STMT.toString())) message += "If";
                else if (booleanExpr.getKind().equals(Kind.WHILE_STMT.toString())) message += "While";
                message += " condition with statement of type '%s'";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(booleanExpr),
                        NodeUtils.getColumn(booleanExpr),
                        String.format(message, condition.getKind()),
                        null)
                );
                return null;
            }
        }
        catch(Exception e){
            var message = String.format("Unexpected boolean condition error.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(booleanExpr),
                    NodeUtils.getColumn(booleanExpr),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }


}
