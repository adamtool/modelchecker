package uniolunisaar.adam.modelchecker.circuits;

import uniolunisaar.adam.modelchecker.circuits.renderer.AigerRenderer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.ExternalProcessHandler;
import uniolunisaar.adam.tools.Logger;

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
    private static CounterExample checkSeparate(PetriNet net, AigerRenderer circ, String formula, String path) throws InterruptedException, IOException {
        ModelCheckerTools.save2AigerAndPdf(net, circ, path);
        ProcessBuilder procBuilder = new ProcessBuilder(ModelCheckerTools.getMCHyperPath(), path + ".aag", formula, path + "_mcHyperOut");
//        procBuilder.directory(new File(AdamProperties.getInstance().getLibFolder() + "/../logic/"));
//        System.out.println(procBuilder.directory());
//        procBuilder = new ProcessBuilder(System.getProperty("libfolder") + "/mchyper");

        Logger.getInstance().addMessage(procBuilder.command().toString(), true);
        Process proc = procBuilder.start();
        // buffering the output and error as it comes
        try (BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = is.readLine()) != null) {
                Logger.getInstance().addMessage("[MCHyper-Out]: " + line, true);
            }
        }
        try (BufferedReader is = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
            String line;
            while ((line = is.readLine()) != null) {
                Logger.getInstance().addMessage("[MCHyper-ERROR]: " + line, true);
            }
        }
        // buffering in total
//        String error = IOUtils.toString(proc.getErrorStream());
//        Logger.getInstance().addMessage(error, true); // todo: print it as error and a proper exception
//        String output = IOUtils.toString(proc.getInputStream());
//        Logger.getInstance().addMessage(output, true);
        proc.waitFor();

        if (proc.exitValue() == 255) {
            throw new RuntimeException("MCHyper didn't finshed correctly.");
        }

        if (proc.exitValue() == 42) { // has a counter example, ergo read it
            return circ.parseCounterExample(net, path + "_complete.cex");
        }

        return null;
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
    public static CounterExample check(PetriNet net, AigerRenderer circ, String formula, String path) throws InterruptedException, IOException {
        return checkWithPythonScript(net, circ, formula, path);
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
    private static CounterExample checkWithPythonScript(PetriNet net, AigerRenderer circ, String formula, String path) throws InterruptedException, IOException {
        ModelCheckerTools.save2AigerAndPdf(net, circ, path);
        // version without threads
//        ProcessBuilder procBuilder = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + "/mchyper.py", "-f", formula, path + ".aag", "-pdr", "-cex", "-v", "1", "-o", path + "_complete");
//        procBuilder.directory(new File(AdamProperties.getInstance().getLibFolder() + "/../logic/"));
//        System.out.println(procBuilder.directory());
//        procBuilder = new ProcessBuilder(System.getProperty("libfolder") + "/mchyper");

//        Logger.getInstance().addMessage(procBuilder.command().toString(), true);
//        Process proc = procBuilder.start();
//        // buffering the output and error as it comes
//        try (BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
//            String line;
//            while ((line = is.readLine()) != null) {
//                Logger.getInstance().addMessage(line, true);
//            }
//        }
//        try (BufferedReader is = new BufferedReader(new InputStreamReader(proc.getErrorStream()))) {
//            String line;
//            while ((line = is.readLine()) != null) {
//                Logger.getInstance().addMessage(line, true);
//            }
//        }
//        // buffering in total
////        String error = IOUtils.toString(proc.getErrorStream());
////        Logger.getInstance().addMessage(error, true); // todo: print it as error and a proper exception
////        String output = IOUtils.toString(proc.getInputStream());
////        Logger.getInstance().addMessage(output, true);
//        proc.waitFor();
        String[] command = {AdamProperties.getInstance().getLibFolder() + "/mchyper.py", "-f", formula, path + ".aag", "-pdr", "-cex", "-v", "1", "-o", path + "_complete"};
        Logger.getInstance().addMessage(Arrays.toString(command), true);
        ExternalProcessHandler proc = new ExternalProcessHandler(true, command);
        int exitValue = proc.startAndWaitFor(true);

        if (exitValue == 255) {
            throw new RuntimeException("MCHyper didn't finshed correctly.");
        }

        if (exitValue == 42) { // has a counter example, ergo read it
            return circ.parseCounterExample(net, path + "_complete.cex");
        }

        return null;
    }

}
