package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger;

import uniolunisaar.adam.ds.circuits.AigerFile;
import java.io.BufferedReader;
import java.io.FileReader;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import java.io.IOException;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.logic.externaltools.pnwt.AigToAig;
import uniolunisaar.adam.logic.externaltools.modelchecking.McHyper;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.PetriNetExtensionHandler;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.benchmarks.modelchecking.BenchmarksMC;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.util.AigerTools;

/**
 *
 * @author Manuel Gieseking
 */
public class CircuitAndLTLtoCircuit {

    /**
     *
     *
     * @param net
     * @param circ
     * @param formula - in MCHyper format
     * @param data
     * @param stats
     * @throws InterruptedException
     * @throws IOException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public static void createCircuit(PetriNet net, AigerRenderer circ, String formula, AdamCircuitLTLMCOutputData data, AdamCircuitLTLMCStatistics stats) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        // Create System 
        String output = data.getPath();
        String input = output + "_system.aag";
        AigerFile circuit = circ.render();
        Tools.saveFile(input, circuit.toString());

        if (data.isOutputCircuit()) { // save as circuit
            AigerTools.saveAiger2DotAndPDF(input, output + "_circ_system", PetriNetExtensionHandler.getProcessFamilyID(net));
        }

        final String timeCommand = "/usr/bin/time";
        final String fileOutput = "-f";
        final String fileArgument = "wall_time_(s)%e\\nCPU_time_(s)%U\\nmemory_(KB)%M\\n";
        final String outputOption = "-o";
        final String outputFile = output + "_stats_time_mem.txt";

        //%%%%%%%%%%%%%%%%%% MCHyper
        String inputFile = input;
        String outputPath = output;
        McHyper.call(inputFile, formula, outputPath, data.isVerbose(), PetriNetExtensionHandler.getProcessFamilyID(net), circ.getMCHyperResultOptimizations());

        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% COLLECT STATISTICS
        if (stats != null) {
            // system size
            int nb_latches = circuit.getNbOfLatches();
            int nb_gates = circuit.getNbOfGates();
            // total size             
            int nb_total_latches = -1;
            int nb_total_gates = -1;
            try (BufferedReader mcHyperAag = new BufferedReader(new FileReader(outputPath + ".aag"))) {
                String header = mcHyperAag.readLine();
                String[] vals = header.split(" ");
                nb_total_latches = Integer.parseInt(vals[3]);
                nb_total_gates = Integer.parseInt(vals[5]);
            }
            // set the values
            stats.setSys_nb_latches(nb_latches);
            stats.setSys_nb_gates(nb_gates);
            stats.setTotal_nb_latches(nb_total_latches);
            stats.setTotal_nb_gates(nb_total_gates);
            // output the values
            if (BenchmarksMC.EDACC) {
                Logger.getInstance().addMessage("nb_places: " + stats.getIn_nb_places(), "edacc");
                Logger.getInstance().addMessage("nb_transitions: " + stats.getIn_nb_transitions(), "edacc");
                Logger.getInstance().addMessage("size_f: " + stats.getIn_size_formula(), "edacc");
                Logger.getInstance().addMessage("nb_sys_latches: " + nb_latches, "edacc");
                Logger.getInstance().addMessage("nb_sys_gates: " + nb_gates, "edacc");
                Logger.getInstance().addMessage("nb_total_latches: " + nb_total_latches, "edacc");
                Logger.getInstance().addMessage("nb_total_gates: " + nb_total_gates, "edacc");
            } else {
                Logger.getInstance().addMessage(stats.getInputSizes(), true);
                // if a file is given already write them to the file
                stats.writeInputSizesToFile();
            }
        }
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% END COLLECT STATISTICS

        if (data.isOutputCircuit()) { // save as circuit
            AigerTools.saveAiger2DotAndPDF(outputPath + ".aag", output + "_circ_all", PetriNetExtensionHandler.getProcessFamilyID(net));
        }

        // %%%%%%%%%%%%%%%% Aiger
        inputFile = outputPath + ".aag";
        outputPath = output + ".aig";
        AigToAig.call(inputFile, outputPath, data.isVerbose(), PetriNetExtensionHandler.getProcessFamilyID(net));
    }

    /**
     *
     * @param net
     * @param circ
     * @param data
     * @param formula - in MCHyper format
     * @throws InterruptedException
     * @throws IOException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public static void createCircuit(PetriNet net, AigerRenderer circ, String formula, AdamCircuitLTLMCOutputData data) throws InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        createCircuit(net, circ, formula, data, null);
    }

}
