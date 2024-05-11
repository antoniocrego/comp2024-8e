package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.*;
import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;
    private static Integer ifNumber = 0;
    private static Integer whileNumber = 0;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getIfNumber() {
        return String.valueOf(++ifNumber);
    }
    public static String getWhileNumber() {
        return String.valueOf(++whileNumber);
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        String typeKind = typeNode.getKind();
        if(typeKind.equals(ARRAY_TYPE.getNodeName()) || typeKind.equals(VARARG_TYPE.getNodeName())) {
            return ".array" + toOllirType(typeNode.getChild(0));
        }
        if(typeKind.equals(CUSTOM_TYPE.getNodeName())) return "." + typeNode.get("id");
        if(typeKind.equals(INTEGER_LITERAL.getNodeName())) return ".i32";

        String typeName = typeNode.get("id");

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        if(type.isArray()) //TODO (thePeras): Maybe this is not wanted
            return ".array" + toOllirType(type.getName());
        return toOllirType(type.getName());
    }

    private static String toOllirType(String typeName) {

        //TODO (thePeras): Should exist casting here? ex: double -> int
        return "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "void" -> "V";
            default -> typeName; // For class names
            //default -> throw new NotImplementedException(typeName);
        };
    }


}
