package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import static pt.up.fe.comp2024.ast.Kind.TYPE;


public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static Type getParamType(JmmNode paramExpr) {
        JmmNode type = paramExpr.getChildren(TYPE).get(0);
        if(type.getKind().equals("ArrayType")){
            JmmNode primite_type = type.getJmmChild(0);
            return new Type(primite_type.get("id"), true);
        }
        else if(type.getKind().equals("VarargType")){
            //TODO:
            return new Type("int...", true);
        }else{
            return new Type(type.get("id"), false);
        }
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL,BINARY_EXPR -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN,BOOLEAN_EXPR,COMPARISON_EXPR,UNARY_OP -> new Type("boolean",false);
            case ARRAY_TYPE, NEW_ARRAY -> new Type(expr.getChild(0).get("id"), true);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*" -> new Type(INT_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        return new Type(INT_TYPE_NAME, false);
    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
