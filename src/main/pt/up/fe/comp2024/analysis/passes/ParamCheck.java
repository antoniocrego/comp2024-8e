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

import java.util.ArrayList;
import java.util.List;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class ParamCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.FUNC_CALL, this::visitParams);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        try{
            List<Symbol> methodParameters;
            methodParameters = table.getParameters(currentMethod);
            for (int i = 0; i<methodParameters.size()-1; i++){
                if (methodParameters.get(0).getType().getName().equals("int...")){
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
        return null;
    }

    private Void visitParams(JmmNode funcCall, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference

        try{
            var methodVariable = funcCall.getChild(0).get("name");
            var methodName = funcCall.get("id");
            Type methodCallerType = new Type("", false);
            var megaTable = new ArrayList<>(table.getLocalVariables(currentMethod));
            megaTable.addAll(table.getParameters(currentMethod));
            megaTable.addAll(table.getFields());

            for (var element : megaTable){
                if (element.getName().equals(methodVariable)){
                    methodCallerType = element.getType();
                    break;
                }
            }

            if (!methodCallerType.getName().equals(table.getClassName())) return null;

            List<Symbol> methodParameters;

            try{
                methodParameters = table.getParameters(methodName);
            }
            catch(Exception e){
                return null;
            }

            var givenParameters = funcCall.getChild(funcCall.getNumChildren()-1);
            if (!givenParameters.getKind().equals(Kind.FUNC_ARGS.toString()) && !methodParameters.isEmpty()){
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
            else if (givenParameters.getKind().equals(Kind.FUNC_ARGS.toString())){
                if(givenParameters.getNumChildren()<methodParameters.size()){
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
                if(givenParameters.getNumChildren()>methodParameters.size()){
                    if (methodParameters.get(methodParameters.size()-1).getName().equals("int...")){
                        for (int i = methodParameters.size()-1; i<givenParameters.getNumChildren(); i++){
                            Type givenArgType = new Type("",false);
                            Type expectedArgType = new Type("int", false);
                            if (givenParameters.getChild(i).getKind().equals(Kind.VAR_REF_EXPR.toString())){
                                var varRefName = givenParameters.getChild(i).get("name");
                                for (var element : megaTable){
                                    if (element.getName().equals(varRefName)){
                                        givenArgType = element.getType();
                                        break;
                                    }
                                }
                            }
                            else if (givenParameters.getChild(i).getKind().equals(Kind.FUNC_CALL.toString())){ // this is archaic, could be better regarding implements, doesnt consider calling funcs like "this.foo()" that dont exist, even without extends
                                var methodVariableInner = givenParameters.getChild(i).getChild(0).get("name");
                                var methodNameInner = givenParameters.getChild(i).get("id");
                                Type methodCallerTypeInner = new Type("", false);
                                if (methodVariableInner.equals("this")) methodCallerTypeInner = new Type(table.getClassName(), false);
                                else if (!table.getImports().contains(methodVariableInner)){
                                    for (var element : megaTable) {
                                        if (element.getName().equals(methodVariableInner)) {
                                            methodCallerTypeInner = element.getType();
                                            break;
                                        }
                                    }
                                }
                                if (!methodCallerTypeInner.getName().equals(table.getClassName())) givenArgType=expectedArgType;
                                else givenArgType = table.getReturnType(methodNameInner);
                            }
                            else givenArgType = TypeUtils.getExprType(givenParameters.getChild(i),table);
                            if (!givenArgType.equals(expectedArgType)){
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
                    else{
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
                }
                for (int i = 0; i<methodParameters.size(); i++){
                    Type givenArgType = new Type("",false);
                    Type expectedArgType = methodParameters.get(i).getType();
                    if (givenParameters.getChild(i).getKind().equals(Kind.VAR_REF_EXPR.toString())){
                        var varRefName = givenParameters.getChild(i).get("name");
                        for (var element : megaTable){
                            if (element.getName().equals(varRefName)){
                                givenArgType = element.getType();
                                break;
                            }
                        }
                    }
                    else if (givenParameters.getChild(i).getKind().equals(Kind.FUNC_CALL.toString())){ // this is archaic, could be better regarding implements, doesnt consider calling funcs like "this.foo()" that dont exist, even without extends
                        var methodVariableInner = givenParameters.getChild(i).getChild(0).get("name");
                        var methodNameInner = givenParameters.getChild(i).get("id");
                        Type methodCallerTypeInner = new Type("", false);
                        if (methodVariableInner.equals("this")) methodCallerTypeInner = new Type(table.getClassName(), false);
                        else if (!table.getImports().contains(methodVariableInner)){
                            for (var element : megaTable) {
                                if (element.getName().equals(methodVariableInner)) {
                                    methodCallerTypeInner = element.getType();
                                    break;
                                }
                            }
                        }
                        if (!methodCallerTypeInner.getName().equals(table.getClassName())) givenArgType=expectedArgType;
                        else givenArgType = table.getReturnType(methodNameInner);
                    }
                    else givenArgType = TypeUtils.getExprType(givenParameters.getChild(i),table);
                    if (!givenArgType.equals(expectedArgType)){
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
        catch(Exception e){
            var message = String.format("Unexpected method parameters error.");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(funcCall),
                    NodeUtils.getColumn(funcCall),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

}
