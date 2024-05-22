package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var optimizedAST = optimize(semanticsResult);
        var ollirCode = visitor.visit(optimizedAST.getRootNode());

        return new OllirResult(optimizedAST, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        int maxRegisters = Integer.parseInt(ollirResult.getConfig().getOrDefault("registerAllocation", "-1"));



        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        var ASTConstantFolder = new ASTConstantFolder();
        var ASTConstantPropagation = new ASTConstantPropagation();
        if (semanticsResult.getConfig().getOrDefault("optimize", "false").equals("false")) {
            return semanticsResult;
        }
        boolean optimized;
        do{
            optimized = false;
            optimized |= ASTConstantFolder.visit(semanticsResult.getRootNode(), null); // if any optimization is done, the loop will continue
            optimized |= ASTConstantPropagation.visit(semanticsResult.getRootNode(), null);
        }
        while(optimized);
        return semanticsResult;
    }
}
