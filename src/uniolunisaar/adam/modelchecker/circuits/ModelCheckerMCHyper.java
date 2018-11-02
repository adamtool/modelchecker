package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerMCHyper {

    public static final String LOGGER_MCHYPER_OUT = "mcHyperOut";
    public static final String LOGGER_MCHYPER_ERR = "mcHyperErr";
    public static final String LOGGER_AIGER_OUT = "aigerOut";
    public static final String LOGGER_AIGER_ERR = "aigerErr";
    public static final String LOGGER_ABC_OUT = "abcOut";
    public static final String LOGGER_ABC_ERR = "abcErr";

    public enum VerificationAlgo {
        IC3,
        INT,
        BMC,
        BMC2,
        BMC3
    }

    /**
     * Returns null iff the formula holds.
     *
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param path
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private static CounterExample checkSeparate(VerificationAlgo alg, PetriNet net, AigerRenderer circ, String formula, String path) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        ModelCheckerTools.save2Aiger(net, circ, path);
//        ProcessBuilder procBuilder = new ProcessBuilder(AdamProperties.getInstance().getProperty(AdamProperties.MC_HYPER), path + ".aag", formula, path + "_mcHyperOut");
//
//        Logger.getInstance().addMessage(procBuilder.command().toString(), true);
//        Process procAiger = procBuilder.start();
//        // buffering the output and error as it comes
//        try (BufferedReader is = new BufferedReader(new InputStreamReader(procAiger.getInputStream()))) {
//            String line;
//            while ((line = is.readLine()) != null) {
//                Logger.getInstance().addMessage("[MCHyper-Out]: " + line, true);
//            }
//        }
//        try (BufferedReader is = new BufferedReader(new InputStreamReader(procAiger.getErrorStream()))) {
//            String line;
//            while ((line = is.readLine()) != null) {
//                Logger.getInstance().addMessage("[MCHyper-ERROR]: " + line, true);
//            }
//        }
        // buffering in total
//        String error = IOUtils.toString(procAiger.getErrorStream());
//        Logger.getInstance().addMessage(error, true); // todo: print it as error and a proper exception
//        String output = IOUtils.toString(procAiger.getInputStream());
//        Logger.getInstance().addMessage(output, true);
//        procAiger.waitFor();

        final String timeCommand = "/usr/bin/time";
        final String fileOutput = "-f";
        final String fileArgument = "wall_time_(s)%e\\nCPU_time_(s)%U\\nmemory_(KB)%M\\n";
        final String outputOption = "-o";
        final String outputFile = path + "_stats_time_mem.txt";

        //%%%%%%%%%%%%%%%%%% MCHyper
        String inputFile = path + ".aag";
        String[] command = {AdamProperties.getInstance().getProperty(AdamProperties.MC_HYPER), inputFile, formula, path + "_mcHyperOut"};
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

        // %%%%%%%%%%%%%%%% Aiger
        String[] aiger_command = {AdamProperties.getInstance().getProperty(AdamProperties.AIGER_TOOLS) + "aigtoaig", path + "_mcHyperOut.aag", path + "_mcHyperOut.aig"};

        Logger.getInstance().addMessage("", false);
        Logger.getInstance().addMessage("Calling Aiger ...", false);
        Logger.getInstance().addMessage(Arrays.toString(aiger_command), true);
        ExternalProcessHandler procAiger = new ExternalProcessHandler(aiger_command);
        out = Logger.getInstance().getMessageStream(LOGGER_AIGER_OUT);
        err = Logger.getInstance().getMessageStream(LOGGER_AIGER_ERR);
        outStream = null;
        if (out != null) {
            outStream = new PrintWriter(out, true);
        }
        errStream = null;
        if (err != null) {
            errStream = new PrintWriter(err, true);
        }
        exitValue = procAiger.startAndWaitFor(outStream, errStream);
        if (exitValue != 0) {
            throw new ExternalToolException("Aigertools didn't finshed correctly. 'aigtoaig' couldn't produce an 'aig'-file from '" + path + "_mcHyperOut.aag'");
        }
        Logger.getInstance().addMessage("... finished calling Aiger.", false);
        Logger.getInstance().addMessage("", false);

        // %%%%%%%%%%%%%%% Abc
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
                throw new RuntimeException("Not all verifcation methods had been considered. '" + alg + "' is missing.");
        }
        String[] abc_command = {AdamProperties.getInstance().getProperty(AdamProperties.ABC)};
        Logger.getInstance().addMessage("", false);
        Logger.getInstance().addMessage("Calling abc ...", false);
        Logger.getInstance().addMessage(Arrays.toString(abc_command), true);
        ExternalProcessHandler procAbc = new ExternalProcessHandler(true, abc_command);
        out = Logger.getInstance().getMessageStream(LOGGER_ABC_OUT);
        err = Logger.getInstance().getMessageStream(LOGGER_ABC_ERR);
        outStream = null;
        if (out != null) {
            outStream = new PrintWriter(out, true);
        }
        errStream = null;
        if (err != null) {
            errStream = new PrintWriter(err, true);
        }
        procAbc.start(outStream, errStream);
        PrintWriter abcInput = new PrintWriter(procAbc.getProcessInput());
        abcInput.println("read " + path + "_mcHyperOut.aig");
        abcInput.println(call);
        abcInput.println("write_cex -f " + path + ".cex");
        abcInput.println("quit");
        abcInput.flush();
        procAbc.waitFor();
        if (exitValue != 0) {
            throw new ExternalToolException("ABC didn't finshed correctly.");
        }
        if (procAbc.getOutput().contains("Io_ReadAiger: The network check has failed.")) {
            throw new ExternalToolException("ABC didn't finshed correctly. The check of the aiger network '" + path + "_mcHyperOut.aig' has failed."
                    + " Check the abc output for more information:\n" + procAbc.getOutput());
        }
        if (procAbc.getOutput().contains("There is no current network.")) {
            Logger.getInstance().addMessage("[WARNING] abc says there is no current network to solve."
                    + " Check the abc output for more information:\n" + procAbc.getOutput(), false);
        }
        if (procAbc.getOutput().contains("Empty network.")) {
            Logger.getInstance().addMessage("[WARNING] abc says the current network is empty."
                    + " Check the abc output for more information:\n" + procAbc.getOutput(), false);
        }

        // has a counter example, ergo read it
        if (!procAbc.getOutput().contains("Counter-example is not available")) {
            String file = path + ".cex";
            File f = new File(file);
            if (f.exists() && !f.isDirectory()) {
                boolean safety = procAbc.getOutput().contains("Output 0 of miter \"" + path + "_mcHyperOut\"" + " was asserted in frame");
                boolean liveness = procAbc.getOutput().contains("Output 1 of miter \"" + path + "_mcHyperOut\"" + " was asserted in frame");
                return circ.parseCounterExample(net, file, new CounterExample(safety, liveness));
            } else {
                throw new ExternalToolException("ABC didn't finshed as expected. There should be a counter-example written to '" + file + "'"
                        + " but the file doesn't exist."
                        + " Check the abc output for more information:\n" + procAbc.getOutput());
            }
        }
        Logger.getInstance().addMessage("... finished calling abc.", false);
        Logger.getInstance().addMessage("", false);
        return null;
    }

    /**
     * Returns null iff the formula holds.
     *
     * @param alg
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param path
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniolunisaar.adam.tools.ProcessNotStartedException
     */
    public static CounterExample check(VerificationAlgo alg, PetriNet net, AigerRenderer circ, String formula, String path) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
//        return checkWithPythonScript(net, circ, formula, path);
        return checkSeparate(alg, net, circ, formula, path);
    }

    /**
     * Returns null iff the formula holds.
     *
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param path
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private static CounterExample checkWithPythonScript(PetriNet net, AigerRenderer circ, String formula, String path) throws InterruptedException, IOException, ExternalToolException {
        ModelCheckerTools.save2AigerAndPdf(net, circ, path);
        // version without threads
//        ProcessBuilder procBuilder = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + "/mchyper.py", "-f", formula, path + ".aag", "-pdr", "-cex", "-v", "1", "-o", path + "_complete");
//        procBuilder.directory(new File(AdamProperties.getInstance().getLibFolder() + "/../logic/"));
//        System.out.println(procBuilder.directory());
//        procBuilder = new ProcessBuilder(System.getProperty("libfolder") + "/mchyper");

//        Logger.getInstance().addMessage(procBuilder.command().toString(), true);
//        Process procAiger = procBuilder.start();
//        // buffering the output and error as it comes
//        try (BufferedReader is = new BufferedReader(new InputStreamReader(procAiger.getInputStream()))) {
//            String line;
//            while ((line = is.readLine()) != null) {
//                Logger.getInstance().addMessage(line, true);
//            }
//        }
//        try (BufferedReader is = new BufferedReader(new InputStreamReader(procAiger.getErrorStream()))) {
//            String line;
//            while ((line = is.readLine()) != null) {
//                Logger.getInstance().addMessage(line, true);
//            }
//        }
//        // buffering in total
////        String error = IOUtils.toString(procAiger.getErrorStream());
////        Logger.getInstance().addMessage(error, true); // todo: print it as error and a proper exception
////        String output = IOUtils.toString(procAiger.getInputStream());
////        Logger.getInstance().addMessage(output, true);
//        procAiger.waitFor();
        String[] command = {AdamProperties.getInstance().getProperty(AdamProperties.LIBRARY_FOLDER) + "/mchyper.py", "-f", formula, path + ".aag", "-pdr", "-cex", "-v", "1", "-o", path + "_complete"};
        Logger.getInstance().addMessage(Arrays.toString(command), true);
        ExternalProcessHandler proc = new ExternalProcessHandler(command);
        int exitValue = proc.startAndWaitFor();

        if (exitValue == 255) {
            throw new ExternalToolException("MCHyper didn't finshed correctly.");
        }

        if (exitValue == 42) { // has a counter example, ergo read it
            return circ.parseCounterExample(net, path + "_complete.cex", new CounterExample(false, false));
        }

        return null;
    }

}
