package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.ds.circuits.AigerFile;
import java.io.BufferedReader;
import java.io.FileReader;
import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import java.io.IOException;
import java.util.Arrays;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.externaltools.Abc;
import uniolunisaar.adam.externaltools.Abc.VerificationAlgo;
import uniolunisaar.adam.externaltools.AigToAig;
import uniolunisaar.adam.externaltools.McHyper;
import uniolunisaar.adam.modelchecker.exceptions.ExternalToolException;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.modelchecker.util.Statistics;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.ProcessNotStartedException;
import uniolunisaar.adam.tools.Tools;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerMCHyper {

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
    private static ModelCheckingResult checkSeparate(VerificationAlgo alg, PetriNet net, AigerRenderer circ, String formula, String path, Statistics stats, String abcParameter, boolean verbose) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        // Create System 
        AigerFile circuit = circ.render(net);
        Tools.saveFile(path + ".aag", circuit.toString());

//        ModelCheckerTools.save2Aiger(net, circ, path);
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
        String outputPath = path + "_mcHyperOut";
        McHyper.call(inputFile, formula, outputPath, verbose);

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            // system size
            stats.setSys_nb_latches(circuit.getNbOfLatches());
            stats.setSys_nb_gates(circuit.getNbOfGates());
            // total size 
            try (BufferedReader mcHyperAag = new BufferedReader(new FileReader(outputPath + ".aag"))) {
                String header = mcHyperAag.readLine();
                String[] vals = header.split(" ");
                stats.setTotal_nb_latches(Integer.parseInt(vals[3]));
                stats.setTotal_nb_gates(Integer.parseInt(vals[5]));
            }
            Logger.getInstance().addMessage(stats.getInputSizes(), true);
            // if a file is given already write them to the file
            stats.writeInputSizesToFile();
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS

        // %%%%%%%%%%%%%%%% Aiger
        inputFile = path + "_mcHyperOut.aag";
        outputPath = path + "_mcHyperOut.aig";
        AigToAig.call(inputFile, outputPath, verbose);

        // %%%%%%%%%%%%%%% Abc
        inputFile = path + "_mcHyperOut.aig";
        outputPath = path + ".cex";
        String abcOutput = Abc.call(inputFile, abcParameter, outputPath, alg, verbose);
        ModelCheckingResult ret = Abc.parseOutput(path, abcOutput, net, circ, outputPath, verbose);
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            if (ret.getSatisfied().equals(ModelCheckingResult.Satisfied.FALSE)) {
                stats.setSatisfied(0);
            } else if (ret.getSatisfied().equals(ModelCheckingResult.Satisfied.TRUE)) {
                stats.setSatisfied(1);
            }
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS

        return ret;
    }

    /**
     * Returns null iff the formula holds.
     *
     * @param alg
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param path
     * @param abcParameters
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniolunisaar.adam.tools.ProcessNotStartedException
     */
    public static ModelCheckingResult check(VerificationAlgo alg, PetriNet net, AigerRenderer circ, String formula, String path, String abcParameters) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        return check(alg, net, circ, formula, path, null, abcParameters, true);
    }

    /**
     * Returns null iff the formula holds.
     *
     * @param alg
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param path
     * @param stats
     * @param abcParameters
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniolunisaar.adam.tools.ProcessNotStartedException
     */
    public static ModelCheckingResult check(VerificationAlgo alg, PetriNet net, AigerRenderer circ, String formula, String path, Statistics stats, String abcParameters, boolean verbose) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
//        return checkWithPythonScript(net, circ, formula, path);
        return checkSeparate(alg, net, circ, formula, path, stats, abcParameters, verbose);
    }

    /**
     * Returns null iff the formula holds.
     *
     * The python script does not return the correct things when abc, aiger,
     * mchyper is crashing or other things happen. Use the separate method
     * instead.
     *
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param path
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Deprecated
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
