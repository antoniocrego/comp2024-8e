package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.comp2024.symboltable.JmmSymbolTable;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NEW_LINE = "\n";

    private final JmmSymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(PAREN_EXPR, this::visitParentExp);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN, this::visitBoolean);
        addVisit(COMPARISON_EXPR, this::visitBinExpr);
        addVisit(BOOLEAN_EXPR, this::visitBoolExpr);
        addVisit(UNARY_OP, this::visitUnary);
        addVisit(FUNC_CALL, this::visitFuncCall);
        addVisit(NEW_CLASS, this::visitNewClass);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(ARRAY_INIT, this::visitInitArray);
        addVisit(LENGTH_EXPR, this::visitArrayLengthExpr);
        addVisit(ARRAY_ACCESS, this::visitArrayAccessExpr);

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

    private OllirExprResult visitNewArray(JmmNode node, Void unused){

            StringBuilder code = new StringBuilder();
            StringBuilder computation = new StringBuilder();

            String arrayType = OptUtils.toOllirType(new Type(node.getJmmChild(0).get("id"), true));
            String size = visit(node.getJmmChild(1)).getCode();


            String arrayTemp = OptUtils.getTemp();
            code.append(arrayTemp);
            code.append(arrayType);

            computation.append(code);
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(arrayType);
            computation.append(SPACE);
            computation.append("new(array, ");
            computation.append(size);
            computation.append(")");
            computation.append(arrayType);
            computation.append(END_STMT);


        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitInitArray(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode funcArgsNode = node.getJmmChild(0);

        String arrayType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), true));
        String arrayValuesType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));
        if(!funcArgsNode.getChildren().isEmpty()){
            arrayValuesType = OptUtils.toOllirType(funcArgsNode.getJmmChild(0));
        }
        int size = funcArgsNode.getChildren().size();

        // create temp
        String arrayTemp = OptUtils.getTemp();
        code.append(arrayTemp);
        code.append(arrayType);

        computation.append(code);
        computation.append(SPACE);
        computation.append(ASSIGN);
        computation.append(arrayType);
        computation.append(SPACE);
        computation.append("new(array, ");
        computation.append(size);
        computation.append(arrayValuesType);
        computation.append(")");
        computation.append(arrayType);
        computation.append(END_STMT);

        for(int i = 0; i < size; i++){
            OllirExprResult arg = visit(funcArgsNode.getJmmChild(i));
            computation.append(arg.getComputation());
            computation.append(arrayTemp);
            computation.append("[");
            computation.append(i);
            computation.append(".i32");
            computation.append("]");
            computation.append(arrayValuesType);
            computation.append(SPACE);
            computation.append(ASSIGN);
            computation.append(arrayValuesType);
            computation.append(SPACE);
            computation.append(arg.getCode());
            computation.append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArrayLengthExpr(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String arrayType = visit(node.getJmmChild(0)).getCode();
        String intType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));

        code.append(OptUtils.getTemp());
        code.append(intType);

        computation.append(code);
        computation.append(SPACE);
        computation.append(ASSIGN);
        computation.append(intType);
        computation.append(SPACE);
        computation.append("arraylength(").append(arrayType).append(")");
        computation.append(intType);
        computation.append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArrayAccessExpr(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        JmmNode arrayVarNode = node.getJmmChild(0);
        String type = TypeUtils.getExprType(arrayVarNode, table).getName();
        String arrayVarType = OptUtils.toOllirType(new Type(type, false));
        JmmNode indexNode = node.getJmmChild(1);
        OllirExprResult index = visit(indexNode);

        computation.append(index.getComputation());
        OllirExprResult arrayVarVisit = visit(arrayVarNode);

        computation.append(arrayVarVisit.getComputation());
        code.append(arrayVarVisit.getCode());
        code.append("[");
        code.append(index.getCode());
        code.append("]");
        code.append(arrayVarType);

        //Don't create temps for assignment statements
        if(node.getAncestor(ASSIGN_STMT).isPresent()){
            return new OllirExprResult(code.toString(), computation);
        }

        String temp = OptUtils.getTemp();
        String tempType = OptUtils.toOllirType(new Type(type, false));
        computation.append(temp);
        computation.append(tempType);
        computation.append(SPACE);
        computation.append(ASSIGN);
        computation.append(tempType);
        computation.append(SPACE);
        computation.append(code);
        computation.append(END_STMT);

        return new OllirExprResult(temp + tempType, computation);
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

    private OllirExprResult visitBoolExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String temp = OptUtils.getTemp() + resOllirType;

        String n = OptUtils.getIfNumber();
        String shortCircuitLabel = "true_" + n;
        String endLabel = "end_" + n;

        computation.append(lhs.getComputation());
        computation.append("if(").append(lhs.getCode()).append(") goto ").append(shortCircuitLabel).append(END_STMT);
        computation.append(temp).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("0.bool").append(END_STMT);
        computation.append("goto ").append(endLabel).append(END_STMT);

        computation.append(shortCircuitLabel).append(":").append(NEW_LINE);

        computation.append(rhs.getComputation());
        computation.append(temp).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        computation.append(endLabel).append(":").append(NEW_LINE);

        return new OllirExprResult(temp, computation);
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
                computation.append("getfield(this, ");
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

        // PARSING PARAMETERS

        // VarArgs
        List<JmmNode> argNodes = new ArrayList<>();
        if(node.getChildren().size()>1){
            argNodes = node.getChild(1).getChildren();
        }

        List<Symbol> methodParameters = new ArrayList<>();

        if(table.methodHasParams(methodCalledName)) {
            methodParameters = table.getParameters(methodCalledName);
        }
        List<JmmNode> varArgsNodes = new ArrayList<>();
        boolean varArgs = hasVarArgs(methodParameters, argNodes);
        if(varArgs){
            for(int i = argNodes.size()-1; i > methodParameters.size()-2; i--){
                varArgsNodes.add(argNodes.get(i));
            }
        }

        if(node.getChildren().size() > 1){
            var toVisit = node.getChild(1).getChildren();
            if(!isStatic && varArgs){
                toVisit = argNodes.subList(0, methodParameters.size()-1);
            }
            for(JmmNode argNode : toVisit){
                var visitedArgNode = visit(argNode);
                params.append(", ");
                code.append(visitedArgNode.getComputation());
                params.append(visitedArgNode.getCode());
                // When accessing arrays, create a temporary variable
                /*
                if(argNode.getKind().equals(ARRAY_ACCESS.getNodeName())) {
                    String temp = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(argNode,table));
                    computation.append(visitedArgNode.getComputation());
                    computation.append(temp);
                    computation.append(SPACE);
                    computation.append(ASSIGN);
                    computation.append(OptUtils.toOllirType(TypeUtils.getExprType(argNode,table)));
                    computation.append(SPACE);
                    computation.append(visitedArgNode.getCode());
                    computation.append(END_STMT);
                    params.append(temp);
                }else {
                    code.append(visitedArgNode.getComputation());
                    params.append(visitedArgNode.getCode());
                }
                */

            }
            if(varArgs){
                String arrayType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), true));
                String arrayValuesType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));
                int size = varArgsNodes.size();

                String arrayTemp = OptUtils.getTemp();
                String temp = arrayTemp + arrayType;

                params.append(", ").append(temp);

                code.append(temp);
                code.append(SPACE);
                code.append(ASSIGN);
                code.append(arrayType);
                code.append(SPACE);
                code.append("new(array, ");
                code.append(size);
                code.append(arrayValuesType);
                code.append(")");
                code.append(arrayType);
                code.append(END_STMT);

                for(int i = 0; i < size; i++){
                    OllirExprResult arg = visit(varArgsNodes.get(i));
                    code.append(arg.getComputation());
                    code.append(arrayTemp);
                    code.append("[");
                    code.append(i);
                    code.append(".i32");
                    code.append("]");
                    code.append(arrayValuesType);
                    code.append(SPACE);
                    code.append(ASSIGN);
                    code.append(arrayValuesType);
                    code.append(SPACE);
                    code.append(arg.getCode());
                    code.append(END_STMT);
                }
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

    private boolean hasVarArgs(List<Symbol> methodParameters, List<JmmNode> argNodes){
        if(methodParameters.isEmpty()) return false;
        if(!methodParameters.get(methodParameters.size()-1).getType().getName().equals("int...")) return false;
        return argNodes.size() >= methodParameters.size();
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
