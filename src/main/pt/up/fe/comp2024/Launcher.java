package pt.up.fe.comp2024;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp2024.analysis.JmmAnalysisImpl;
import pt.up.fe.comp2024.backend.JasminBackendImpl;
import pt.up.fe.comp2024.optimization.JmmOptimizationImpl;
import pt.up.fe.comp2024.parser.JmmParserImpl;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

import java.util.HashMap;
import java.util.Map;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        new OllirResult("import B;\n" +
                "\n" +
                "A {\n" +
                "\n" +
                "    .field public b.i32;\n" +
                "\n" +
                "    .method public foo(a.i32, b.bool, c.i32).i32 {\n" +
                "\n" +
                "        ret.i32 0.i32;\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    .method public testing().i32 {\n" +
                "\n" +
                "        t1.i32 :=.i32 getfield(this.A, b.i32).i32;\n" +
                "        t2.i32 :=.i32 invokevirtual(this.A, \"testing\").i32;\n" +
                "        a.i32 :=.i32 invokevirtual(this.A, \"foo\", t1.i32, 1.bool, t2.i32).i32;\n" +
                "\n" +
                "        ret.i32 0.i32;\n" +
                "\n" +
                "    }\n" +
                "\n" +
                "    .construct A().V {\n" +
                "        invokespecial(this, \"<init>\").V;\n" +
                "    }\n" +
                "}", new HashMap<>());

        Map<String, String> config = CompilerConfig.parseArgs(args);

        var inputFile = CompilerConfig.getInputFile(config).orElseThrow();
        if (!inputFile.isFile()) {
            throw new RuntimeException("Option '-i' expects a path to an existing input file, got '" + args[0] + "'.");
        }
        String code = SpecsIo.read(inputFile);

        // Parsing stage
        JmmParserImpl parser = new JmmParserImpl();
        JmmParserResult parserResult = parser.parse(code, config);
        //TestUtils.noErrors(parserResult.getReports());

        // Print AST
        //System.out.println(parserResult.getRootNode().toTree());

        // Semantic Analysis stage
        JmmAnalysisImpl sema = new JmmAnalysisImpl();
        JmmSemanticsResult semanticsResult = sema.semanticAnalysis(parserResult);
        System.out.println(semanticsResult.getReports());
        TestUtils.noErrors(semanticsResult.getReports());

        //Print AST resulting from semantic analysis
        //System.out.println(semanticsResult.getRootNode().toTree());

        // Optimization stage
        JmmOptimizationImpl ollirGen = new JmmOptimizationImpl();
        OllirResult ollirResult = ollirGen.toOllir(semanticsResult);
        TestUtils.noErrors(ollirResult.getReports());

        // Print OLLIR code
        System.out.println(ollirResult.getOllirCode());

        // Code generation stage
        JasminBackendImpl jasminGen = new JasminBackendImpl();
        JasminResult jasminResult = jasminGen.toJasmin(ollirResult);
        TestUtils.noErrors(jasminResult.getReports());

        // Print Jasmin code
        System.out.println(jasminResult.getJasminCode());
    }

}
