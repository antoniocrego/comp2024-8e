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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class AssignmentCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignment);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAssignment(JmmNode assign, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        String varRefName;
        boolean forceNotArray = false;
        if (assign.getChild(0).getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            varRefName = assign.getChild(0).get("name");
        }
        else if (assign.getChild(0).getKind().equals(Kind.ARRAY_ACCESS.toString())){
            varRefName = assign.getChild(0).getChild(0).get("name");
            forceNotArray = true;
        }
        else {
            var message = "Assignment to non-variable on the left hand side.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assign),
                    NodeUtils.getColumn(assign),
                    message,
                    null)
            );
            return null;
        }

        var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
        megaTable.addAll(table.getParameters(currentMethod));
        megaTable.addAll(table.getFields());

        String elementType = "";
        boolean isArray = false;
        boolean isCustom = false;

        for (var element : megaTable) {
            if (element.getName().equals(varRefName)) {
                if (element.getType().isArray() && !forceNotArray) isArray=true;
                elementType = element.getType().getName();
                isCustom = !element.getType().getName().equals("int") && !element.getType().getName().equals("boolean");
                break;
            }
        }
        if (elementType.isEmpty()) return null;
        JmmNode assignExpr = assign.getChild(1);
        while (assignExpr.getKind().equals(Kind.PAREN_EXPR.toString())){
            assignExpr = assign.getChild(0);
        }
        if (assignExpr.getKind().equals(Kind.FUNC_CALL.toString())) {
            var methodVariable = assignExpr.getChild(0).get("name");
            var methodName = assignExpr.get("id");
            Type methodCallerType = new Type("", false);
            if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
            else if (table.getImports().contains(methodVariable)) return null;
            else {
                for (var element : megaTable) {
                    if (element.getName().equals(methodVariable)) {
                        methodCallerType = element.getType();
                        break;
                    }
                }
            }
            if (!methodCallerType.getName().equals(table.getClassName()) && !table.getImports().contains(methodCallerType.getName())) {
                var message = String.format("Assignment of variable '%s' to return value of undefined function '%s' of object of unknown class '%s'", varRefName, methodName, methodCallerType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assign),
                        NodeUtils.getColumn(assign),
                        message,
                        null)
                );
                return null;
            } else if (methodCallerType.getName().equals(table.getClassName()) && !table.getMethods().contains(methodName) && table.getSuper() == null) {
                var message = String.format("Assignment of variable '%s' to return value of undefined function '%s' of object of class '%s'", varRefName, methodName, methodCallerType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assign),
                        NodeUtils.getColumn(assign),
                        message,
                        null)
                );
                return null;
            } else if (methodCallerType.getName().equals(table.getClassName()) && table.getMethods().contains(methodName)) {
                if (table.getSuper() != null && elementType.equals(table.getSuper()) && table.getReturnType(methodName).getName().equals(table.getClassName()))
                    return null;
                if (!table.getReturnType(methodName).equals(new Type(elementType, isArray))) {
                    var message = "Assignment of variable '%s' of type '%s'";
                    if (isArray) message += " array";
                    message += " to return value of function '%s' of type '%s'";
                    if (table.getReturnType(methodName).isArray()) message += " array";
                    message = String.format(message, varRefName, elementType, methodName, table.getReturnType(methodName).getName());
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assign),
                            NodeUtils.getColumn(assign),
                            message,
                            null)
                    );
                    return null;
                }
            }
        }
        else if (isArray && !assignExpr.getKind().equals(Kind.ARRAY_INIT.toString())) {
            if (assignExpr.getKind().equals(Kind.NEW_ARRAY.toString())) {
                if (!assignExpr.getChild(0).get("id").equals(elementType)) {
                    var message = String.format("Initialization of array '%s' with array initializer of type '%s'.", varRefName, assignExpr.getChild(0).get("id"));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(assign),
                            NodeUtils.getColumn(assign),
                            message,
                            null)
                    );
                    return null;
                }
                return null;
            }
            var message = String.format("Initialization of array '%s' with non-array structure.", varRefName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assign),
                    NodeUtils.getColumn(assign),
                    message,
                    null)
            );
            return null;
        } else if (isArray && assignExpr.getKind().equals(Kind.ARRAY_INIT.toString())) {
            return null;
        } else if (assignExpr.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            var secondName = assignExpr.get("name");
            for (var element : megaTable) {
                if (element.getName().equals(secondName)) {
                    if (!element.getType().getName().equals(elementType)) {
                        if (!(elementType.equals(table.getSuper()) && element.getType().getName().equals(table.getClassName())) && !(new HashSet<>(table.getImports()).containsAll(Arrays.asList(elementType, element.getType().getName())) && !(elementType.equals(table.getClassName()) && element.getType().getName().equals(table.getSuper())))) {
                            var message = String.format("Assignment of type '%s' to variable '%s' of type '%s'.", element.getType().getName(), varRefName, elementType);
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(assign),
                                    NodeUtils.getColumn(assign),
                                    message,
                                    null)
                            );
                            return null;
                        }
                    }
                    break;
                }
            }
        }
        else if (assignExpr.getKind().equals(Kind.NEW_CLASS.toString()) && !assignExpr.get("id").equals(elementType) && !(elementType.equals(table.getSuper()) && assignExpr.get("id").equals(table.getClassName())) && (!new HashSet<>(table.getImports()).containsAll(Arrays.asList(elementType, assignExpr.get("id"))))) {
            var message = String.format("Assignment of type '%s' to variable '%s' of type '%s'.", assignExpr.get("id"), varRefName, elementType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assign),
                    NodeUtils.getColumn(assign),
                    message,
                    null)
            );
            return null;
        }
        else if (elementType.equals("int") && !isArray && !(assignExpr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || assignExpr.getKind().equals(Kind.BINARY_EXPR.toString()) || assignExpr.getKind().equals(Kind.ARRAY_ACCESS.toString()) || assignExpr.getKind().equals(Kind.LENGTH_EXPR.toString()))) {
            var message = String.format("Assignment of type '%s' to variable '%s' of type int.", assignExpr.getKind(), varRefName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assign),
                    NodeUtils.getColumn(assign),
                    message,
                    null)
            );
            return null;
        } else if (elementType.equals("boolean") && !isArray && !(assignExpr.getKind().equals(Kind.BOOLEAN.toString()) || assignExpr.getKind().equals(Kind.BOOLEAN_EXPR.toString()) || assignExpr.getKind().equals(Kind.COMPARISON_EXPR.toString()) || assignExpr.getKind().equals(Kind.UNARY_OP.toString()))) {
            var message = String.format("Assignment of type '%s' to variable '%s' of type bool.", assignExpr.getKind(), varRefName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assign),
                    NodeUtils.getColumn(assign),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }


}
