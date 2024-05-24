package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import javax.swing.text.AbstractDocument;
import java.util.ArrayList;
import java.util.Arrays;
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

    int stack_limit;
    int stack_size;

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

        generators.put(CondBranchInstruction.class, this::generateBranchInstruction);
        generators.put(GotoInstruction.class, this::generateGotoInstruction);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);

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
                .append(".class ").append(getClassAccessModifier(classUnit)).append(className).append(NL)
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
        var methodBody = new StringBuilder();

        stack_limit = 0;
        stack_size = 0;

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

        for (var inst : method.getInstructions()) {
            var labels = method.getLabels(inst);

            for (var label : labels) {
                methodBody.append(label).append(":").append(NL);
            }

            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            methodBody.append(instCode);
            if (inst.getInstType() == InstructionType.CALL
                    &&
                    ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) {
                methodBody
                        .append("pop").append(NL);
                updateStack(-1);
            }
        }


        // Add limits
//        method.getVarTable().size() + 1
        code.append(TAB).append(".limit stack ").append(stack_limit).append(NL);
        code.append(TAB).append(".limit locals ").append(method.getVarTable().size() + 1).append(NL);
        code.append(methodBody);
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

// TODO:          ----         ----         ----         ----         ----         ----         ----         ----
//       ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2
//  -----          ----         ----         ----         ----         ----         ----         ----         ----

    private String generateBranchInstruction(CondBranchInstruction instruction) {
        StringBuilder code = new StringBuilder();

        Instruction condition = instruction.getCondition();
        switch (condition.getInstType()) {
            case UNARYOPER -> {
                throw new NotImplementedException(instruction.getCondition().getInstType());
            }
            case BINARYOPER -> {
                BinaryOpInstruction bop = (BinaryOpInstruction) condition;
                code
                        .append(generateLoadInstruction(bop.getLeftOperand()))
                        .append(generateLoadInstruction(bop.getRightOperand()))
                        .append("isub").append(NL)
                        .append(switch (bop.getOperation().getOpType()) {
                            case LTH -> "iflt ";
                            case GTH -> "ifgt ";
                            case EQ -> "ifeq ";
                            case NEQ -> "ifne ";
                            case LTE -> "ifle ";
                            case GTE -> "ifge ";
                            default -> throw new NotImplementedException(instruction.getInstType());
                        });
                updateStack(-1);
            }
            case NOPER -> {
                SingleOpInstruction op = (SingleOpInstruction) condition;
                code
                        .append(generateLoadInstruction(op.getSingleOperand()))
                        .append("ifne "); // Append here!
            }
            default -> throw new NotImplementedException(instruction.getInstType());
        }

        code.append(instruction.getLabel());

        return code.toString();
    }

    private String generateGotoInstruction(GotoInstruction instruction) {
        return "goto " + instruction.getLabel();
    }

    private String generateUnaryOp(UnaryOpInstruction instruction) {
        var code = new StringBuilder();
        switch (instruction.getOperation().getOpType()) {
            case NOTB -> {
                code
                        .append("iconst_1").append(NL)
                        .append(generateLoadInstruction(instruction.getOperand()))
                        .append("ixor").append(NL);
                updateStack(new ArrayList<>(Arrays.asList(1, -1)));
//                stack_limit = Math.max(stack_size + 1, stack_limit); // ???? como adiciona e da logo pop
            }
            default -> throw new NotImplementedException(instruction.getOperation().getOpType());
        };

        return code.toString();
    }

    private void updateStack(int stackChange) {
        stack_size += stackChange;
        stack_limit = Math.max(stack_size, stack_limit);
    }

    private void updateStack(List<Integer> stackChanges) {
        for (int j : stackChanges) {
            stack_size += j;
            stack_limit = Math.max(stack_size, stack_limit);
        }
    }

