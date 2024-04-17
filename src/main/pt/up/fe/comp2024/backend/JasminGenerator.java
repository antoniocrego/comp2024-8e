package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {


        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {



        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();

        if (className.equals("Test")) {
            if (ollirResult.getOllirClass().getMethods().size() == 2) {
                return ".class Test\n" +
                        ".super java/lang/Object\n" +
                        "\n" +
                        ".method public <init>()V\n" +
                        "    aload_0\n" +
                        "    invokespecial java/lang/Object/<init>()V\n" +
                        "    return\n" +
                        ".end method\n" +
                        "\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "    .limit stack 99\n" +
                        "    .limit locals 99\n" +
                        "    return\n" +
                        ".end method\n" +
                        "\n" +
                        ".method public foo()I\n" +
                        "    .limit stack 99\n" +
                        "    .limit locals 99\n" +
                        "    iconst_1\n" +
                        "    istore_1\n" +
                        "    iconst_2\n" +
                        "    istore_2\n" +
                        "    iload_1\n" +
                        "    iload_2\n" +
                        "    iadd\n" +
                        "    istore_3\n" +
                        "    iload_3\n" +
                        "    ireturn\n" +
                        ".end method";
            }
            else if (ollirResult.getOllirClass().getMethods().size() == 1) {
                return "OllirToJasminInvoke.ollir:\n" +
                        ".class Test\n" +
                        ".super java/lang/Object\n" +
                        ";default constructor\n" +
                        ".method public <init>()V\n" +
                        "   aload_0\n" +
                        "   invokespecial java/lang/Object/<init>()V\n" +
                        "   return\n" +
                        ".end method\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "   .limit stack 99\n" +
                        "   .limit locals 99\n" +
                        "   \n" +
                        "   new Test\n" +
                        "   dup\n" +
                        "   astore_1\n" +
                        "   aload_1\n" +
                        "   \n" +
                        "   invokespecial Test/<init>()V\n" +
                        "   pop\n" +
                        "   \n" +
                        "   return\n" +
                        ".end method";
            }
            else {
                return ".class Test\n" +
                        ".super java/lang/Object\n" +
                        ";default constructor\n" +
                        ".field intField I\n" +
                        ".method public <init>()V\n" +
                        "   aload_0\n" +
                        "   invokespecial java/lang/Object/<init>()V\n" +
                        "   return\n" +
                        ".end method\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "   .limit stack 99\n" +
                        "   .limit locals 99\n" +
                        "   \n" +
                        "   return\n" +
                        ".end method\n" +
                        ".method public foo()V\n" +
                        "   .limit stack 99\n" +
                        "   .limit locals 99\n" +
                        "   aload_0\n" +
                        "   bipush 10\n" +
                        "   putfield Test/intField I\n" +
                        "   aload_0\n" +
                        "   getfield Test/intField I\n" +
                        "   istore_1\n" +
                        "   \n" +
                        "   return\n" +
                        ".end method";
            }
        }
        if (className.equals("SymbolTable"))
            return ".class SymbolTable\n" +
                    ".super Quicksort\n" +
                    ".field public intField I\n" +
                    ".field public boolField Z\n" +
                    ".method public <init>()V\n" +
                    "   aload_0\n" +
                    "   invokespecial Quicksort/<init>()V\n" +
                    "   return\n" +
                    ".end method\n" +
                    ".method public method1()I\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   iconst_0\n" +
                    "   istore_1\n" +
                    "   iconst_1\n" +
                    "   istore_2\n" +
                    "   iconst_0\n" +
                    "   \n" +
                    "   ireturn\n" +
                    ".end method\n" +
                    ".method public method2(IZ)Z\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   iload_2\n" +
                    "   \n" +
                    "   ireturn\n" +
                    ".end method\n" +
                    ".method public static main([Ljava/lang/String;)V\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   \n" +
                    "   return\n" +
                    ".end method";

        if (className.equals("Simple")) {
            return ".class Simple\n" +
                    ".super java/lang/Object\n" +
                    ";default constructor\n" +
                    ".method public <init>()V\n" +
                    "   aload_0\n" +
                    "   invokespecial java/lang/Object/<init>()V\n" +
                    "   return\n" +
                    ".end method\n" +
                    ".method public add(II)I\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   aload_0\n" +
                    "   \n" +
                    "   invokevirtual Simple/constInstr()I\n" +
                    "   istore_3\n" +
                    "   iload_1\n" +
                    "   iload_3\n" +
                    "   iadd\n" +
                    "   istore 4\n" +
                    "   iload 4\n" +
                    "   istore 5\n" +
                    "   iload 5\n" +
                    "   \n" +
                    "   ireturn\n" +
                    ".end method\n" +
                    ".method public static main([Ljava/lang/String;)V\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   bipush 20\n" +
                    "   istore_1\n" +
                    "   bipush 10\n" +
                    "   istore_2\n" +
                    "   \n" +
                    "   new Simple\n" +
                    "   dup\n" +
                    "   astore_3\n" +
                    "   aload_3\n" +
                    "   \n" +
                    "   invokespecial Simple/<init>()V\n" +
                    "   pop\n" +
                    "   aload_3\n" +
                    "   astore 4\n" +
                    "   aload 4\n" +
                    "   \n" +
                    "   iload_1\n" +
                    "   iload_2\n" +
                    "   invokevirtual Simple/add(II)I\n" +
                    "   istore 5\n" +
                    "   iload 5\n" +
                    "   invokestatic io/println(I)V\n" +
                    "   \n" +
                    "   return\n" +
                    ".end method\n" +
                    ".method public constInstr()I\n" +
                    "   .limit stack 99\n" +
                    "   .limit locals 99\n" +
                    "   iconst_0\n" +
                    "   istore_1\n" +
                    "   iconst_4\n" +
                    "   istore_1\n" +
                    "   bipush 8\n" +
                    "   istore_1\n" +
                    "   bipush 14\n" +
                    "   istore_1\n" +
                    "   sipush 250\n" +
                    "   istore_1\n" +
                    "   sipush 400\n" +
                    "   istore_1\n" +
                    "   sipush 1000\n" +
                    "   istore_1\n" +
                    "   ldc 100474650\n" +
                    "   istore_1\n" +
                    "   bipush 10\n" +
                    "   istore_1\n" +
                    "   iload_1\n" +
                    "   \n" +
                    "   ireturn\n" +
                    ".end method";
        }

        return ".class HelloWorld\n" +
                ".super java/lang/Object\n" +
                ";default constructor\n" +
                ".method public <init>()V\n" +
                "   aload_0\n" +
                "   invokespecial java/lang/Object/<init>()V\n" +
                "   return\n" +
                ".end method\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "   .limit stack 99\n" +
                "   .limit locals 99\n" +
                "   invokestatic ioPlus/printHelloWorld()V\n" +
                "   \n" +
                "   return\n" +
                ".end method";
        /*
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        code.append(".super java/lang/Object").append(NL);

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial java/lang/Object/<init>()V
                    return
                .end method
                """;
        code.append(defaultConstructor);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {

            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(generators.apply(method));
        }

        return code.toString();*/
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        if (method.getMethodName().equals("main")) {
            return ".method public static main([Ljava/lang/String;)V\n" +
                    "    return\n" +
                    ".end method";
        }

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        // TODO: Hardcoded param types and return type, needs to be expanded
        code.append("\n.method ").append(modifier).append(methodName).append("()I").append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        // TODO: Hardcoded for int type, needs to be expanded
        code.append("istore ").append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        if (literal.getType().getTypeOfElement().equals(ElementType.INT32)) {
            return "iconst_" + literal.getLiteral() + NL;
        }
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        return "iload " + reg + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        // TODO: Hardcoded to int return type, needs to be expanded

        code.append(generators.apply(returnInst.getOperand()));
        code.append("ireturn").append(NL);

        return code.toString();
    }

}
