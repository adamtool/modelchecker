package uniolunisaar.adam.logic.modelchecking.circuits;

import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.CounterExample;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;
import java.util.Arrays;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger.CircuitAndLTLtoCircuit;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitFlowLTLMCStatistics;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.processHandling.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.ds.modelchecking.settings.AbcSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.tools.processHandling.ProcessPool;
import uniolunisaar.adam.util.AigerTools;

/**
 *
 * @author Manuel Gieseking
 */
@Deprecated
public class PetriNetModelChecker {

    @Deprecated
    private static ModelCheckingResult checkSeparate(AbcSettings settings, AdamCircuitLTLMCOutputData outputData, PetriNet net, AdamCircuitFlowLTLMCStatistics stats) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        return Abc.call(settings, outputData, stats);
    }

    /**
     * @param inputFile
     * @param alg
     * @param net
     * @param circ
     * @param path
     * @param abcParameters
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    @Deprecated
    public static ModelCheckingResult check(String inputFile, VerificationAlgo alg, PetriNet net, AigerRenderer circ, String path, String abcParameters, AdamCircuitLTLMCOutputData data) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        AbcSettings settings = new AbcSettings(inputFile, abcParameters, true, null, new VerificationAlgo[]{alg});
        settings.setNet(net);
        return check(settings, data, net, null);
    }

    @Deprecated
    public static ModelCheckingResult check(AbcSettings settings, AdamCircuitLTLMCOutputData outputData, PetriNet net, AdamCircuitFlowLTLMCStatistics stats) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
//        return checkWithPythonScript(net, circ, formula, path);
        return checkSeparate(settings, outputData, net, stats);
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
        AigerTools.save2Aiger(circ, path);
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
        ProcessPool.getInstance().putProcess(PetriNetExtensionHandler.getProcessFamilyID(net) + "#mchyper", proc);
        int exitValue = proc.startAndWaitFor();

        if (exitValue == 255) {
            throw new ExternalToolException("MCHyper didn't finshed correctly.");
        }

        if (exitValue == 42) { // has a counter example, ergo read it
            AbcSettings settings = new AbcSettings();
            settings.setNet(net);
            return CounterExampleParser.parseCounterExampleWithStutteringLatch(settings, path + "_complete.cex", new CounterExample(false, false));
        }

        return null;
    }

    @Deprecated
    public static ModelCheckingResult check(VerificationAlgo verificationAlgo, PetriNet net, AigerRenderer renderer, String formula, String path, String abcParameters) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData(path + net.getName(), false, false);
        CircuitAndLTLtoCircuit.createCircuit(renderer, formula, data, null, false, PetriNetExtensionHandler.getProcessFamilyID(net));
        String inputFile = path + net.getName() + ".aig";
        return check(inputFile, verificationAlgo, net, renderer, path, abcParameters, data);
    }

}
