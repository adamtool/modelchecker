package uniolunisaar.adam.modelchecker.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.FormulaBinary;
import uniolunisaar.adam.logic.flowltl.IFormula;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.RunFormula;
import uniolunisaar.adam.logic.util.FormulaCreatorIngoingSemantics;
import uniolunisaar.adam.logic.util.FormulaCreatorOutgoingSemantics;
import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import uniolunisaar.adam.modelchecker.circuits.ModelCheckerLTL;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerTools {

    public static List<FlowFormula> getFlowFormulas(IFormula formula) {
        List<FlowFormula> flowFormulas = new ArrayList<>();
        if (formula instanceof FlowFormula) {
            flowFormulas.add((FlowFormula) formula);
            return flowFormulas;
        } else if (formula instanceof ILTLFormula) {
            return flowFormulas;
        } else if (formula instanceof RunFormula) {
            return getFlowFormulas(((RunFormula) formula).getPhi());
        } else if (formula instanceof FormulaBinary) {
            FormulaBinary binF = (FormulaBinary) formula;
            flowFormulas.addAll(getFlowFormulas(binF.getPhi1()));
            flowFormulas.addAll(getFlowFormulas(binF.getPhi2()));
        }
        return flowFormulas;
    }

    /**
     *
     * @param maximality
     * @param semantics
     * @param net
     * @return null iff MAX_NONE
     */
    public static ILTLFormula getMaximality(ModelCheckerLTL.Maximality maximality, ModelCheckerLTL.TransitionSemantics semantics, PetriNet net) {
        switch (maximality) {
            case MAX_INTERLEAVING:
                if (semantics == ModelCheckerLTL.TransitionSemantics.INGOING) {
                    return FormulaCreatorIngoingSemantics.getMaximalityInterleavingDirectAsObject(net);
                } else {
                    return FormulaCreatorOutgoingSemantics.getMaximalityInterleavingDirectAsObject(net);
                }
            case MAX_CONCURRENT:
                if (semantics == ModelCheckerLTL.TransitionSemantics.INGOING) {
                    return FormulaCreatorIngoingSemantics.getMaximalityConcurrentDirectAsObject(net);
                } else {
                    return FormulaCreatorOutgoingSemantics.getMaximalityConcurrentDirectAsObject(net);
                }
            case MAX_NONE:
                return null;
            case MAX_INTERLEAVING_IN_CIRCUIT:
                return null;
        }
        throw new RuntimeException("Not all maximality terms had been considered: " + maximality);
    }

    public static void save2Aiger(PetriNet net, AigerRenderer renderer, String path) throws FileNotFoundException {
        String aigerFile = renderer.render(net).toString();
        // save aiger file
        try (PrintStream out = new PrintStream(path + ".aiger")) {
            out.println(aigerFile);
        }
    }

    public static void save2AigerAndDot(PetriNet net, AigerRenderer renderer, String path) throws FileNotFoundException, IOException, InterruptedException {
        save2Aiger(net, renderer, path);
//        String command = AdamProperties.getInstance().getLibFolder() + "/aigtodot";
//        Logger.getInstance().addMessage(command, true);
//        ProcessBuilder procBuilder = new ProcessBuilder(command, path + ".aiger", path + "_circuit.dot");
//        Process proc = procBuilder.start();
//        proc.waitFor();
//        String error = IOUtils.toString(proc.getErrorStream());
//        Logger.getInstance().addMessage(error, true);
//        String output = IOUtils.toString(proc.getInputStream());
//        Logger.getInstance().addMessage(output, true);
    }

    public static void save2AigerAndDotAndPdf(PetriNet net, AigerRenderer renderer, String path) throws FileNotFoundException, IOException, InterruptedException {
        save2AigerAndDot(net, renderer, path);
        // show circuit
//        String dotCommand = "dot -Tpdf " + path + "_circuit.dot -o " + path + "_circuit.pdf";
//        Logger.getInstance().addMessage(dotCommand, true);
//        ProcessBuilder procBuilder = new ProcessBuilder("dot", "-Tpdf", path + "_circuit.dot", "-o", path + "_circuit.pdf");
//        Process proc = procBuilder.start();
//        proc.waitFor();
//        String error = IOUtils.toString(proc.getErrorStream());
//        Logger.getInstance().addMessage(error, true);
//        String output = IOUtils.toString(proc.getInputStream());
//        Logger.getInstance().addMessage(output, true);
    }

    public static void save2AigerAndPdf(PetriNet net, AigerRenderer renderer, String path) throws FileNotFoundException, IOException, InterruptedException {
        String bufferpath = path + "_" + System.currentTimeMillis();
        save2AigerAndDotAndPdf(net, renderer, bufferpath);
//        // Delete dot file
//        new File(bufferpath + "_circuit.dot").delete();
//        Logger.getInstance().addMessage("Deleted: " + bufferpath + "_circuit.dot", true);
//        // move to original name
//        Files.move(new File(bufferpath + "_circuit.pdf").toPath(), new File(path + "_circuit.pdf").toPath(), REPLACE_EXISTING);
//        Logger.getInstance().addMessage("Moved: " + bufferpath + "_circuit.pdf --> " + path + "_circuit.pdf", true);
        Files.move(new File(bufferpath + ".aiger").toPath(), new File(path + ".aag").toPath(), REPLACE_EXISTING);
//        Logger.getInstance().addMessage("Moved: " + bufferpath + ".aiger --> " + path + ".aag", true);
    }
}
