package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(PAREN_EXPR, this::visitParentExp);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(FUNC_CALL, this::visitFuncCall);
        addVisit(NEW_CLASS, this::visitNewClass);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitNewClass(JmmNode node, Void unused){

        StringBuilder computation = new StringBuilder();

        String tempToUse = OptUtils.getTemp();
        String ollirIntType = OptUtils.toOllirType(node);
        String className = node.get("id");

        computation.append(tempToUse);
        computation.append(ollirIntType);
        computation.append(SPACE);
        computation.append(ASSIGN);
        computation.append(ollirIntType);
        computation.append(SPACE);
        computation.append("new(");
        computation.append(className);
        computation.append(")");
        computation.append(ollirIntType);
        computation.append(END_STMT);

        computation.append("invokespecial(");
        computation.append(tempToUse);
        computation.append(ollirIntType);

        computation.append(", \"\").V "); // Void because constructors are void
        computation.append(END_STMT);

        String code = tempToUse + ollirIntType;

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitParentExp(JmmNode node, Void unused){

        return visit(node.getJmmChild(0));
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        String lhsCode = lhs.getCode();
        if(lhs.getCode().contains("invokevirtual")){
            String type = OptUtils.getTemp() + resOllirType;
            computation.append(type);
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(resOllirType);
            computation.append(SPACE);
            computation.append(lhs.getCode());
            lhsCode = type;
        }

        String rhsCode = rhs.getCode();
        if(rhs.getCode().contains("invokevirtual")){
            String type = OptUtils.getTemp() + resOllirType;
            computation.append(type);
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(resOllirType);
            computation.append(SPACE);
            computation.append(rhs.getCode());
            rhsCode = type;
        }

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhsCode).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhsCode).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        if(type == null) return new OllirExprResult("");
        String ollirType = OptUtils.toOllirType(type);

        StringBuilder computation = new StringBuilder();

        var methodNode = node.getAncestor(METHOD_DECL);
        if(methodNode.isEmpty()) return new OllirExprResult("");

        var methodName = methodNode.get().get("name");

        for (Symbol symbol : table.getLocalVariables(methodName)){
            if (symbol.getName().equals(id)) return new OllirExprResult(id+ollirType);
        }

        for (Symbol symbol : table.getParameters(methodName)){
            if (symbol.getName().equals(id)) return new OllirExprResult(id+ollirType);
        }

        for(Symbol symbol : table.getFields()){
            if(symbol.getName().equals(id)){
                // It is a field!
                var tempUsed = OptUtils.getTemp();
                computation.append(tempUsed);
                computation.append(ollirType);
                computation.append(SPACE);
                computation.append(ASSIGN);
                computation.append(ollirType);
                computation.append(SPACE);
                computation.append("getfield(this, "); //field_1.i32).i32;
                computation.append(id);
                computation.append(ollirType);
                computation.append(")");
                computation.append(ollirType);
                computation.append(END_STMT);

                String code = tempUsed + ollirType;
                return new OllirExprResult(code, computation);
            }
        }


        String code = id + ollirType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitFuncCall(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        var headNode = node.getChild(0);
        String objectName = getObjectName(headNode);
        String objectType = getObjectType(headNode);

        var methodCalledName = node.get("id");

        String invoke = "invokevirtual";

        var methodNode = node.getAncestor(METHOD_DECL);
        if(methodNode.isEmpty()) return new OllirExprResult("");

        var methodName = methodNode.get().get("name");
        boolean isStatic = false;

        // Check in imports
        for(String importt : table.getImports()){
            if(importt.equals(objectName)){
                isStatic = true;
                break;
            }
        }

        String callerType = "";

        // Check in class methods
        if (!isStatic){
            boolean found = false;


            if (objectName.equals("this")){
                callerType = table.getClassName();
            }
            else{
                for (Symbol local : table.getLocalVariables(methodName)){
                    if (local.getName().equals(objectName)){
                        callerType = local.getType().getName();
                        found = true;
                        break;
                    }
                }

                if (!found){
                    for (Symbol parameter : table.getParameters(methodName)){
                        if (parameter.getName().equals(objectName)){
                            callerType = parameter.getType().getName();
                            found = true;
                            break;
                        }
                    }
                }

                if (!found){
                    for (Symbol field: table.getFields()){
                        if (field.getName().equals(objectName)){
                            callerType = field.getType().getName();
                            break;
                        }
                    }
                }
            }
            objectName = objectName + "." + callerType;
        }

        // Check in class fields
        for(Symbol field : table.getFields()){
            if(field.getName().equals(objectName)){
                String tempUsed = OptUtils.getTemp();
                String type = OptUtils.toOllirType(field.getType());
                computation.append(tempUsed);
                computation.append(type);
                computation.append(SPACE);
                computation.append(ASSIGN);
                computation.append(type);
                computation.append(" getfield(this, ");
                computation.append(objectName);
                computation.append(type);
                computation.append(")");
                computation.append(type);
                computation.append(END_STMT);

                objectName = tempUsed + type;

                isStatic = false;
                break;
            }
        }

        if(objectType.equals(NEW_CLASS.getNodeName())){
            var visitedHead = visit(headNode);
            computation.append(visitedHead.getComputation());
            objectName = visitedHead.getCode();
        }

        if(isStatic){
            invoke = "invokestatic";
        }

        code.append(invoke);
        code.append("(");

        code.append(objectName);
        code.append(", ");

        code.append('"');
        code.append(methodCalledName);
        code.append('"');

        // Parsing parameters
        if(node.getChildren().size() > 1){
            for(JmmNode argNode : node.getChild(1).getChildren()){
                var visitedArgNode = visit(argNode);
                code.append(", ");
                if(argNode.getKind().equals(FUNC_CALL.getNodeName())) {
                    computation.append(visitedArgNode.getComputation());
                    String tempUsed = OptUtils.getTemp();
                    Type thisType = TypeUtils.getExprType(argNode.getChild(1).getChild(0), table);
                    String type = OptUtils.toOllirType(thisType);

                    String tempName = tempUsed + type;
                    computation.append(tempName);
                    computation.append(SPACE);
                    computation.append(ASSIGN);
                    computation.append(type);
                    computation.append(SPACE);
                    computation.append(visitedArgNode.getCode());
                    code.append(tempName);
                }else{
                    computation.append(visitedArgNode.getComputation());
                    code.append(visitedArgNode.getCode());
                }
            }
        }

        code.append(")");

        var assignStm = node.getAncestor(ASSIGN_STMT);
        var returnStm = node.getAncestor(RETURN_STMT);
        JmmNode parent = null;
        if(assignStm.isPresent()) parent = assignStm.get();
        if(returnStm.isPresent()) parent = returnStm.get();

        if(parent != null){
            String typeString = "";
            if(assignStm.isPresent()) {
                Type thisType = TypeUtils.getExprType(assignStm.get().getJmmChild(0), table);
                typeString = OptUtils.toOllirType(thisType);
            }
            if(returnStm.isPresent()){
                Type retType = table.getReturnType(methodName);
                typeString = OptUtils.toOllirType(retType);
            }

            code.append(typeString);

            String tempToUse = OptUtils.getTemp();
            computation.append(tempToUse);
            computation.append(typeString);
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(typeString);
            computation.append(SPACE);
            computation.append(code);
            computation.append(END_STMT);

            var newCode = tempToUse + typeString;
            return new OllirExprResult(newCode, computation);
        }

        var funcStm = node.getAncestor(FUNC_CALL);
        if(funcStm.isPresent()){
            Type thisType = TypeUtils.getExprType(node.getJmmChild(1).getJmmChild(0), table);
            String typeString = OptUtils.toOllirType(thisType);

            code.append(typeString);
            code.append(END_STMT);

            return new OllirExprResult(code.toString(), computation);
        }

        if (callerType.equals(table.getClassName())){
            code.append(OptUtils.toOllirType(table.getReturnType(methodCalledName)));
            code.append(END_STMT);
            return new OllirExprResult(code.toString(), computation);
        }

        code.append(".V");
        code.append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    String getObjectName(JmmNode node){
        if(node.getKind().equals(PAREN_EXPR.getNodeName())){
            return getObjectName(node.getChild(0));
        }

        if(node.getKind().equals(NEW_CLASS.getNodeName())) return node.get("id");

        return node.get("name");
    }

    String getObjectType(JmmNode node){
        if(node.getKind().equals(PAREN_EXPR.getNodeName())){
            return getObjectType(node.getChild(0));
        }

        return node.getKind();
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
