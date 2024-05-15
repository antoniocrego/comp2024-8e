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
public class IndexCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
        megaTable.addAll(table.getParameters(currentMethod));
        megaTable.addAll(table.getFields());
        JmmNode arrayVariable = arrayAccess.getChild(0);
        while (arrayVariable.getKind().equals(Kind.PAREN_EXPR.toString())){
            arrayVariable = arrayAccess.getChild(0);
        }
        if (arrayVariable.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = arrayVariable.getChild(0).get("name");
            var methodName = arrayVariable.get("id");
            Type methodCallerType = new Type("", false);
            if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
            else if (table.getImports().contains(methodVariable)) return null;
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
            if (!returnType.getName().equals("int") || !returnType.isArray()){
                var message = "Indexing return type '%s' of function '%s'";
                if (returnType.isArray()) message += " array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(arrayAccess),
                        NodeUtils.getColumn(arrayAccess),
                        String.format(message, returnType.getName(), methodName),
                        null)
                );
                return null;
            }
        }
        else if (!(arrayVariable.getKind().equals(Kind.VAR_REF_EXPR.toString()) || arrayVariable.getKind().equals(Kind.ARRAY_INIT.toString()) || arrayVariable.getKind().equals(Kind.NEW_ARRAY.toString()))){
            var message = "Indexing expression of type '%s', which is not an array";
            message = String.format(message, arrayAccess.getKind());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAccess),
                    NodeUtils.getColumn(arrayAccess),
                    message,
                    null)
            );
            return null;
        }
        String arrayName = "[...]";
        if (arrayVariable.getKind().equals(Kind.VAR_REF_EXPR.toString())){
            arrayName = arrayVariable.get("name");
            for (var element : megaTable){
                if (element.getName().equals(arrayName)){
                    if (!element.getType().isArray()){
                        var message = "Indexing variable '%s' of type '%s', which is not an array";
                        message = String.format(message, arrayName, element.getType().getName());
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(arrayAccess),
                                NodeUtils.getColumn(arrayAccess),
                                message,
                                null)
                        );
                        return null;
                    }
                    break;
                }
            }
        }
        JmmNode arrayIndex = arrayAccess.getChild(1);
        while (arrayIndex.getKind().equals(Kind.PAREN_EXPR.toString())){
            arrayIndex = arrayIndex.getChild(0);
        }
        boolean isInt = arrayIndex.getKind().equals(Kind.INTEGER_LITERAL.toString()) || arrayIndex.getKind().equals(Kind.BINARY_EXPR.toString()) || arrayIndex.getKind().equals(Kind.ARRAY_ACCESS.toString()) || arrayIndex.getKind().equals(Kind.LENGTH_EXPR.toString());
        if (arrayIndex.getKind().equals(Kind.VAR_REF_EXPR.toString())){
            var varRefName = arrayIndex.get("name");

            for (var element : megaTable){
                if (element.getName().equals(varRefName)){
                    if (element.getType().isArray() || !element.getType().getName().equals("int")){
                        var message = "Indexing array '%s' with variable '%s' of type '%s'";
                        if (element.getType().isArray()) message+=" array";
                        message = String.format(message, arrayName, varRefName, element.getType().getName());
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(arrayAccess),
                                NodeUtils.getColumn(arrayAccess),
                                message,
                                null)
                        );
                        return null;
                    }
                    isInt = true;
                    break;
                }
            }
        }
        else if (arrayIndex.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = arrayIndex.getChild(0).get("name");
            var methodName = arrayIndex.get("id");
            Type methodCallerType = new Type("", false);
            if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
            else if (table.getImports().contains(methodVariable)) return null;
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
            if (!returnType.getName().equals("int") || returnType.isArray()){
                var message = "Indexing array '%s' with return value of function '%s' of type '%s'";
                if (returnType.isArray()) message += " array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(arrayAccess),
                        NodeUtils.getColumn(arrayAccess),
                        String.format(message, arrayName, methodName, returnType.getName()),
                        null)
                );
                return null;
            }
            isInt = true;
        }
        if (!isInt){
            var message = "Indexing array '%s' with expression resulting type '%s'";
            message = String.format(message, arrayName, arrayIndex.getKind());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAccess),
                    NodeUtils.getColumn(arrayAccess),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }


}
