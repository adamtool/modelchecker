package uniolunisaar.adam.logic.modelchecking.circuits;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import static uniolunisaar.adam.logic.transformers.pn2aiger.AigerRendererSafeOutStutterRegister.STUTT_LATCH;
import uniolunisaar.adam.ds.modelchecking.CounterExample;
import uniolunisaar.adam.ds.modelchecking.CounterExampleElement;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExampleParser {

    public static CounterExample parseCounterExampleStandard(PetriNet net, String path, CounterExample cex) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(path)) {
            String cexText = IOUtils.toString(inputStream);
//            Logger.getInstance().addMessage(cexText, true);
            // crop counter example
            List<String> cropped = new ArrayList<>();
            cexText = cexText.replaceAll("=0 ", "=0 \n").replaceAll("=1 ", "=1 \n");
            String[] lines = cexText.split(" \n");
            // only take the inputs and registers and the lasso latch of MCHyper 
            // and removes the only for hyperLTL relevant _0 at each identifier
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith(AigerRenderer.INPUT_PREFIX)) {
                    cropped.add(lines[i].substring(5).replace("_0@", "@"));
                } else if (lines[i].startsWith(AigerRenderer.INIT_LATCH)) {
                    cropped.add(lines[i].replace("_0@", "@"));
                } else if (lines[i].startsWith("I:remember_state")) {
                    cropped.add(lines[i]);
                } else if (lines[i].startsWith("entered_lasso")) {
                    cropped.add(lines[i]);
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
//            Logger.getInstance().addMessage(cropped.toString(), true);
            int timestep = 0;
            while (timestep >= 0) {
                CounterExampleElement cexe = new CounterExampleElement(timestep, false);
                boolean found = false;
                for (int i = 0; i < cropped.size(); i++) {
                    String elem = cropped.get(i);
                    if (elem.substring(elem.lastIndexOf('@') + 1, elem.lastIndexOf('=')).equals(String.valueOf(timestep))) {
                        elem = elem.replace("@" + timestep, "");
                        char val = elem.charAt(elem.length() - 1);
                        if (elem.startsWith(AigerRenderer.INIT_LATCH)) {
                            cexe.setInit(val == '1');
                        } else if (elem.startsWith("I:remember_state")) {
                            cexe.setStartsLoop(val == '1');
                        } else if (elem.startsWith("entered_lasso")) {
                            cexe.setLooping(val == '1');
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
            return cex;
        }
    }

    public static CounterExample parseCounterExampleWithStutteringLatch(PetriNet net, String path, CounterExample cex) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(path)) {
            String cexText = IOUtils.toString(inputStream);
//            Logger.getInstance().addMessage(cexText, true);
            // crop counter example
            List<String> cropped = new ArrayList<>();
            cexText = cexText.replaceAll("=0 ", "=0 \n").replaceAll("=1 ", "=1 \n");
            String[] lines = cexText.split(" \n");
            // only take the inputs and registers and the lasso latch of MCHyper                 
            // and removes the only for hyperLTL relevant _0 at each identifier
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith(AigerRenderer.INPUT_PREFIX)) {
                    cropped.add(lines[i].substring(5).replace("_0@", "@"));
                } else if (lines[i].startsWith(AigerRenderer.INIT_LATCH)) {
                    cropped.add(lines[i].replace("_0@", "@"));
                } else if (lines[i].startsWith(STUTT_LATCH)) {
                    cropped.add(lines[i].replace("_0@", "@"));
                } else if (lines[i].startsWith("entered_lasso")) {
                    cropped.add(lines[i]);
                } else if (lines[i].startsWith("I:remember_state")) {
                    cropped.add(lines[i]);
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
//            Logger.getInstance().addMessage(cropped.toString(), true);
            // create the counter example
            int timestep = 0;
            while (timestep >= 0) {
                CounterExampleElement cexe = new CounterExampleElement(timestep, true);
                boolean found = false;
                for (int i = 0; i < cropped.size(); i++) {
                    String elem = cropped.get(i);
                    if (elem.substring(elem.lastIndexOf('@') + 1, elem.lastIndexOf('=')).equals(String.valueOf(timestep))) {
                        elem = elem.replace("@" + timestep, "");
                        char val = elem.charAt(elem.length() - 1);
                        if (elem.startsWith(AigerRenderer.INIT_LATCH)) {
                            cexe.setInit(val == '1');
                        } else if (elem.startsWith(STUTT_LATCH)) {
                            cexe.setStutter(val == '1');
                        } else if (elem.startsWith("I:remember_state")) {
                            cexe.setStartsLoop(val == '1');
                        } else if (elem.startsWith("entered_lasso")) {
                            cexe.setLooping(val == '1');
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
            return cex;
        }
    }

}
