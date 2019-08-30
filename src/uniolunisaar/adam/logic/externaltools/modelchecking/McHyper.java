package uniolunisaar.adam.logic.externaltools.modelchecking;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.processHandling.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer.OptimizationsComplete;
import uniolunisaar.adam.tools.processHandling.ProcessPool;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.logics.OptimizeAigerCircuitsByText;
import uniolunisaar.adam.util.logics.OptimizingAigerCircuitByDataStructure;

/**
 *
 * @author Manuel Gieseking
 */
public class McHyper {

    public static final String LOGGER_MCHYPER_OUT = "mcHyperOut";
    public static final String LOGGER_MCHYPER_ERR = "mcHyperErr";

    public static void call(String inputFile, String formula, String output, boolean verbose, String procFamilyID, OptimizationsComplete opt) throws IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
        String[] command = {AdamProperties.getInstance().getProperty(AdamProperties.MC_HYPER), inputFile, formula, output};
        Logger.getInstance().addMessage("", false);
        Logger.getInstance().addMessage("Calling MCHyper ...", false);
        Logger.getInstance().addMessage(Arrays.toString(command), true);
        ExternalProcessHandler procMCHyper = new ExternalProcessHandler(true, command);
        ProcessPool.getInstance().putProcess(procFamilyID + "#mchyper", procMCHyper);
        PrintStream out = Logger.getInstance().getMessageStream(LOGGER_MCHYPER_OUT);
        PrintStream err = Logger.getInstance().getMessageStream(LOGGER_MCHYPER_ERR);
        PrintWriter outStream = null;
        if (out != null) {
            outStream = new PrintWriter(out, true);
        }
        PrintWriter errStream = null;
        if (err != null) {
            errStream = new PrintWriter(err, true);
        }
        int exitValue = procMCHyper.startAndWaitFor(outStream, errStream);

        if (!verbose) { // cleanup
            Tools.deleteFile(inputFile);
        }
        if (exitValue != 0) {
            String error = "";
            if (procMCHyper.getErrors().contains("mchyper: Prelude.read: no parse")) {
                error = " Error parsing formula: " + formula;
            } else if (procMCHyper.getErrors().contains("mchyper: " + inputFile + ": openFile: does not exist (No such file or directory)")) {
                error = " File '" + inputFile + "' does not exist (No such file or directory).";
            } else if (procMCHyper.getErrors().contains("mchyper: Map.!: given key is not an element in the map")) {
                error = " A used atom in the formula does not exists in the system.";
            }
            throw new ExternalToolException("MCHyper didn't finshed correctly." + error);
        }
        Logger.getInstance().addMessage("... finished calling MCHyper", false);

        // Possibly optimize
        switch (opt) {
            case NONE:
                break;
            case NB_GATES_BY_REGEX:
                OptimizeAigerCircuitsByText.optimizeByTextReplacement(output, false);
                break;
            case NB_GATES_BY_REGEX_WITH_IDX_SQUEEZING:
                OptimizeAigerCircuitsByText.optimizeByTextReplacement(output, true);
                break;
            case NB_GATES_BY_DS:
                OptimizingAigerCircuitByDataStructure.optimizeByCreatingAigerfileAndRendering(output, AigerRenderer.OptimizationsSystem.NB_GATES);
                break;
            case NB_GATES_BY_DS_WITH_IDX_SQUEEZING:
                OptimizingAigerCircuitByDataStructure.optimizeByCreatingAigerfileAndRendering(output, AigerRenderer.OptimizationsSystem.NB_GATES_AND_INDICES);
                break;
            case NB_GATES_BY_DS_WITH_IDX_SQUEEZING_AND_EXTRA_LIST:
                OptimizingAigerCircuitByDataStructure.optimizeByCreatingAigerfileAndRendering(output, AigerRenderer.OptimizationsSystem.NB_GATES_AND_INDICES_EXTRA);
                break;
        }
        Logger.getInstance().addMessage("", false);
    }

}
