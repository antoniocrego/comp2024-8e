package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ValidDeclarationCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
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

        return null;
    }


}
