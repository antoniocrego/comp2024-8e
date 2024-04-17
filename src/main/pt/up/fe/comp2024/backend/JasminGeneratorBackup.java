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
public class JasminGeneratorBackup {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGeneratorBackup(OllirResult ollirResult) {
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
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(FieldInstruction.class, this::generateField);
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
        code.append(".class ").append(className).append(NL).append(NL);

        // TODO: Hardcoded to Object, needs to be expanded
        // DONE ?
        code.append(".super ");
        if (classUnit.getSuperClass() != null)
            code.append(classUnit.getSuperClass());
        else
            code.append("java/lang/Object");
        code.append(NL);

        // generate a single constructor method
        var superClass = classUnit.getSuperClass() == null ? "java/lang/Object" : classUnit.getSuperClass();
        var defaultConstructor = """
        .method public <init>()V
            aload_0
            invokespecial %s/<init>()V
            return
        .end method
        """.formatted(superClass);
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

        return code.toString();
    }


    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var isStatic = method.isStaticMethod();
        var isFinal = method.isFinalMethod();

        var methodName = method.getMethodName();

        var returnType = getType(method.getReturnType());
        var paramTypes = method.getParams().stream()
                .map(param -> getType(param.getType()))
                .collect(Collectors.joining());

        // TODO: Hardcoded param types and return type, needs to be expanded
        // DONE ?
        code
                .append("\n.method ")
                .append(modifier)
                .append(isFinal ? "final " : "")
                .append(isStatic ? "static " : "")
                .append(methodName)
                .append("(")
                .append(paramTypes)
                .append(")")
                .append(returnType)
                .append(NL);

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
        // DONE ?
        code.append(switch (assign.getTypeOfAssign().getTypeOfElement()) {
            case INT32, BOOLEAN -> "i";
            case VOID -> "";
            default -> "a";
        }).append("store ").append(reg).append(NL);

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
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
        // DONE ?
        if (returnInst.getOperand() != null)
            code.append(generators.apply(returnInst.getOperand()));
        code.append(switch (returnInst.getElementType()) {
            case INT32, BOOLEAN -> "i";
            case VOID -> "";
            default -> "a";
        }).append("return").append(NL);


        return code.toString();
    }

    private String generateCall(CallInstruction callInst) {
        var code = new StringBuilder();

        switch (callInst.getInvocationType()) {
            case invokevirtual -> {
                // dar load ao objeto
//
//                Operand caller = (Operand) callInst.getCaller();
//                code
//                        .append("invokevirtual ")
//                        .append(caller.getName())
//                        .append("(")
////                        .append(args)
//                        .append(")")
//                        .append(getType(callInst.getReturnType()))
//                        .append(NL);
            }
            case invokestatic -> {
//                var caller = (Operand) callInst.getCaller();
//                code
//                        .append("invokestatic ")
//                        .append(caller.getName())
//                        .append("(")
////                        .append(args)
//                        .append(")")
//                        .append(getType(callInst.getReturnType()))
//                        .append(NL);
            }
            case invokespecial -> {
                var caller = (Operand) callInst.getCaller();
                var className = ((ClassType) caller.getType()).getName();
//                var funcPath = getFuncPath(className);
//                var funcName = className
//                var methodName = (LiteralElement) callInst.getMethodName();
                var reg = currentMethod.getVarTable().get(caller.getName()).getVirtualReg();
                code
                        .append("aload ")
                        .append(reg)
                        .append(NL)
                        .append("invokespecial ")
//                        .append(funcPath)
//                        .append(methodName.getLiteral())
                        .append(className)
                        .append("/<init>()")
                        .append(getType(callInst.getReturnType()))
                        .append(NL);
            }
            case NEW -> {
                var caller = (Operand) callInst.getCaller();
                code
                        .append("new ")
                        .append(caller.getName())
                        .append(NL);
            }
            default -> throw new NotImplementedException(callInst.getClass());
        };



        return code.toString();
    }

    private String generateField(FieldInstruction fieldInstruction) {
        StringBuilder code = new StringBuilder();

        switch (fieldInstruction.getInstType()) {
            case PUTFIELD -> {
                Element op1 = fieldInstruction.getOperands().get(0);
                Element op2 = fieldInstruction.getOperands().get(1);
                Element op3 = fieldInstruction.getOperands().get(2);
                code
                        .append(loadInstruction(op1))
                        .append(loadInstruction(op3))
                        .append("putfield ")
                        .append(getFieldSignature(fieldInstruction))
                        .append(NL);
            }
            case GETFIELD -> {
                Element op1 = fieldInstruction.getOperands().get(0);
                Element op2 = fieldInstruction.getOperands().get(1);
                code
                        .append(loadInstruction(op1))
                        .append("getfield ")
                        .append(getFieldSignature(fieldInstruction))
                        .append(NL);
            }
            default -> throw new NotImplementedException(fieldInstruction.getClass());
        }

        return code.toString();
    }

    private String getFieldSignature(FieldInstruction fieldInstruction) {
        StringBuilder signature = new StringBuilder();

        List<Element> operandsList = fieldInstruction.getOperands();
        Operand firstOp = (Operand) operandsList.get(0);
        Operand secondOp = (Operand) operandsList.get(1);
        String className = ((ClassType) firstOp.getType()).getName();
        String fieldName = secondOp.getName();
        String returnType = getType(secondOp.getType());

        signature
                .append(className)
                .append("/")
                .append(fieldName)
                .append(" ")
                .append(returnType);

        return signature.toString();
    }

    private String getFuncPath(String className) {
        String path = "";
        for (String importPath : ollirResult.getOllirClass().getImports())
            if (importPath.endsWith(className)) path = importPath;
        if (path.isEmpty())
            return className + "/";
        return path.replace('.', '/') + "/";
    }

    private String loadInstruction(Element element) {
        StringBuilder code = new StringBuilder();
        if (element instanceof LiteralElement) {
            LiteralElement literal = (LiteralElement) element;
            ElementType type = element.getType().getTypeOfElement();
            switch (type) {
                case INT32, BOOLEAN -> {
                    int val = Integer.parseInt(literal.getLiteral());
                    if (val == -1) code.append("iconst_m1");
                    else if (val >= 0 && val <= 5) code.append("iconst_").append(val);
                    else if (val >= -128 && val <= 127) code.append("bipush ").append(val);
                    else if (val >= -32768 && val <= 32767) code.append("sipush ").append(val);
                    else code.append("ldc ").append(val);
                }
                default -> throw new NotImplementedException(element);
            }
        } else if (element instanceof Operand) {
            code
                    .append(switch (element.getType().getTypeOfElement()) {
                        case INT32, BOOLEAN -> code
                                .append("iload ")
                                .append(getIndex(((Operand) element).getName()));
                        case OBJECTREF, STRING, ARRAYREF, CLASS -> code
                                .append("aload ")
                                .append(getIndex(((Operand) element).getName()));
                        case THIS -> "aload_0";
                        default -> throw new NotImplementedException(element);
                    });
        }
        code.append(NL);
        return code.toString();
    }

    private String getIndex(String name) {
        return String.valueOf(currentMethod.getVarTable().get(name).getVirtualReg());
    }

    private String getType(Type type) {
        return switch (type.getTypeOfElement()) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case ARRAYREF -> {
                ArrayType arrayType = (ArrayType) type;
                yield "[" + getType(arrayType.getElementType());
            }
            case OBJECTREF, CLASS -> {
                ClassType classType = (ClassType) type;
                yield "L" + classType.getName();
            }
            case THIS -> null;
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
        };
    }

}
