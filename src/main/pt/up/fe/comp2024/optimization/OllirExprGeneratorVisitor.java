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
        addVisit(BOOLEAN, this::visitBoolean);
        addVisit(COMPARISON_EXPR, this::visitBinExpr);
        addVisit(BOOLEAN_EXPR, this::visitBinExpr);
        addVisit(UNARY_OP, this::visitUnary);
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

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var intType = new Type("boolean", false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String boolNum = node.get("value").equals("true") ? "1" : "0";
        String code = boolNum + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitUnary(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());

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

        Type type = TypeUtils.getExprType(node, table);

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE).append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(lhsCode).append(END_STMT);

        return new OllirExprResult(code, computation);
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
        String objectType = getObjectType(headNode);
        String objectName = "";
        String exprCode = "";
        if (objectType.equals(FUNC_CALL.toString())){
            var visitedFuncCall = visit(headNode);
            code.append(visitedFuncCall.getComputation());
            exprCode = visitedFuncCall.getCode();
        }else{
            objectName = getObjectName(headNode);
        }



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
                String type = OptUtils.toOllirType(new Type(callerType,false));
                objectName = objectName + type;
            }
            else{
                for (Symbol local : table.getLocalVariables(methodName)){
                    if (local.getName().equals(objectName)){
                        callerType = local.getType().getName();
                        String type = OptUtils.toOllirType(new Type(callerType,false));
                        objectName = objectName + type;
                        found = true;
                        break;
                    }
                }

                if (!found){
                    for (Symbol parameter : table.getParameters(methodName)){
                        if (parameter.getName().equals(objectName)){
                            callerType = parameter.getType().getName();
                            String type = OptUtils.toOllirType(new Type(callerType,false));
                            objectName = objectName + type;
                            found = true;
                            break;
                        }
                    }
                }

                if (!found){
                    for (Symbol field: table.getFields()){
                        if (field.getName().equals(objectName)){
                            callerType = field.getType().getName();
                            String tempUsed = OptUtils.getTemp();
                            String type = OptUtils.toOllirType(field.getType());
                            code.append(tempUsed);
                            code.append(type);
                            code.append(SPACE);
                            code.append(ASSIGN);
                            code.append(type);
                            code.append(" getfield(this, ");
                            code.append(objectName);
                            code.append(type);
                            code.append(")");
                            code.append(type);
                            code.append(END_STMT);

                            objectName = tempUsed + type;

                            break;
                        }
                    }
                }
            }

        }

        if(objectType.equals(NEW_CLASS.getNodeName())){
            var visitedHead = visit(headNode);
            code.append(visitedHead.getComputation());
            objectName = visitedHead.getCode();
        }

        if(isStatic){
            invoke = "invokestatic";
        }

        StringBuilder params = new StringBuilder();

        // Parsing parameters
        if(node.getChildren().size() > 1){
            for(JmmNode argNode : node.getChild(1).getChildren()){
                var visitedArgNode = visit(argNode);
                params.append(", ");
                code.append(visitedArgNode.getComputation());
                params.append(visitedArgNode.getCode());
            }
        }

        var returnType = "";

        if(callerType.equals(table.getClassName())){
            returnType = OptUtils.toOllirType(new Type(table.getReturnType(methodCalledName).getName(),false));
        }
        else{
            var parent = node.getParent();
            if (parent.getKind().equals(ASSIGN_STMT.toString())){
                returnType = OptUtils.toOllirType(TypeUtils.getExprType(parent.getChild(0),table));
            }
            else if (parent.getKind().equals(FUNC_ARGS.toString())){
                int k = -1;
                for (int i = 0; i<parent.getNumChildren(); i++){
                    if (parent.getChild(i).equals(node)){
                        k = i;
                        break;
                    }
                }
                returnType = OptUtils.toOllirType(table.getParameters(parent.getParent().get("id")).get(k).getType());
            }
            else if (parent.getKind().equals(RETURN_STMT.toString())){
                returnType = OptUtils.toOllirType(table.getReturnType(methodName));
            }
            else{
                returnType = ".V";
            }
        }

        var input = "";
        if (exprCode.isEmpty()) input = objectName;
        else input = exprCode;

        var funcOllir = invoke + "(" + input + ", \"" + methodCalledName + "\"" + params + ")"+returnType+END_STMT;

        if(!node.getParent().isInstance(DEFAULT_STMT)){
            StringBuilder temp = new StringBuilder (OptUtils.getTemp()+returnType);
            computation = code;
            computation.append(temp+SPACE+ASSIGN+returnType+SPACE+funcOllir);
            code = temp;
        }else{
            code.append(funcOllir);
        }

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
