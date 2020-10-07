package uniolunisaar.adam.logic.externaltools.modelchecking;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.CTLLoLAModelcheckingSettings;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.IOUtils;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.tools.processHandling.ExternalProcessHandler;
import uniolunisaar.adam.tools.processHandling.ProcessPool;

/**
 *
 * @author Manuel Gieseking
 */
public class LoLA {

    public static final String LOGGER_LOLA_OUT = "lolaOut";
    public static final String LOGGER_LOLA_ERR = "lolaErr";

    public static CTLModelcheckingResult call(String inputFile, String formula, CTLLoLAModelcheckingSettings settings, String procFamilyID) throws IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
        final String lola = AdamProperties.getInstance().getProperty(AdamProperties.LOLA);
        // the following version doesn't care about fairness assumptions (at least for LTL), cp:
        //  ./src/Exploration/LTLExploration.h:class LTLExploration // In this version, we do not care about fairness!!!!!!!
//        final String lola = "/home/thewn/tools/mcc2019/lola-tool/lola_bin_mcc"; 
        final String argFile = inputFile;
        final String argFormula = "--formula=" + formula + "";
        final String argOutput = "--json=" + settings.getJsonPath() + "";
        final String argState = "--state=" + settings.getWitnessStatePath() + "";
        final String argPath = "--path=" + settings.getWitnessPathPath() + "";

        String[] command = {lola, argFile, argFormula, argOutput, argState, argPath};
        Logger.getInstance().addMessage("Calling LoLA ...", false);
        Logger.getInstance().addMessage(Arrays.toString(command), true);

        ExternalProcessHandler procHandlerLoLA = new ExternalProcessHandler(true, command);
        ProcessPool.getInstance().putProcess(procFamilyID + "#lola", procHandlerLoLA);
        PrintStream out = Logger.getInstance().getMessageStream(LOGGER_LOLA_OUT);
        PrintStream err = Logger.getInstance().getMessageStream(LOGGER_LOLA_ERR);
        PrintWriter outStream = null;
        if (out != null) {
            outStream = new PrintWriter(out, true);
        }
        PrintWriter errStream = null;
        if (err != null) {
            errStream = new PrintWriter(err, true);
        }
        int exitValue = procHandlerLoLA.startAndWaitFor(outStream, errStream);

        CTLModelcheckingResult result = new CTLModelcheckingResult(procHandlerLoLA.getOutput(), procHandlerLoLA.getErrors());

        try (FileInputStream inputStream = new FileInputStream(settings.getJsonPath())) {
            String output = IOUtils.streamToString(inputStream);
//            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%");
//            System.out.println(output);
//            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%");
            // %%% START OLD LoLA format parsing
            int start_idx = output.indexOf("result");
            int endA_idx = output.indexOf('}', start_idx);
            int endB_idx = output.indexOf(',', start_idx);
            int end_idx = (endA_idx < endB_idx) ? endA_idx : endB_idx;
            if (start_idx == -1) { // no result found
                int idx = output.indexOf("error");
                if (idx != -1) {
                    throw new ExternalToolException("LoLA: \"" + output.substring(idx, output.indexOf(",", idx)));
                }
                throw new ExternalToolException("LoLA ended unexpectedly");
            }
            String line = output.substring(start_idx, end_idx);
            // %% END OLD LoLA format            

            // %% NEW LoLA format (mcc2019 version)
////            int result_idx = output.indexOf("result");
////            int value_idx = output.indexOf("value", result_idx);
////            int end_line = output.indexOf("\n", value_idx);
////            String line = output.substring(value_idx, end_line);
//            // %% END LoLA format (mcc2019 version)
            if (line.contains("true")) {
                result.setSat(ModelCheckingResult.Satisfied.TRUE);
            } else if (line.contains("false")) {
                result.setSat(ModelCheckingResult.Satisfied.FALSE);
            } else {
                result.setSat(ModelCheckingResult.Satisfied.UNKNOWN);
            }
            if (output.contains("witness state")) {
                try (FileInputStream is = new FileInputStream(settings.getWitnessStatePath())) {
                    String state = IOUtils.streamToString(is);
                    if (!state.isEmpty()) {
                        result.setWitnessState(state);
                    }
//                    Logger.getInstance().addMessage("Witness state: " + IOUtils.streamToString(is), true);
                }
            }
            if (output.contains("witness path")) {
                try (FileInputStream is = new FileInputStream(settings.getWitnessPathPath())) {
//                    Logger.getInstance().addMessage("Witness path: " + IOUtils.streamToString(is), true);
                    String path = IOUtils.streamToString(is);
                    if (!path.isEmpty()) {
                        result.setWitnessPath(path);
                    }
                }
            }
        }
        if (!settings.isVerbose()) { // cleanup
            Tools.deleteFile(settings.getJsonPath());
            Tools.deleteFile(settings.getWitnessStatePath());
            Tools.deleteFile(settings.getWitnessPathPath());
        }

        return result;
    }
}
