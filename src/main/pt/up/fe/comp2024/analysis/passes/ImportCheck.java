package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.stream.Stream;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ImportCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table){
        // Check if exists a parameter or variable declaration with the same name as the variable reference
        long occurrences = table.getImports().stream()
                .filter(importt -> importt.equals(importDecl.get("name")))
                .count();

        if (occurrences>1){
            var message = String.format("Importing class '%s' multiple times", importDecl.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importDecl),
                    NodeUtils.getColumn(importDecl),
                    message,
                    null)
            );
        }

        if (importDecl.get("name").equals(table.getClassName())){
            var message = String.format("Importing class '%s' with another definition in file", importDecl.get("name"));
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(importDecl),
                    NodeUtils.getColumn(importDecl),
                    message,
                    null)
            );
        }

        return null;
    }


}
