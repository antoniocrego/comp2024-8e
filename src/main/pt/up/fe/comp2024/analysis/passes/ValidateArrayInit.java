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
import java.util.List;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ValidateArrayInit extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_INIT, this::visitArrayInit);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayInit(JmmNode arrayInit, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        try{
            var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
            megaTable.addAll(table.getParameters(currentMethod));
            megaTable.addAll(table.getFields());
            List<JmmNode> arrayInitArgs = arrayInit.getChild(0).getChildren();
            List<JmmNode> toBeExplored = new ArrayList<>();
            while (!arrayInitArgs.isEmpty()) {
                JmmNode top = arrayInitArgs.remove(0);
                if (top.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                    toBeExplored.add(top);
                }
                else if (top.getKind().equals(Kind.FUNC_CALL.toString())){
                    var methodVariable = top.getChild(0).get("name");
                    var methodName = top.get("id");
                    Type methodCallerType = new Type("", false);
                    if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
                    else if (table.getImports().contains(methodVariable)) continue;
                    else{
                        for (var element : megaTable) {
                            if (element.getName().equals(methodVariable)) {
                                methodCallerType = element.getType();
                                break;
                            }
                        }
                    }
                    if (!methodCallerType.isArray() && !methodCallerType.getName().equals(table.getClassName())) continue;
                    var returnType = table.getReturnType(methodName);
                    if (!returnType.getName().equals("int") || returnType.isArray()){
                        var message = "Initialization of array of type 'int' with function '%s' as member of invalid return type '%s'";
                        if (returnType.isArray()) message += " array";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(arrayInit),
                                NodeUtils.getColumn(arrayInit),
                                String.format(message, methodName,returnType.getName()),
                                null)
                        );
                        return null;
                    }
                }
                else if (!(top.getKind().equals(Kind.INTEGER_LITERAL.toString()) || top.getKind().equals(Kind.BINARY_EXPR.toString()) || top.getKind().equals(Kind.ARRAY_ACCESS.toString()))) {
                    var message = String.format("Initialization of array of type 'int' with member of invalid type '%s'.", top.getKind());
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(arrayInit),
                            NodeUtils.getColumn(arrayInit),
                            message,
                            null)
                    );
                    return null;
                }
            }
            if (!toBeExplored.isEmpty()) {
                for (var element : megaTable) {
                    if (toBeExplored.stream().anyMatch(node -> element.getName().equals(node.get("name")))) {
                        if (element.getType().isArray()) {
                            var message = "Initialization of array of type 'int' with member of type array.";
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(arrayInit),
                                    NodeUtils.getColumn(arrayInit),
                                    message,
                                    null)
                            );
                            return null;
                        } else if (!element.getType().getName().equals("int")) {
                            var message = String.format("Initialization of array of type 'int' with member of invalid type '%s'.", element.getType().getName());
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(arrayInit),
                                    NodeUtils.getColumn(arrayInit),
                                    message,
                                    null)
                            );
                            return null;
                        }
                    }
                }
            }
        }
        catch(Exception e){
            var message = String.format("Unexpected array initialization error.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayInit),
                    NodeUtils.getColumn(arrayInit),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

}
