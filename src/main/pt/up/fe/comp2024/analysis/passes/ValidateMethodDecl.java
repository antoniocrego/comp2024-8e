package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ValidateMethodDecl extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNC_CALL, this::visitParams);
        addVisit(Kind.RETURN_STMT, this::visitReturn);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        long occurrences = table.getMethods().stream()
                .filter(methodd -> methodd.equals(currentMethod))
                .count();

        if (occurrences>1){
            var message = String.format("Redeclaration of method '%s'", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            currentMethod = null;
            return null;
        }

        if (currentMethod.equals("main")){
            if (!method.getChild(0).getKind().equals(Kind.VOID_TYPE.toString())){
                var message = String.format("Main method declared with non-void return type");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
            var mainParams = table.getParameters("main");
            if (mainParams.size()!=1 || !mainParams.get(0).getType().isArray() || !mainParams.get(0).getType().getName().equals("String")){
                var message = "Invalid parameters for main method, expected single parameter of type String array";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
            return null;
        }

        if (method.getOptional("accessType").isPresent() && method.get("accessType").equals("static")){
            var message = String.format("Non-main method '%s' declared with static access type", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }

        Map<String, Long> parameterCounts = table.getParameters(currentMethod).stream()
                .map(Symbol::getName) // Extract parameter names
                .collect(Collectors.groupingBy(name -> name, Collectors.counting())); // Count occurrences

        boolean hasDuplicates = parameterCounts.values().stream().anyMatch(count -> count > 1);

        if (hasDuplicates) {
            var message = String.format("Method '%s' declared with repeated parameters", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        try{
            List<Symbol> methodParameters;
            methodParameters = table.getParameters(currentMethod);
            for (int i = 0; i<methodParameters.size()-1; i++){
                if (methodParameters.get(i).getType().getName().equals("int...")){
                    var message = String.format("Definition of function '%s' with vararg argument not at the end.", currentMethod);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(method),
                            NodeUtils.getColumn(method),
                            message,
                            null)
                    );
                    return null;
                }
            }
        }
        catch(Exception e){
            return null;
        }

        if (method.getChild(0).getKind().equals(Kind.VOID_TYPE.toString()) && !method.getChildren(Kind.RETURN_STMT.toString()).isEmpty()){
            var message = String.format("Method '%s' expected no return statement, however, got one", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }
        else if (!method.getChildren(Kind.RETURN_STMT.toString()).isEmpty()){
            if (method.getChildren(Kind.RETURN_STMT.toString()).size()>1){
                var message = String.format("Multiple return statements given for method '%s'", currentMethod);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
            if (!method.getChild(method.getNumChildren()-1).getKind().equals(Kind.RETURN_STMT.toString())){
                var message = String.format("Return statement for method '%s' not given as last statement", currentMethod);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
                return null;
            }
        }
        else{
            var message = String.format("No return statement given for method '%s'", currentMethod);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitParams(JmmNode funcCall, SymbolTable table) {
        if (currentMethod==null) return null;

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        var methodVariable = "";
        var methodName = funcCall.get("id");
        Type methodCallerType = new Type("", false);
        if (funcCall.getChild(0).getKind().equals(Kind.VAR_REF_EXPR.toString())){
            methodVariable = funcCall.getChild(0).get("name");
        }
        else{
            var callingMethod = funcCall.getChild(0);
            while(callingMethod.getKind().equals(Kind.FUNC_CALL.toString())){
                callingMethod = callingMethod.getChild(0);
            }
            if (!callingMethod.getKind().equals(Kind.VAR_REF_EXPR.toString())){
                return null;
            }
            if (table.getImports().contains(callingMethod.get("name"))){
                return null;
            }
            else{
                while(callingMethod.getParent().getKind().equals(Kind.FUNC_CALL.toString())){
                    var nextReturn = table.getReturnType(callingMethod.getParent().get("id"));
                    if (!nextReturn.equals(new Type(table.getClassName(), false))){
                        return null;
                    }
                    callingMethod = callingMethod.getParent();
                    methodCallerType = nextReturn;
                }
            }
        }
        var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
        megaTable.addAll(table.getParameters(currentMethod));
        megaTable.addAll(table.getFields());

        for (var element : megaTable){
            if (element.getName().equals(methodVariable)){
                methodCallerType = element.getType();
                break;
            }
        }

        if (!(methodCallerType.getName().equals(table.getClassName()) || methodVariable.equals("this"))) return null;

        List<Symbol> methodParameters;

        try{
            methodParameters = table.getParameters(methodName);
        }
        catch(Exception e){
            return null;
        }

        var givenParameters = funcCall.getChild(funcCall.getNumChildren()-1);
        if (!givenParameters.getKind().equals(Kind.FUNC_ARGS.toString()) && !methodParameters.isEmpty()){
            if (givenParameters.getNumChildren()==methodParameters.size()-1 && methodParameters.get(methodParameters.size()-1).getType().getName().equals("int...")) return null;
            var message = String.format("Call to function '%s' without parameters.", methodName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(funcCall),
                    NodeUtils.getColumn(funcCall),
                    message,
                    null)
            );
            return null;
        }
        else if (givenParameters.getKind().equals(Kind.FUNC_ARGS.toString())) {
            if (!methodParameters.isEmpty() && methodParameters.get(methodParameters.size()-1).getType().getName().equals("int...")) {
                if (!(methodParameters.size()==givenParameters.getNumChildren() && TypeUtils.getExprType(givenParameters.getChild(givenParameters.getNumChildren()-1),table).isArray() && TypeUtils.getExprType(givenParameters.getChild(givenParameters.getNumChildren()-1),table).getName().equals("int"))){
                    if (methodParameters.size()<=givenParameters.getNumChildren()) {
                        for (int i = methodParameters.size() - 1; i < givenParameters.getNumChildren(); i++) {
                            Type givenArgType = new Type("", false);
                            Type expectedArgType = new Type("int", false);
                            var currentExplore = givenParameters.getChild(i);
                            while (currentExplore.getKind().equals(Kind.PAREN_EXPR.toString())){
                                currentExplore = currentExplore.getChild(0);
                            }
                            if (currentExplore.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                                var varRefName = currentExplore.get("name");
                                for (var element : megaTable) {
                                    if (element.getName().equals(varRefName)) {
                                        givenArgType = element.getType();
                                        break;
                                    }
                                }
                            } else if (currentExplore.getKind().equals(Kind.FUNC_CALL.toString())) { // this is archaic, could be better regarding implements, doesnt consider calling funcs like "this.foo()" that dont exist, even without extends
                                var methodVariableInner = "";
                                var methodNameInner = currentExplore.get("id");
                                Type methodCallerTypeInner = new Type("", false);
                                if (!currentExplore.getChild(0).getKind().equals(Kind.FUNC_CALL.toString())){
                                    methodVariableInner = currentExplore.getChild(0).get("name");
                                }
                                else{
                                    var callingMethod = currentExplore.getChild(0);
                                    while(callingMethod.getKind().equals(Kind.FUNC_CALL.toString())){
                                        callingMethod = callingMethod.getChild(0);
                                    }
                                    if (!callingMethod.getKind().equals(Kind.VAR_REF_EXPR.toString())){
                                        return null;
                                    }
                                    if (table.getImports().contains(callingMethod.get("name"))){
                                        return null;
                                    }
                                    else{
                                        while(callingMethod.getParent().getKind().equals(Kind.FUNC_CALL.toString())){
                                            var nextReturn = table.getReturnType(callingMethod.getParent().get("id"));
                                            if (!nextReturn.equals(new Type(table.getClassName(), false))){
                                                continue;
                                            }
                                            callingMethod = callingMethod.getParent();
                                            methodCallerTypeInner = nextReturn;
                                        }
                                    }
                                }
                                if (methodVariableInner.equals("this")) methodCallerTypeInner = new Type(table.getClassName(), false);
                                else if (!methodVariableInner.isEmpty() && !table.getImports().contains(methodVariableInner)) {
                                    for (var element : megaTable) {
                                        if (element.getName().equals(methodVariableInner)) {
                                            methodCallerTypeInner = element.getType();
                                            break;
                                        }
                                    }
                                }
                                if (!methodCallerTypeInner.getName().equals(table.getClassName()))
                                    givenArgType = expectedArgType;
                                else givenArgType = table.getReturnType(methodNameInner);
                            } else givenArgType = TypeUtils.getExprType(currentExplore, table);
                            if (!givenArgType.equals(expectedArgType)) {
                                var message = "Call to function '%s' with invalid parameter type '%s'";
                                if (givenArgType.isArray()) message += " array";
                                message += ", expected '%s'";
                                if (expectedArgType.isArray()) message += " array";
                                message = String.format(message, methodName, givenArgType.getName(), expectedArgType.getName());
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        NodeUtils.getLine(funcCall),
                                        NodeUtils.getColumn(funcCall),
                                        message,
                                        null)
                                );
                                return null;
                            }
                        }
                    }
                }
            }
            else if (givenParameters.getNumChildren() < methodParameters.size()) {
                var message = String.format("Call to function '%s' with insufficient parameters.", methodName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(funcCall),
                        NodeUtils.getColumn(funcCall),
                        message,
                        null)
                );
                return null;
            }
            else if (givenParameters.getNumChildren()>methodParameters.size()){
                var message = String.format("Call to function '%s' with too many parameters.", methodName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(funcCall),
                        NodeUtils.getColumn(funcCall),
                        message,
                        null)
                );
                return null;
            }
            if (!methodParameters.get(0).getType().getName().equals("int...")) {
                var exploreLength = methodParameters.size();
                if (methodParameters.get(methodParameters.size()-1).getType().getName().equals("int...")) exploreLength = exploreLength-1;
                if (exploreLength>givenParameters.getNumChildren()){
                    var message = String.format("Call to function '%s' with insufficient parameters.", methodName);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(funcCall),
                            NodeUtils.getColumn(funcCall),
                            message,
                            null)
                    );
                    return null;
                }
                for (int i = 0; i < exploreLength; i++) {
                    Type givenArgType = new Type("", false);
                    Type expectedArgType = methodParameters.get(i).getType();
                    var currentExplore = givenParameters.getChild(i);
                    while (currentExplore.getKind().equals(Kind.PAREN_EXPR.toString())){
                        currentExplore = currentExplore.getChild(0);
                    }
                    if (currentExplore.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                        var varRefName = currentExplore.get("name");
                        if (varRefName.equals("this")) givenArgType = new Type(table.getClassName(), false);
                        else{
                            for (var element : megaTable) {
                                if (element.getName().equals(varRefName)) {
                                    givenArgType = element.getType();
                                    break;
                                }
                            }
                        }
                    } else if (currentExplore.getKind().equals(Kind.FUNC_CALL.toString())) { // this is archaic, could be better regarding implements, doesnt consider calling funcs like "this.foo()" that dont exist, even without extends
                        var methodVariableInner = "";
                        var methodNameInner = currentExplore.get("id");
                        Type methodCallerTypeInner = new Type("", false);
                        if (!currentExplore.getChild(0).getKind().equals(Kind.FUNC_CALL.toString())){
                            methodVariableInner = currentExplore.getChild(0).get("name");
                        }
                        else{
                            var callingMethod = currentExplore.getChild(0);
                            while(callingMethod.getKind().equals(Kind.FUNC_CALL.toString())){
                                callingMethod = callingMethod.getChild(0);
                            }
                            if (!callingMethod.getKind().equals(Kind.VAR_REF_EXPR.toString())){
                                return null;
                            }
                            if (table.getImports().contains(callingMethod.get("name"))){
                                return null;
                            }
                            else{
                                while(callingMethod.getParent().getKind().equals(Kind.FUNC_CALL.toString())){
                                    var nextReturn = table.getReturnType(callingMethod.getParent().get("id"));
                                    if (!nextReturn.equals(new Type(table.getClassName(), false))){
                                        continue;
                                    }
                                    callingMethod = callingMethod.getParent();
                                    methodCallerTypeInner = nextReturn;
                                }
                            }
                        }
                        if (methodVariableInner.equals("this")) methodCallerTypeInner = new Type(table.getClassName(), false);
                        else if (!methodVariableInner.isEmpty() && !table.getImports().contains(methodVariableInner)) {
                            for (var element : megaTable) {
                                if (element.getName().equals(methodVariableInner)) {
                                    methodCallerTypeInner = element.getType();
                                    break;
                                }
                            }
                        }
                        if (!methodCallerTypeInner.getName().equals(table.getClassName()))
                            givenArgType = expectedArgType;
                        else givenArgType = table.getReturnType(methodNameInner);
                    } else givenArgType = TypeUtils.getExprType(currentExplore, table);
                    if (!givenArgType.equals(expectedArgType)) {
                        if (!(expectedArgType.getName().equals(table.getSuper()) && givenArgType.getName().equals(table.getClassName())) && !(new HashSet<>(table.getImports()).containsAll(Arrays.asList(givenArgType.getName(), expectedArgType.getName())) && !(expectedArgType.getName().equals(table.getClassName()) && givenArgType.getName().equals(table.getSuper())))) {
                            var message = "Call to function '%s' with invalid parameter type '%s'";
                            if (givenArgType.isArray()) message += " array";
                            message += ", expected '%s'";
                            if (expectedArgType.isArray()) message += " array";
                            message = String.format(message, methodName, givenArgType.getName(), expectedArgType.getName());
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(funcCall),
                                    NodeUtils.getColumn(funcCall),
                                    message,
                                    null)
                            );
                            return null;
                        }
                    }
                }
            }
        }

        return null;
    }

    private Void visitReturn(JmmNode returnStmt, SymbolTable table) {
        if (currentMethod==null) return null;

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        String varRefName = "";
        Type elementType = new Type("",false);
        Type expectedReturnType = table.getReturnType(returnStmt.getParent().get("name"));
        JmmNode returnExpr = returnStmt.getChild(0);
        while (returnExpr.getKind().equals(Kind.PAREN_EXPR.toString())){
            returnExpr=returnExpr.getChild(0);
        }
        if (returnExpr.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
            varRefName = returnExpr.get("name");
            var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
            megaTable.addAll(table.getParameters(currentMethod));
            megaTable.addAll(table.getFields());

            for (var element : megaTable){
                if (element.getName().equals(varRefName)){
                    elementType = element.getType();
                    break;
                }
            }
        }
        else if (returnExpr.getKind().equals(Kind.FUNC_CALL.toString())){
            var methodVariable = "";
            var methodName = returnExpr.get("id");
            Type methodCallerType = new Type("", false);
            if (!returnExpr.getChild(0).getKind().equals(Kind.FUNC_CALL.toString())){
                methodVariable = returnExpr.getChild(0).get("name");
            }
            else{
                var callingMethod = returnExpr.getChild(0);
                while(callingMethod.getKind().equals(Kind.FUNC_CALL.toString())){
                    callingMethod = callingMethod.getChild(0);
                }
                if (!callingMethod.getKind().equals(Kind.VAR_REF_EXPR.toString())){
                    return null;
                }
                if (table.getImports().contains(callingMethod.get("name"))){
                    return null;
                }
                else{
                    while(callingMethod.getParent().getKind().equals(Kind.FUNC_CALL.toString())){
                        var nextReturn = table.getReturnType(callingMethod.getParent().get("id"));
                        if (!nextReturn.equals(new Type(table.getClassName(), false))){
                            return null;
                        }
                        callingMethod = callingMethod.getParent();
                        methodCallerType = nextReturn;
                    }
                }
            }
            var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
            megaTable.addAll(table.getParameters(currentMethod));
            megaTable.addAll(table.getFields());
            if (methodVariable.equals("this")) methodCallerType = new Type(table.getClassName(), false);
            else if (!table.getImports().contains(methodVariable)){
                for (var element : megaTable) {
                    if (element.getName().equals(methodVariable)) {
                        methodCallerType = element.getType();
                        break;
                    }
                }
            }
            if (!methodCallerType.getName().equals(table.getClassName())) return null;
            else elementType = table.getReturnType(methodName);
        }
        else if (returnExpr.getKind().equals(Kind.INTEGER_LITERAL.toString()) || returnExpr.getKind().equals(Kind.BINARY_EXPR.toString()) || returnExpr.getKind().equals(Kind.ARRAY_ACCESS.toString()) || returnExpr.getKind().equals(Kind.LENGTH_EXPR.toString())){
            if (!expectedReturnType.equals(new Type("int", false))){
                var message = "Return value of type '%s' given for function '%s' of return type '%s'";
                if (expectedReturnType.isArray()) message += " array";
                message = String.format(message, "int", returnStmt.getParent().get("name"),expectedReturnType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmt),
                        NodeUtils.getColumn(returnStmt),
                        message,
                        null)
                );
                return null;
            }
        }
        else if (returnExpr.getKind().equals(Kind.BOOLEAN.toString()) || returnExpr.getKind().equals(Kind.COMPARISON_EXPR.toString()) || returnExpr.getKind().equals(Kind.BOOLEAN_EXPR.toString()) || returnExpr.getKind().equals(Kind.UNARY_OP.toString())){
            if (!expectedReturnType.equals(new Type("boolean", false))){
                var message = "Return value of type '%s' given for function '%s' of return type '%s'";
                if (expectedReturnType.isArray()) message += " array";
                message = String.format(message, "boolean", returnStmt.getParent().get("name"),expectedReturnType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmt),
                        NodeUtils.getColumn(returnStmt),
                        message,
                        null)
                );
                return null;
            }
        }
        else if (returnExpr.getKind().equals(Kind.ARRAY_INIT.toString())){
            if (!expectedReturnType.equals(new Type("int", true))){
                var message = String.format("Return value of type '%s' array given for function '%s' of return type '%s'","int",returnStmt.getParent().get("name"),expectedReturnType.getName());
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(returnStmt),
                        NodeUtils.getColumn(returnStmt),
                        message,
                        null)
                );
                return null;
            }
        }
        if (elementType.getName().isEmpty()) return null;
        if (!elementType.equals(expectedReturnType)){
            var message = "Return value of type '%s' ";
            if (elementType.isArray()) message+="array ";
            message+="given for function '%s' of return type '%s'";
            if (expectedReturnType.isArray()) message+=" array";
            message = String.format(message, elementType.getName(), returnStmt.getParent().get("name"), expectedReturnType.getName());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

}
