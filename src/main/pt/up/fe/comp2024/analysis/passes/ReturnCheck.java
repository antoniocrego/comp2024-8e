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
public class ReturnCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturn);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitReturn(JmmNode returnStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        try{
            String varRefName = "";
            var isArray = false;
            Type elementType = new Type("",false);
            Type expectedReturnType = table.getReturnType(returnStmt.getParent().get("name"));
            if (returnStmt.getChild(0).getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                varRefName = returnStmt.getChild(0).get("name");
                var megaTable = new ArrayList<>(table.getFields());
                megaTable.addAll(table.getParameters(currentMethod));
                megaTable.addAll(table.getLocalVariables(currentMethod));

                for (var element : megaTable){
                    if (element.getName().equals(varRefName)){
                        elementType = element.getType();
                        break;
                    }
                }
            }
            else if (returnStmt.getChild(0).getKind().equals(Kind.FUNC_CALL.toString())){
                elementType = table.getReturnType(returnStmt.getChild(0).get("id"));
                if (elementType == null) return null;
            }
            else if (returnStmt.getChild(0).getKind().equals(Kind.INTEGER_LITERAL.toString()) || returnStmt.getChild(0).getKind().equals(Kind.BINARY_EXPR.toString())){
                if (!expectedReturnType.equals(new Type("int", false))){
                    var message = String.format("Return value of type '%s' given for function '%s' of return type '%s'","int",returnStmt.getParent().get("name"),returnStmt.getParent().getChild(0).get("id"));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(returnStmt),
                            NodeUtils.getColumn(returnStmt),
                            message,
                            null)
                    );
                    return null;
                }
            }
            else if (returnStmt.getChild(0).getKind().equals(Kind.BOOLEAN.toString())){
                if (!expectedReturnType.equals(new Type("boolean", false))){
                    var message = String.format("Return value of type '%s' given for function '%s' of return type '%s'","boolean",returnStmt.getParent().get("name"),returnStmt.getParent().getChild(0).get("id"));
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(returnStmt),
                            NodeUtils.getColumn(returnStmt),
                            message,
                            null)
                    );
                    return null;
                }
            }
            if (elementType.getName().isEmpty()) return null;
            if (!elementType.equals(expectedReturnType)){
                var message = "Return value of type '%s' ";
                if (elementType.isArray()) message+="array ";
                message+="given for function '%s' of return type '%s'";
                if (expectedReturnType.isArray()) message+=" array";
                message = String.format(message, elementType.getName(), returnStmt.getParent().get("name"), expectedReturnType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmt),
                        NodeUtils.getColumn(returnStmt),
                        message,
                        null)
                );
                return null;
            }
        }
        catch(Exception e){
            var message = String.format("Unexpected return error.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }


}
