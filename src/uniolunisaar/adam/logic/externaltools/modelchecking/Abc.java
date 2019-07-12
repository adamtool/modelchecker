package uniolunisaar.adam.logic.externaltools.modelchecking;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.CounterExample;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.ModelcheckingStatistics;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.modelchecking.circuits.CounterExampleParser;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.ProcessPool;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.logics.benchmarks.mc.BenchmarksMC;

/**
 *
 * @author Manuel Gieseking
 */
public class Abc {

    public static final String LOGGER_ABC_OUT = "abcOut";
    public static final String LOGGER_ABC_ERR = "abcErr";

    public enum VerificationAlgo {
        IC3,
        INT,
        BMC,
        BMC2,
        BMC3
    }

    public static String call(String inputFile, String parameters, String output, VerificationAlgo alg, boolean verbose, String procFamilyID) throws IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
        String call;
        switch (alg) {
            case IC3:
                call = "pdr";
                break;
            case INT:
                call = "int";
                break;
            case BMC:
                call = "bmc";
                break;
            case BMC2:
                call = "bmc2";
                break;
            case BMC3:
                call = "bmc3";
                break;
            default:
                throw new RuntimeException("Not all verification methods had been considered: '" + alg + "' is missing.");
        }

        String[] abc_command = {AdamProperties.getInstance().getProperty(AdamProperties.TIME), "-f", "wall_time_(s)%e;CPU_time_(s)%U;memory_(KB)%M;",
            AdamProperties.getInstance().getProperty(AdamProperties.ABC)};
        Logger.getInstance().addMessage("", false);
        Logger.getInstance().addMessage("Calling abc ...", false);
        Logger.getInstance().addMessage(Arrays.toString(abc_command), true);
        ExternalProcessHandler procAbc = new ExternalProcessHandler(true, abc_command);
        ProcessPool.getInstance().putProcess(procFamilyID + "#abc", procAbc);
        PrintStream out = Logger.getInstance().getMessageStream(LOGGER_ABC_OUT);
        PrintStream err = Logger.getInstance().getMessageStream(LOGGER_ABC_ERR);
        PrintWriter outStream = null;
        if (out != null) {
            outStream = new PrintWriter(out, true);
        }
        PrintWriter errStream = null;
        if (err != null) {
            errStream = new PrintWriter(err, true);
        }
        procAbc.start(outStream, errStream);
        PrintWriter abcInput = new PrintWriter(procAbc.getProcessInput());
        abcInput.println("read " + inputFile);
        abcInput.println(call + " " + parameters);
        abcInput.println("write_cex -f " + output);
        abcInput.println("quit");
        abcInput.flush();
        int exitValue = procAbc.waitFor();
        if (!verbose) { // cleanup
            Tools.deleteFile(inputFile);
        }
        if (exitValue != 0) {
            throw new ExternalToolException("ABC didn't finshed correctly.");
        }
        String abcOutput = procAbc.getOutput();
        if (abcOutput.contains("Io_ReadAiger: The network check has failed.")) {
            throw new ExternalToolException("ABC didn't finshed correctly. The check of the aiger network '" + inputFile + "' has failed."
                    + " Check the abc output for more information:\n" + procAbc.getOutput());
        }
        if (abcOutput.contains("There is no current network.")) {
            Logger.getInstance().addMessage("[WARNING] abc says there is no current network to solve."
                    + " Check the abc output for more information:\n" + procAbc.getOutput(), false);
        }
        if (abcOutput.contains("Empty network.")) {
            Logger.getInstance().addMessage("[WARNING] abc says the current network is empty."
                    + " Check the abc output for more information:\n" + procAbc.getOutput(), false);
        }
        Logger.getInstance().addMessage("... finished calling abc.", false);
        return abcOutput + "\nERRORS: " + procAbc.getErrors();
    }

    public static ModelCheckingResult parseOutput(String path, String output, PetriNet net, AigerRenderer circ, String cexFile, boolean verbose, ModelcheckingStatistics stats) throws ExternalToolException, IOException {
        Logger.getInstance().addMessage("Parsing abc output ...", false);
        //todo: hack for checking the abc output
//        Logger.getInstance().addMessage(abcOutput, false, true);
        // Parse the output to know the result of the model checking
        ModelCheckingResult ret = new ModelCheckingResult();
        // for the falsifiers when they didn't finished
        if (output.contains("No output asserted in")) {
            Logger.getInstance().addWarning("The bound for the falsifier was not big enough.");
            ret.setSat(ModelCheckingResult.Satisfied.UNKNOWN);
        } else if (!output.contains("Counter-example is not available")) {
            // has a counter example, ergo read it
            File f = new File(cexFile);
            if (f.exists() && !f.isDirectory()) {
                boolean safety = output.contains("Output 0 of miter \"" + path + "\"" + " was asserted in frame");
                boolean liveness = output.contains("Output 1 of miter \"" + path + "\"" + " was asserted in frame");
                CounterExample cex = CounterExampleParser.parseCounterExampleWithStutteringLatch(net, cexFile, new CounterExample(safety, liveness));
                ret.setCex(cex);
                ret.setSat(ModelCheckingResult.Satisfied.FALSE);
                if (!verbose) { // cleanup
                    Tools.deleteFile(cexFile);
                }
            } else {
                throw new ExternalToolException("ABC didn't finshed as expected. There should be a counter-example written to '" + cexFile + "'"
                        + " but the file doesn't exist."
                        + " Check the abc output for more information:\n" + output);
            }
        } else {
            ret.setSat(ModelCheckingResult.Satisfied.TRUE);
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            if (ret.getSatisfied().equals(ModelCheckingResult.Satisfied.FALSE)) {
                stats.setSatisfied(0);
            } else if (ret.getSatisfied().equals(ModelCheckingResult.Satisfied.TRUE)) {
                stats.setSatisfied(1);
            }
            if (BenchmarksMC.EDACC) {
                String out = stats.isSatisfied() == 0 ? "unsat" : stats.isSatisfied() == 1 ? "sat" : "unknown";
                Logger.getInstance().addMessage("" + out, "edacc");
            }
            // get the time data
            String key = "CPU_time_(s)";
            int idx = output.indexOf(key) + key.length();
            stats.setAbc_sec(Double.parseDouble(output.substring(idx, output.indexOf(';', idx))));
            key = "memory_(KB)";
            idx = output.indexOf(key) + key.length();
            stats.setAbc_mem(Long.parseLong(output.substring(idx, output.indexOf(';', idx))));
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS
        Logger.getInstance().addMessage("... finished parsing abc output.", false);
        Logger.getInstance().addMessage("", false);
        return ret;
    }
}
