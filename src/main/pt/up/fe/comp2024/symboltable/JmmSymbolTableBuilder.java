package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;
import static pt.up.fe.comp2024.ast.Kind.PARAM;
import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDeclaractions = root.getChildren(Kind.CLASS_DECL);
        var classDecl = classDeclaractions.get(0);

        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superClassName = classDecl.hasAttribute("superName")
                ? classDecl.get("superName")
                : null;

        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);

        return new JmmSymbolTable(imports, className, superClassName, methods, returnTypes, params, locals, fields);
    }

    private static List<String> buildImports(JmmNode root){
        var importDeclarations = root.getChildren(Kind.IMPORT_DECL);
        return importDeclarations.stream().map(node -> node.get("name")).toList();

    }
   private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(METHOD_DECL)){
            String method_name = method.get("name");
            map.put(method_name, TypeUtils.getParamType(method));
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL).forEach(method -> map.put(method.get("name"), method.getChildren(PARAM).stream().map(param-> new Symbol(TypeUtils.getParamType(param), param.get("name"))).toList()));

        return map;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> map = new ArrayList<>();

        classDecl.getChildren(VAR_DECL).forEach(var -> map.add(new Symbol(TypeUtils.getParamType(var),var.get("name"))));

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(TypeUtils.getParamType(varDecl), varDecl.get("name")))
                .toList();
    }

}
