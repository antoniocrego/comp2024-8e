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
public class BooleanOperatorCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.UNARY_OP, this::visitUnaryOp);
        addVisit(Kind.BOOLEAN_EXPR, this::visitBooleanExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitUnaryOp(JmmNode unaryOp, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        try{
            var condition = unaryOp.getChild(0);
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
                            var message = "Boolean negation of variable '%s' of type '%s'";
                            if (element.getType().isArray()) message += " array";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(unaryOp),
                                    NodeUtils.getColumn(unaryOp),
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
                    var message = "Boolean negation of function '%s' returning type '%s'";
                    if (returnType.isArray()) message += " array";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(unaryOp),
                            NodeUtils.getColumn(unaryOp),
                            String.format(message, condition.getChild(1).get("id"), returnType.getName()),
                            null)
                    );
                    return null;
                }
            }
            else if (!(condition.getKind().equals(Kind.BOOLEAN.toString()) || condition.getKind().equals(Kind.BOOLEAN_EXPR.toString()) || condition.getKind().equals(Kind.COMPARISON_EXPR.toString()) || condition.getKind().equals(Kind.UNARY_OP.toString()))){
                var message = "Boolean negation of statement of type '%s'";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(unaryOp),
                        NodeUtils.getColumn(unaryOp),
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
                    NodeUtils.getLine(unaryOp),
                    NodeUtils.getColumn(unaryOp),
                    message,
                    null)
            );
            return null;
        }


        return null;
    }

    private Void visitBooleanExpr(JmmNode booleanExpr, SymbolTable table){
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        String varRefName = "";
        String varRefName2 = "";
        boolean found1 = false;
        boolean found2 = false;

        var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
        megaTable.addAll(table.getParameters(currentMethod));
        megaTable.addAll(table.getFields());

        JmmNode bool1 = booleanExpr.getChild(0);
        while(bool1.getKind().equals(Kind.PAREN_EXPR.toString())){
            bool1 = bool1.getChild(0);
        }
        if (bool1.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            varRefName = bool1.get("name");
        }
        else if (bool1.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = bool1.getChild(0).get("name");
            var methodName = bool1.get("id");
            Type methodCallerType = new Type("", false);
            if (!table.getImports().contains(methodVariable)){
                if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
                else{
                    for (var element : megaTable) {
                        if (element.getName().equals(methodVariable)) {
                            methodCallerType = element.getType();
                            break;
                        }
                    }
                }
                if (methodCallerType.getName().equals(table.getClassName())){
                    var returnType = table.getReturnType(methodName);
                    if (!returnType.getName().equals("int") || returnType.isArray()){
                        var message = "Boolean expression with function returning '%s'";
                        if (returnType.isArray()) message += " array";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(booleanExpr),
                                NodeUtils.getColumn(booleanExpr),
                                String.format(message, returnType.getName()),
                                null)
                        );
                        return null;
                    }
                }
            }
            found1 = true;
        }
        else if (!(bool1.getKind().equals(Kind.BOOLEAN.toString()) || bool1.getKind().equals(Kind.BOOLEAN_EXPR.toString()) || bool1.getKind().equals(Kind.COMPARISON_EXPR.toString()) || bool1.getKind().equals(Kind.UNARY_OP.toString()))){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(booleanExpr),
                    NodeUtils.getColumn(booleanExpr),
                    String.format("Unexpected primitive type '%s' in boolean expression.", bool1.getKind()),
                    null)
            );
            return null;
        }
        else{
            found1 = true;
        }
        JmmNode bool2 = booleanExpr.getChild(1);
        while(bool2.getKind().equals(Kind.PAREN_EXPR.toString())){
            bool2 = bool2.getChild(0);
        }
        if (bool2.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            varRefName2 = bool2.get("name");
        }
        else if (bool2.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = bool2.getChild(0).get("name");
            var methodName = bool2.get("id");
            Type methodCallerType = new Type("", false);
            if (!table.getImports().contains(methodVariable)){
                if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
                else{
                    for (var element : megaTable) {
                        if (element.getName().equals(methodVariable)) {
                            methodCallerType = element.getType();
                            break;
                        }
                    }
                }
                if (methodCallerType.getName().equals(table.getClassName())){
                    var returnType = table.getReturnType(methodName);
                    if (!returnType.getName().equals("int") || returnType.isArray()){
                        var message = "Boolean expression with function returning '%s'";
                        if (returnType.isArray()) message += " array";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(booleanExpr),
                                NodeUtils.getColumn(booleanExpr),
                                String.format(message, returnType.getName()),
                                null)
                        );
                        return null;
                    }
                }
            }
            found2 = true;
        }
        else if (!(bool2.getKind().equals(Kind.BOOLEAN.toString()) || bool2.getKind().equals(Kind.BOOLEAN_EXPR.toString()) || bool2.getKind().equals(Kind.COMPARISON_EXPR.toString()) || bool2.getKind().equals(Kind.UNARY_OP.toString()))){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(booleanExpr),
                    NodeUtils.getColumn(booleanExpr),
                    String.format("Unexpected primitive type '%s' in boolean expression.", bool2.getKind()),
                    null)
            );
            return null;
        }
        else{
            found2 = true;
        }

        for (var element : megaTable){
            if (found1 && found2) break;
            var flag = false;
            var message = "";
            if (element.getName().equals(varRefName) || element.getName().equals(varRefName2)){
                String variable = "";
                if (element.getName().equals(varRefName)){
                    variable = varRefName;
                    found1 = true;
                }
                else if (element.getName().equals(varRefName2)){
                    variable = varRefName2;
                    found2 = true;
                }
                if (element.getType().isArray()){
                    // Create error report
                    message = String.format("Boolean expression with array '%s'!", variable);
                    flag = true;
                }
                else if (element.getType().getName().equals("int")) {
                    // Create error report
                    message = String.format("Boolean expression with integer variable '%s'!", variable);
                    flag = true;
                }
                else if (!element.getType().getName().equals("boolean")){
                    // Create error report
                    message = String.format("Boolean expression with object '%s' of type '%s'!", variable, element.getType().getName());
                    flag = true;
                }
                if (flag){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(booleanExpr),
                            NodeUtils.getColumn(booleanExpr),
                            message,
                            null)
                    );
                    return null;
                }
            }
        }

        return null;
    }


}
