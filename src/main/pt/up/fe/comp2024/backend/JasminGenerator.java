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
        generators.put(LiteralElement.class, this::generateLoadInstruction);
        generators.put(Operand.class, this::generateLoadInstruction);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(FieldInstruction.class, this::generateFieldInstruction);
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

        var className = ollirResult.getOllirClass().getClassName();
        var superClass = classUnit.getSuperClass() == null || classUnit.getSuperClass().equals("Object")
                ? "java/lang/Object"
                : classUnit.getSuperClass();
        var fields = classUnit.getFields().stream()
                .map(this::getFieldSignature)
                .collect(Collectors.joining());

        code
                .append(".class ").append(className).append(NL)
                .append(".super ").append(superClass).append(NL)
                .append(fields)
                .append(".method public <init>()V").append(NL)
                .append(TAB).append("aload_0").append(NL)
                .append(TAB).append("invokespecial %s/<init>()V".formatted(superClass)).append(NL)
                .append(TAB).append("return").append(NL)
                .append(".end method").append(NL);

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

            code
                    .append(instCode)
                    .append(
                            inst.getInstType() == InstructionType.CALL
                                    &&
                                    ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID
                                    ? "pop" + NL
                                    : ""
                    );
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

        code.append(generateStoreInstruction((Operand) lhs));

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

//    private String generateLiteral(LiteralElement literal) {
//        return generateLoadInstruction(literal);
//    }

//    private String generateOperand(Operand operand) {
//        return generateLoadInstruction(operand);
//    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

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
        var params = new StringBuilder();
        switch (callInst.getInvocationType()) {
            case invokevirtual -> {
                var caller = (Operand) callInst.getCaller();
                var callerClass = ((ClassType) caller.getType()).getName();
                var method = (LiteralElement) callInst.getMethodName();
                var paramTypes = callInst.getArguments().stream()
                        .map(param -> getType(param.getType()))
                        .collect(Collectors.joining());
                for (var param : callInst.getArguments()) {
                    params.append(generateLoadInstruction(param));
                }
                //                        .append(getFuncPath(caller.getName()))
                code
                        .append(generateLoadInstruction(caller))
                        .append(params)
                        .append("invokevirtual ").append(callerClass).append("/")
                        .append(formatMethodName(method.getLiteral()))
                        .append("(").append(paramTypes).append(")")
                        .append(getType(callInst.getReturnType()))
                        .append(NL);
            }
            case invokestatic -> {
                var caller = (Operand) callInst.getCaller();
                var method = (LiteralElement) callInst.getMethodName();
                var paramTypes = callInst.getArguments().stream()
                        .map(param -> getType(param.getType()))
                        .collect(Collectors.joining());
                for (var param : callInst.getArguments()) {
                    code.append(generateLoadInstruction(param));
                }

                code
                        .append("invokestatic ")
                        .append(getFuncPath(caller.getName()))
                        .append(formatMethodName(method.getLiteral()))
                        .append("(").append(paramTypes).append(")")
                        .append(getType(callInst.getReturnType()))
                        .append(NL);

            }
            case invokespecial -> {
                var caller = (Operand) callInst.getCaller();
                var className = ((ClassType) caller.getType()).getName();
                code
                        .append(generateLoadInstruction(caller))
                        .append("invokespecial ")
                        .append(className)
                        .append("/<init>()")
                        .append(getType(callInst.getReturnType()))
                        .append(NL)
                        .append("pop")
                        .append(NL);
            }
            case NEW -> {
                var caller = (Operand) callInst.getCaller();
                code
                        .append("new ")
                        .append(caller.getName())
                        .append(NL)
                        .append("dup")
                        .append(NL);
            }
            default -> throw new NotImplementedException(callInst.getClass());
        }

        return code.toString();
    }

    private String generateFieldInstruction(FieldInstruction fieldInstruction) {
        StringBuilder code = new StringBuilder();

        switch (fieldInstruction.getInstType()) {
            case PUTFIELD -> {
                Element op1 = fieldInstruction.getOperands().get(0);
                Element op2 = fieldInstruction.getOperands().get(1);
                Element op3 = fieldInstruction.getOperands().get(2);
                code
                    .append(generateLoadInstruction(op1))
                    .append(generateLoadInstruction(op3))
                    .append("putfield ")
                    .append(getFieldSignature(fieldInstruction))
                    .append(NL);
            }
            case GETFIELD -> {
                Element op1 = fieldInstruction.getOperands().get(0);
                Element op2 = fieldInstruction.getOperands().get(1);
                code
                    .append(generateLoadInstruction(op1))
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
        if (className.equals("this"))
            return ollirResult.getOllirClass().getClassName() + "/";
        for (String importPath : ollirResult.getOllirClass().getImports())
            if (importPath.endsWith(className))
                return importPath.replace('.', '/') + "/";
        return className;
    }

    private String generateStoreInstruction(Operand operand) {
        String name = operand.getName();
        String reg = getRegIndex(name);
        return switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "istore" + reg;
            case OBJECTREF, STRING, ARRAYREF, CLASS -> "astore" + reg;
            default -> throw new NotImplementedException(operand);
        };
    }

    private String generateLoadInstruction(Element element) {
        String instruction = "";
        if (element instanceof LiteralElement literal) {
            switch (element.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {
                    int val = Integer.parseInt(literal.getLiteral());
                    if (val == -1) instruction = "iconst_m1";
                    else if (val >= 0 && val <= 5) instruction = "iconst_" + val;
                    else if (val >= -128 && val <= 127) instruction = "bipush " + val;
                    else if (val >= -32768 && val <= 32767) instruction = "sipush " + val;
                    else instruction = "ldc " + val;
                }
                default -> throw new NotImplementedException(element);
            }
        } else if (element instanceof Operand) {
            instruction = switch (element.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> "iload" + getRegIndex(((Operand) element).getName());
                case OBJECTREF, STRING, ARRAYREF, CLASS -> "aload" + getRegIndex(((Operand) element).getName());
                case THIS -> "aload_0";
                default -> throw new NotImplementedException(element);
            };
        }

        return instruction + NL;
    }

    private String getFieldSignature(Field field) {
        return ".field " +
                switch (field.getFieldAccessModifier()) {
                    case PUBLIC -> "public ";
                    case PRIVATE -> "private ";
                    case PROTECTED -> "protected ";
                    case DEFAULT -> "";
                } +
                (field.isFinalField() ? "final " : "") +
                (field.isStaticField() ? "static " : "") +
                field.getFieldName() +
                " " +
                getType(field.getFieldType()) +
                NL;
    }

    private String getRegIndex(String name) {
        if (name.equals("THIS")) return "_0";
        var reg = currentMethod.getVarTable().get(name).getVirtualReg();
        return (reg > 3 ?  " " : "_") + reg;
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

    private String formatMethodName(String methodName) {
        return methodName.substring(1, methodName.length() - 1);
    }

}
