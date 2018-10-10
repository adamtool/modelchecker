package uniolunisaar.adam.modelchecker.circuits;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerMCHyper {

    public static boolean check(PetriNet net, String formula, String path) throws InterruptedException, IOException {
        ProcessBuilder procBuilder = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + "/mchyper.py", "-f", formula, path + ".aiger", "-pdr", "-cex");
//        procBuilder.directory(new File(AdamProperties.getInstance().getLibFolder() + "/../logic/"));
//        System.out.println(procBuilder.directory());
//        procBuilder = new ProcessBuilder(System.getProperty("libfolder") + "/mchyper");
        System.out.println(procBuilder.command());
        Process proc = procBuilder.start();
        proc.waitFor();
        String error = IOUtils.toString(proc.getErrorStream());
        Logger.getInstance().addMessage(error, true);
        String output = IOUtils.toString(proc.getInputStream());
        Logger.getInstance().addMessage(output, true);
        return false;
    }

}
