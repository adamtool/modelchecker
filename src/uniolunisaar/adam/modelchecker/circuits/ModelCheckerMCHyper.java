package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.AdamProperties;
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
     * @param formula - in MCHyper format
     * @param path
     * @param previousSemantics
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static CounterExample check(PetriNet net, String formula, String path, boolean previousSemantics) throws InterruptedException, IOException {
        ModelCheckerTools.save2AigerAndPdf(net, path, previousSemantics);
        ProcessBuilder procBuilder = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + "/mchyper.py", "-f", formula, path + ".aag", "-pdr", "-cex", "-v", "2", "-o", path + "_complete");
//        procBuilder.directory(new File(AdamProperties.getInstance().getLibFolder() + "/../logic/"));
//        System.out.println(procBuilder.directory());
//        procBuilder = new ProcessBuilder(System.getProperty("libfolder") + "/mchyper");

        Logger.getInstance().addMessage(procBuilder.command().toString(), true);
        Process proc = procBuilder.start();
        proc.waitFor();
        String error = IOUtils.toString(proc.getErrorStream());
        Logger.getInstance().addMessage(error, true); // todo: print it as error and a proper exception
        String output = IOUtils.toString(proc.getInputStream());
        Logger.getInstance().addMessage(output, true);

        if (proc.exitValue() == 255) {
            throw new RuntimeException("MCHyper didn't finshed correctly.");
        }

        if (proc.exitValue() == 42) { // has a counter example, ergo read it
            return ModelCheckerTools.parseCounterExample(net, path + "_complete.cex");
        }

        return null;
    }

}
