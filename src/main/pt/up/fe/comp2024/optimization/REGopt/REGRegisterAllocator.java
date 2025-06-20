package pt.up.fe.comp2024.optimization.REGopt;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.utils.ReportUtils;

import java.util.*;

public class REGRegisterAllocator {

    private REGGraph buildInterferenceGraph(List<REGInstInfo> insts) {
        REGGraph graph = new REGGraph(); // i'm not 100% sure of this

        for (REGInstInfo inst : insts) {
            Set<String> liveOutSet = inst.getOuts();
            for (String var1 : liveOutSet) {
                graph.addNode(var1);
                for (String var2 : liveOutSet) {
                    if (!var1.equals(var2)) {
                        graph.addEdge(var1, var2);
                    }
                }
            }
        }

        return graph;
    }


    public OllirResult allocateRegisters(OllirResult ollirResult, int maxRegisters) {
        List<Method> methods = ollirResult.getOllirClass().getMethods();
        for (Method method : methods) {
            var additional = 1; // this guarantees that the 1st register is stored for the "this" keyword
            if (method.isStaticMethod()) additional = 0; // if its static, no need to store this
            additional += method.getParams().size(); // stores the parameters in the first registers
            REGLiveness liveness = new REGLiveness();
            List<REGInstInfo> insts = liveness.livenessAnalysis(method);
            REGGraph graph = buildInterferenceGraph(insts);
            REGColoring coloring = new REGColoring();
            coloring.colorGraph(graph);

            if (maxRegisters!=0 && graph.colorsUsed() > maxRegisters) {
                ollirResult.getReports().add(Report.newError(Stage.OPTIMIZATION, -1, -1, "Register allocation failed: " + graph.colorsUsed() + " registers used, " + maxRegisters + " available", null));
                return ollirResult;
            }
            else {
                Map<String, Descriptor> vars = method.getVarTable();
                for (String var : graph.getNodes()) {
                    Descriptor desc = vars.get(var);
                    if (desc != null) {
                        desc.setVirtualReg(graph.getNode(var).getColor()+additional);
                    }
                }
            }
        }
        return ollirResult;
    }

}