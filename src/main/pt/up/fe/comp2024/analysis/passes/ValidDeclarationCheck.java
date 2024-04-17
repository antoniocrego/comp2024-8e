package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.stream.Stream;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ValidDeclarationCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table){
        // Check if exists a parameter or variable declaration with the same name as the variable reference
        if (!(varDecl.getChild(0).getKind().equals(Kind.CUSTOM_TYPE.toString()) || varDecl.getChild(0).getKind().equals(Kind.ARRAY_TYPE.toString()) || varDecl.getChild(0).getKind().equals(Kind.PRIMITIVE_TYPE.toString()))){
            var message = String.format("Invalid type for variable declaration '%s'", varDecl.getChild(0).getKind());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null)
            );
            return null;
        }

        if(currentMethod==null) {
            long occurrences = table.getFields().stream()
                    .filter(field -> field.getName().equals(varDecl.get("name")))
                    .count();

            if (occurrences > 1) {
                var message = String.format("Redeclaration of field of name '%s'", varDecl.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }
        }
        else{
            long occurrences = Stream.concat(
                            table.getParameters(currentMethod).stream(),
                            table.getLocalVariables(currentMethod).stream()
                    )
                    .filter(field -> field.getName().equals(varDecl.get("name")))
                    .count();

            if (occurrences > 1) {
                var message = String.format("Redeclaration of variable of name '%s'", varDecl.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varDecl),
                        NodeUtils.getColumn(varDecl),
                        message,
                        null)
                );
                return null;
            }
        }

        return null;
    }


}
