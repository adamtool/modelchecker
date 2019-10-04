package uniolunisaar.adam.logic.externaltools.modelchecking;

import uniolunisaar.adam.ds.modelchecking.settings.AbcSettings;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.CounterExample;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.logic.modelchecking.circuits.CounterExampleParser;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.processHandling.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.processHandling.ProcessPool;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.benchmarks.modelchecking.BenchmarksMC;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;

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

    public static ModelCheckingResult call(AbcSettings settings, AdamCircuitLTLMCOutputData outputData, PetriNet net, AdamCircuitLTLMCStatistics stats) throws IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
        if (settings.getVerificationAlgos().length == 0) {
            throw new InvalidParameterException("At least one verification algorithm has to be given.");
        }
        if (settings.getVerificationAlgos().length == 1) {
            String outputFile = outputData.getPath() + ".cex";
            String abcOutput = call(settings.getInputFile(), settings.getParameters(), outputFile, settings.getVerificationAlgos()[0], settings.isVerbose(), settings.getProcessFamilyID(), stats != null && stats.isMeasure_abc());
            ModelCheckingResult result = Abc.parseOutput(outputData.getPath(), abcOutput, net, outputFile, settings.isVerbose(), stats);
            result.setAlgo(settings.getVerificationAlgos()[0]);
            return result;
        }
        // Start all given verifier and falsifier in parallel, return when the first finished with a real result        
        final CountDownLatch latch = new CountDownLatch(1);
        final ModelCheckingResult result = new ModelCheckingResult();
        for (VerificationAlgo verificationAlgo : settings.getVerificationAlgos()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String outputFile = outputData.getPath() + ".cex";
                        String abcOutput = call(settings.getInputFile(), settings.getParameters(), outputFile, verificationAlgo, settings.isVerbose(), settings.getProcessFamilyID(), stats != null && stats.isMeasure_abc());
                        ModelCheckingResult out = Abc.parseOutput(outputData.getPath(), abcOutput, net, outputFile, settings.isVerbose(), stats);
                        if (out.getSatisfied() != ModelCheckingResult.Satisfied.UNKNOWN) {
                            result.setCex(out.getCex());
                            result.setSat(out.getSatisfied());
                            result.setAlgo(verificationAlgo);
                            latch.countDown();
                        }
                    } catch (IOException | InterruptedException | ProcessNotStartedException | ExternalToolException ex) {
                        // todo: should here be some handling? For example the killed processes will send an ABC didn't finished correctly...
                    }
                }
            }).start();
        }
        latch.await(); // todo: problem that it blocks when no processes returns a proper result
        // Kill all the other parallel algos
        for (VerificationAlgo verificationAlgo : settings.getVerificationAlgos()) {
            if (!verificationAlgo.equals(result.getAlgo())) {
                ProcessPool.getInstance().destroyForciblyProcessAndChilds(settings.getProcessFamilyID() + "#abc" + verificationAlgo.name());
            }
        }
        return result;
    }

    private static String call(String inputFile, String parameters, String outputFile, VerificationAlgo alg, boolean verbose, String procFamilyID, boolean measure) throws IOException, InterruptedException, ProcessNotStartedException, ExternalToolException {
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

        String[] abc_command;
        if (measure) {
            String[] abcCommand = {AdamProperties.getInstance().getProperty(AdamProperties.TIME), "-f", "wall_time_(s)%e;CPU_time_(s)%U;memory_(KB)%M;",
                AdamProperties.getInstance().getProperty(AdamProperties.ABC)};
            abc_command = abcCommand;
        } else {
            String[] abcCommand = {AdamProperties.getInstance().getProperty(AdamProperties.ABC)};
            abc_command = abcCommand;
        }
        Logger.getInstance().addMessage("", false);
        Logger.getInstance().addMessage("Calling abc ...", false);
        Logger.getInstance().addMessage(Arrays.toString(abc_command), true);
        ExternalProcessHandler procAbc = new ExternalProcessHandler(true, abc_command);
        ProcessPool.getInstance().putProcess(procFamilyID + "#abc" + alg.name(), procAbc);
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
        abcInput.println("write_cex -f " + outputFile);
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

    private static ModelCheckingResult parseOutput(String path, String abcOutput, PetriNet net, String cexOutputFile, boolean verbose, AdamCircuitLTLMCStatistics stats) throws ExternalToolException, IOException {
        Logger.getInstance().addMessage("Parsing abc output ...", false);
        //todo: hack for checking the abc output
//        Logger.getInstance().addMessage(abcOutput, false, true);
        // Parse the output to know the result of the model checking
        ModelCheckingResult ret = new ModelCheckingResult();
        // for the falsifiers when they didn't finished
        if (abcOutput.contains("No output asserted in")) {
            Logger.getInstance().addWarning("The bound for the falsifier was not big enough.");
            ret.setSat(ModelCheckingResult.Satisfied.UNKNOWN);
        } else if (!abcOutput.contains("Counter-example is not available")) {
            // has a counter example, ergo read it
            File f = new File(cexOutputFile);
            if (f.exists() && !f.isDirectory()) {
                boolean safety = abcOutput.contains("Output 0 of miter \"" + path + "\"" + " was asserted in frame");
                boolean liveness = abcOutput.contains("Output 1 of miter \"" + path + "\"" + " was asserted in frame");
                CounterExample cex = CounterExampleParser.parseCounterExampleWithStutteringLatch(net, cexOutputFile, new CounterExample(safety, liveness));
                ret.setCex(cex);
                ret.setSat(ModelCheckingResult.Satisfied.FALSE);
                if (!verbose) { // cleanup
                    Tools.deleteFile(cexOutputFile);
                }
            } else {
                throw new ExternalToolException("ABC didn't finshed as expected. There should be a counter-example written to '" + cexOutputFile + "'"
                        + " but the file doesn't exist."
                        + " Check the abc output for more information:\n" + abcOutput);
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
            if (stats.isMeasure_abc()) {
                String key = "CPU_time_(s)";
                int idx = abcOutput.indexOf(key) + key.length();
                stats.setAbc_sec(Double.parseDouble(abcOutput.substring(idx, abcOutput.indexOf(';', idx))));
                key = "memory_(KB)";
                idx = abcOutput.indexOf(key) + key.length();
                stats.setAbc_mem(Long.parseLong(abcOutput.substring(idx, abcOutput.indexOf(';', idx))));
            }
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS
        Logger.getInstance().addMessage("... finished parsing abc output.", false);
        Logger.getInstance().addMessage("", false);
        return ret;
    }
}
