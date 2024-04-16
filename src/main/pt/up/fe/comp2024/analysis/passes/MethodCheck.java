package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class MethodCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNC_CALL, this::visitFuncCall);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitFuncCall(JmmNode funcCall, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        try{
            var methodVariable = funcCall.getChild(0).get("name");
            var methodName = funcCall.get("id");
            Type methodCallerType = new Type("", false);
            if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
            else if (table.getImports().contains(methodVariable)) return null;
            else {
                var megaTable = new ArrayList<>(table.getFields());
                megaTable.addAll(table.getParameters(currentMethod));
                megaTable.addAll(table.getLocalVariables(currentMethod));

                for (var element : megaTable){
                    if (element.getName().equals(methodVariable)){
                        methodCallerType = element.getType();
                        break;
                    }
                }
            }
            if (methodCallerType.getName().equals("int") || methodCallerType.getName().equals("boolean") || methodCallerType.isArray()){
                var message = "Calling function from variable of type '%s'";
                if (methodCallerType.isArray()) message+=" array";
                if (methodCallerType.getName().equals("int") || methodCallerType.getName().equals("boolean")) message+=", which is not an object";
                message = String.format(message, methodCallerType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(funcCall),
                        NodeUtils.getColumn(funcCall),
                        message,
                        null)
                );
                return null;
            }
            else if (!methodCallerType.getName().equals(table.getClassName()) && !table.getImports().contains(methodCallerType.getName())){
                var message = String.format("Call to undefined function '%s' of object of unknown class '%s'",methodName,methodCallerType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(funcCall),
                        NodeUtils.getColumn(funcCall),
                        message,
                        null)
                );
                return null;
            }
            else if (methodCallerType.getName().equals(table.getClassName()) && !table.getMethods().contains(methodName) && table.getSuper()==null){
                var message = String.format("Call to undefined function '%s' of object of class '%s'", methodName, table.getClassName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(funcCall),
                        NodeUtils.getColumn(funcCall),
                        message,
                        null)
                );
                return null;
            }
        }
        catch(Exception e){
            var message = String.format("Unexpected method call error.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(funcCall),
                    NodeUtils.getColumn(funcCall),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

}
