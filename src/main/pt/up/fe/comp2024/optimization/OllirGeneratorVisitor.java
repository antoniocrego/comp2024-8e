package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL, this::visitClassField);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(DEFAULT_STMT, this::visitDefaultStmt);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitDefaultStmt(JmmNode node, Void unused){
        var stmt = node.getChild(0);

        // method only defined for FUNC_CALL for now
        if(!stmt.getKind().equals(FUNC_CALL.getNodeName())) return "";

        var funcCall = exprVisitor.visit(stmt);

        return funcCall.getComputation() + funcCall.getCode();
    }
    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        return id + typeCode;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        if (isPublic) {
            code.append("public ");
        }

        // Access Type
        var accessType = node.getOptional("accessType");
        if(accessType.isPresent()){
            code.append(accessType.get());
            code.append(SPACE);
        }

        // method na,me
        var name = node.get("name");
        code.append(name);

        // params
        ArrayList<String> paramsCode = new ArrayList<>();
        for(JmmNode paramNode : node.getChildren(PARAM)){
            paramsCode.add(visit(paramNode));
        }
        var paramCode = String.join(", ", paramsCode);
        code.append("(").append(paramCode).append(")");

        // return type
        var retType = OptUtils.toOllirType(table.getReturnType(name));
        code.append(retType);
        code.append(L_BRACKET);

        // rest of the stmts
        boolean returnCalled = false;
        int numParams = table.getParameters(name).size();
        for (int i = 1+numParams; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
            if(child.getKind().equals(RETURN_STMT.getNodeName())){
                returnCalled = true;
            }
        }

        if(!returnCalled){
            code.append("ret.V");
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitClassField(JmmNode node, Void unused) {

        String parent = node.getParent().getKind();
        if(!parent.equals(CLASS_DECL.getNodeName())) return "";

        StringBuilder code = new StringBuilder(".field ");

        boolean isPrivate = NodeUtils.getBooleanAttribute(node, "isPrivate", "false");

        String visibility = isPrivate ? "private " : "public ";
        code.append(visibility);

        // name
        var name = node.get("name");
        code.append(name);

        // type
        var type = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(type);

        code.append(END_STMT);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        var superName = node.getOptional("superName");
        if(superName.isPresent()){
            code.append(" extends ");
            code.append(superName.get());
        }else{
            code.append(" extends Object ");
        }

        code.append(L_BRACKET);
        code.append(NL);

        var needNl = true;
        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {
        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }

    private String visitImportDecl(JmmNode node, Void unused){
        return "import " + node.get("name") + ";" + NL;
    }

    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        var toSeeTheCodeOnDebug = code.toString();
        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
