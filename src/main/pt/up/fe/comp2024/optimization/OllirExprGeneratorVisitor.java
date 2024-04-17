package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

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

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        if(type == null) return new OllirExprResult("");

        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitFuncCall(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        var objectName = node.getChild(0).get("name");
        var methodCalledName = node.get("id");

        String invoke = "invokevirtual";

        var methodNode = node.getAncestor(METHOD_DECL);
        if(methodNode.isEmpty()) return new OllirExprResult("");

        var methodName = methodNode.get().get("name");
        boolean isStatic = true;

        // Check in method locals
        for(Symbol local : table.getLocalVariables(methodName)){
            if(local.getName().equals(objectName)){

                isStatic = false;
                break;
            }
        }

        // Check in class methods
        for(String method : table.getMethods()){
            if(method.equals(methodCalledName)){

                //If the method is calling itself only add "this" else "this.ClassName"
                if(!method.equals(methodName)){
                    objectName = objectName + "." + table.getClassName();
                }

                isStatic = false;
                break;
            }
        }

        // Check in class fields
        for(Symbol field : table.getFields()){
            if(field.getName().equals(objectName)){

                isStatic = false;
                break;
            }
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
                code.append(", ");
                var id = argNode.get("name");
                Type type = TypeUtils.getExprType(argNode, table);
                String ollirType = OptUtils.toOllirType(type);
                code.append(id);
                code.append(ollirType);
            }
        }

        code.append(")");

        // Check if func call was done in an assign statement
        var assignStm = node.getAncestor(ASSIGN_STMT);
        if(assignStm.isPresent()){
            Type thisType = TypeUtils.getExprType(assignStm.get().getJmmChild(0), table);
            String typeString = OptUtils.toOllirType(thisType);
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

        code.append(".V");
        code.append(END_STMT);

        return new OllirExprResult(code.toString(), computation);
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