// TODO:          ----         ----         ----         ----         ----         ----         ----         ----
//       ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2    ENTREGA 2
//  -----          ----         ----         ----         ----         ----         ----         ----         ----

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

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        String code =
                generators.apply(binaryOp.getLeftOperand()) +
                generators.apply(binaryOp.getRightOperand()) +
                switch (binaryOp.getOperation().getOpType()) {
                    case ADD -> "iadd";
                    case MUL -> "imul";
                    case SUB -> "isub";
                    case DIV -> "idiv";
                    default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
                } +
                NL;

        updateStack(-1);
        return code;
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
                code
                        .append(generateLoadInstruction(caller))
                        .append(params)
                        .append("invokevirtual ").append(getFullPath(callerClass)).append("/")
                        .append(formatMethodName(method.getLiteral()))
                        .append("(").append(paramTypes).append(")")
                        .append(getType(callInst.getReturnType()))
                        .append(NL);
                updateStack(1);
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
                        .append(getFullPath(caller.getName()))
                        .append("/")
                        .append(formatMethodName(method.getLiteral()))
                        .append("(").append(paramTypes).append(")")
                        .append(getType(callInst.getReturnType()))
                        .append(NL);
                updateStack(1);
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
                updateStack(1);
            }
            case NEW -> {
                var caller = (Operand) callInst.getCaller();

                if (caller.getName().equals("array")) {
                    code
                            .append(generateLoadInstruction(callInst.getOperands().get(1)))
                            .append("newarray int").append(NL);
                } else {
                    code
                            .append("new ").append(caller.getName()).append(NL)
                            .append("dup").append(NL);
                }

                updateStack(2);
            }
            case arraylength -> {
                code
                        .append(generateLoadInstruction(callInst.getOperands().get(0)))
                        .append("arraylength ").append(NL);
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

    private String getFullPath(String className) {
        if (className.equals("this"))
            return ollirResult.getOllirClass().getClassName() + "/";
        for (String importPath : ollirResult.getOllirClass().getImports())
            if (importPath.endsWith(className))
                return importPath.replace('.', '/');
        return className;
    }

    private String generateStoreInstruction(Operand operand) {
        String name = operand.getName();
        String reg = getRegIndex(name);
        updateStack(-1);
        return switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "istore" + reg;
            case OBJECTREF, STRING, ARRAYREF, CLASS -> "astore" + reg;
            default -> throw new NotImplementedException(operand);
        };
    }

    private String generateLoadInstruction(Element element) {
        StringBuilder code = new StringBuilder();
        if (element instanceof LiteralElement literal) {
            switch (element.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> {
                    int val = Integer.parseInt(literal.getLiteral());
                    if (val == -1) code.append("iconst_m1");
                    else if (val >= 0 && val <= 5) code.append("iconst_" + val);
                    else if (val >= -128 && val <= 127) code.append("bipush " + val);
                    else if (val >= -32768 && val <= 32767) code.append("sipush " + val);
                    else code.append("ldc " + val);
                }
                default -> throw new NotImplementedException(element);
            }
        } else if (element instanceof ArrayOperand) {
            code
                    .append("aload").append(getRegIndex(((Operand) element).getName())).append(NL)
                    .append(generateLoadInstruction(((ArrayOperand) element).getIndexOperands().get(0)))
                    .append("iaload").append(NL);
            updateStack(new ArrayList<>(Arrays.asList(1, -1)));
        } else if (element instanceof Operand) {
            code.append(switch (element.getType().getTypeOfElement()) {
                case INT32, BOOLEAN -> "iload" + getRegIndex(((Operand) element).getName());
                case OBJECTREF, STRING, ARRAYREF, CLASS -> "aload" + getRegIndex(((Operand) element).getName());
                case THIS -> "aload_0";
                default -> throw new NotImplementedException(element);
            });
        }

        updateStack(1);
        return code + NL;
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

    private String getClassAccessModifier(ClassUnit classUnit) {
        StringBuilder code = new StringBuilder();
        switch (classUnit.getClassAccessModifier()) {
            case PUBLIC -> code.append("public ");
            case PRIVATE -> code.append("private ");
            case PROTECTED -> code.append("protected ");
            case DEFAULT -> {}
        };
        code.append(classUnit.isFinalClass() ? "final " : "");
        code.append(classUnit.isStaticClass() ? "abstract " : "");
        return code.toString();
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
                yield "L" + getFullPath(classType.getName()) + ";";
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
