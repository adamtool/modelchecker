package uniolunisaar.adam.modelchecker.circuits;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerMCHyper {
    
    
    public static boolean check(PetriNet net, String formula, String path) throws InterruptedException, IOException {
        ModelCheckerTools.save2AigerAndPdf(net, path);
        ProcessBuilder procBuilder = new ProcessBuilder(AdamProperties.getInstance().getLibFolder() + "/mchyper.py", "-f", formula, path + ".aag", "-pdr", "-cex", "-v", "2", "-o", path + "_complete");
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

        if (proc.exitValue() == 255) {
            throw new RuntimeException("MCHyper didn't finshed correctly.");
        }

        if (proc.exitValue() == 42) { // has a counter example, ergo read it
            try (FileInputStream inputStream = new FileInputStream(path + "_complete.cex")) {
                String cexText = IOUtils.toString(inputStream);
                Logger.getInstance().addMessage(cexText, true);
                // crop counter example
                List<String> cropped = new ArrayList<>();
                String[] lines = cexText.split(" \n");
                // only take the inputs and registers                
                // and removes the only for hyperLTL relevant _0 at each identifier
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].startsWith("#in#_")) {
                        cropped.add(lines[i].substring(5).replace("_0@", "@"));
                    } else if (lines[i].startsWith(AigerRenderer.INIT_LATCH)) {
                        cropped.add(lines[i].replace("_0@", "@"));
                    } else {
                        for (Place p : net.getPlaces()) {
                            int idx = lines[i].lastIndexOf("_0@");
                            if (idx != -1) {
                                String id = lines[i].substring(0, idx);
                                if (id.equals(p.getId())) {
                                    cropped.add(lines[i].replace("_0@", "@"));
                                }
                            }
                        }
                    }
                }
                Logger.getInstance().addMessage(cropped.toString(), true);
                // create the counter example
                CounterExample cex = new CounterExample();
                int timestep = 0;
                while (timestep >= 0) {
                    CounterExampleElement cexe = new CounterExampleElement(timestep);
                    boolean found = false;
                    for (int i = 0; i < cropped.size(); i++) {
                        String elem = cropped.get(i);
                        if (elem.contains("@" + timestep)) {
                            elem = elem.replace("@" + timestep, "");
                            char val = elem.charAt(elem.length() - 1);
                            if (elem.startsWith(AigerRenderer.INIT_LATCH)) {
                                cexe.setInit(val == '1');
                            } else {
                                String id = elem.substring(0, elem.length() - 2);
                                if (val == '1') {
                                    if (net.containsPlace(id)) {
                                        cexe.add(net.getPlace(id));
                                    } else if (net.containsTransition(id)) {
                                        cexe.add(net.getTransition(id));
                                    }
                                }
                            }
                            found = true;
                        }
                    }
                    if (!found) {
                        timestep = -1;
                    } else {
                        cex.addTimeStep(cexe);
                        ++timestep;
                    }
                }
                Logger.getInstance().addMessage(cex.toString(), true);
            }
            return false;
        }

        return true;
    }

}
