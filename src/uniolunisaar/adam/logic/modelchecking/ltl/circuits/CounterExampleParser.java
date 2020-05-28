package uniolunisaar.adam.logic.modelchecking.ltl.circuits;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.circuits.CircuitRendererSettings;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import static uniolunisaar.adam.logic.transformers.pn2aiger.AigerRendererSafeStutterRegister.STUTT_LATCH;
import uniolunisaar.adam.ds.modelchecking.cex.CounterExample;
import uniolunisaar.adam.ds.modelchecking.cex.CounterExampleElement;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AbcSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitMCSettings;
import uniolunisaar.adam.ds.modelchecking.statistics.AdamCircuitLTLMCStatistics;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRendererSafeStutterRegisterLogTrans;
import uniolunisaar.adam.tools.IOUtils;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class CounterExampleParser {

    @Deprecated
    public static CounterExample parseCounterExampleStandard(PetriNet net, String path, CounterExample cex) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(path)) {
            boolean isParallel = net.hasExtension("parallel"); // todo: quick hack to have it properly printed
            String cexText = IOUtils.streamToString(inputStream);
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
                CounterExampleElement cexe = new CounterExampleElement(timestep, false, isParallel);
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

    public static CounterExample parseCounterExampleWithStutteringLatch(AdamCircuitMCSettings<? extends AdamCircuitLTLMCOutputData, ? extends AdamCircuitLTLMCStatistics> settings, String path, CounterExample cex) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(path)) {
            AbcSettings abcSettings = settings.getAbcSettings();
            PetriNet net = abcSettings.getNet();
            boolean isParallel = net.hasExtension("parallel"); // todo: quick hack to have it properly printed
            cex.setIsDetailed(abcSettings.isDetailedCEX());
            // start binary coding
            // if it is binary coded get the number of digits
            int digits = Integer.toBinaryString(net.getTransitions().size() - 1).length();
            // create a map of binID -> transition
            Map<String, Transition> codingMap = new HashMap<>();
            for (Transition transition : net.getTransitions()) {
                if (PetriNetExtensionHandler.hasBinID(transition)) {
                    codingMap.put(PetriNetExtensionHandler.getBinID(transition), transition);
                }
            }
            cex.setCodingMap(codingMap);
            // end for binary coding
            String cexText = IOUtils.streamToString(inputStream);
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
            Logger.getInstance().addMessage(cropped.toString(), false);
//            System.out.println(cropped.toString());
            // create the counter example
            int timestep = 0;
            while (timestep >= 0) {
                CounterExampleElement cexe = new CounterExampleElement(timestep, true, digits, isParallel);
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
                            // for outgoing jump over the first step
                            if (settings.getRendererSettings().getSemantics() == CircuitRendererSettings.TransitionSemantics.INGOING || timestep != 0) {
                                String id = elem.substring(0, elem.length() - 2);
//                                System.out.println(id + "=" + val);
                                if (id.startsWith(AigerRendererSafeStutterRegisterLogTrans.BIN_COD_ID)) {
                                    String binIDChar = id.substring((AigerRendererSafeStutterRegisterLogTrans.BIN_COD_ID).length());
                                    cexe.add(Integer.parseInt(binIDChar), val);
                                }
                                if (val == '1') {
                                    if (net.containsPlace(id)) {
                                        Place place = net.getPlace(id);
                                        if (abcSettings.isDetailedCEX() || PetriNetExtensionHandler.isOriginal(place)) {
                                            cexe.add(place);
                                        }
                                    } else if (net.containsTransition(id)) {
                                        Transition t = net.getTransition(id);
                                        if (abcSettings.isDetailedCEX() || PetriNetExtensionHandler.isOriginal(t) || isParallel) {
                                            cexe.add(t);
                                        }
                                    }
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
