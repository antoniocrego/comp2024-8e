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
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class MathCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.COMPARISON_EXPR, this::visitBinaryExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        String varRefName = "";
        String varRefName2 = "";
        boolean found1 = false;
        boolean found2 = false;

        var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
        megaTable.addAll(table.getParameters(currentMethod));
        megaTable.addAll(table.getFields());

        JmmNode lhs = binaryExpr.getChild(0);
        JmmNode rhs = binaryExpr.getChild(1);

        while (lhs.getKind().equals(Kind.PAREN_EXPR.toString())){
            lhs = lhs.getChild(0);
        }
        while (rhs.getKind().equals(Kind.PAREN_EXPR.toString())){
            rhs = rhs.getChild(0);
        }

        if (lhs.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            varRefName = lhs.get("name");
        }
        else if (lhs.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = lhs.getChild(0).get("name");
            var methodName = lhs.get("id");
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
                        var message = "Binary expression with function returning '%s'";
                        if (returnType.isArray()) message += " array";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(binaryExpr),
                                NodeUtils.getColumn(binaryExpr),
                                String.format(message, returnType.getName()),
                                null)
                        );
                        return null;
                    }
                }
            }
            found1 = true;
        }
        else if (!(lhs.getKind().equals(Kind.INTEGER_LITERAL.toString()) || lhs.getKind().equals(Kind.ARRAY_ACCESS.toString()) || lhs.getKind().equals(Kind.LENGTH_EXPR.toString()) || lhs.getKind().equals(Kind.BINARY_EXPR.toString()))){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    String.format("Unexpected primitive type '%s' in binary expression.", lhs.getKind()),
                    null)
            );
            return null;
        }
        else{
            found1 = true;
        }
        if (rhs.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            varRefName2 = rhs.get("name");
        }
        else if (rhs.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = rhs.getChild(0).get("name");
            var methodName = rhs.get("id");
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
                        var message = "Binary expression with function returning '%s'";
                        if (returnType.isArray()) message += " array";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(binaryExpr),
                                NodeUtils.getColumn(binaryExpr),
                                String.format(message, returnType.getName()),
                                null)
                        );
                        return null;
                    }
                }
            }
            found2 = true;
        }
        else if (!(rhs.getKind().equals(Kind.INTEGER_LITERAL.toString()) || rhs.getKind().equals(Kind.ARRAY_ACCESS.toString()) || rhs.getKind().equals(Kind.LENGTH_EXPR.toString()) || rhs.getKind().equals(Kind.BINARY_EXPR.toString()))){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    String.format("Unexpected primitive type '%s' in binary expression.", rhs.getKind()),
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
                    message = String.format("Binary operation with array '%s'!", variable);
                    flag = true;
                }
                else if (element.getType().getName().equals("boolean")) {
                    // Create error report
                    message = String.format("Binary operation with boolean variable '%s'!", variable);
                    flag = true;
                }
                else if (!element.getType().getName().equals("int")){
                    // Create error report
                    message = String.format("Binary operation with object '%s' of type '%s'!", variable, element.getType().getName());
                    flag = true;
                }
                if (flag){
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(binaryExpr),
                            NodeUtils.getColumn(binaryExpr),
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
