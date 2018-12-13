package uniolunisaar.adam.externaltools;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.ProcessNotStartedException;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
public class McHyper {

    public static final String LOGGER_MCHYPER_OUT = "mcHyperOut";
    public static final String LOGGER_MCHYPER_ERR = "mcHyperErr";

    public static void call(String inputFile, String formula, String output, boolean verbose) throws IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
        String[] command = {AdamProperties.getInstance().getProperty(AdamProperties.MC_HYPER), inputFile, formula, output};
        Logger.getInstance().addMessage("", false);
        Logger.getInstance().addMessage("Calling MCHyper ...", false);
        Logger.getInstance().addMessage(Arrays.toString(command), true);
        ExternalProcessHandler procMCHyper = new ExternalProcessHandler(true, command);
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
            }
            throw new ExternalToolException("MCHyper didn't finshed correctly." + error);
        }
        Logger.getInstance().addMessage("... finished calling MCHyper", false);
        Logger.getInstance().addMessage("", false);
    }

}
