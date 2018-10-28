package uniolunisaar.adam.modelchecker.circuits.renderer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.modelchecker.circuits.AigerFile;
import uniolunisaar.adam.modelchecker.circuits.CounterExample;
import uniolunisaar.adam.modelchecker.circuits.CounterExampleElement;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRendererSafeOutStutterRegisterMaxInterleaving extends AigerRenderer {

    public static final String STUTT_LATCH = "#stutt#";

    public String renderToString(PetriNet net) {
        return render(net).toString();
    }

    public String renderWithSavingTransitions(PetriNet net) {
        AigerFile file = render(net);
        //%%%%%%%%%% Add the additional latches
        // the transitions (todo: save only the one relevant id)
        for (Transition t : net.getTransitions()) {
            file.addLatch(t.getId());
        }

        // %%%%%%%%%% Update additional latches
        // the transitions are the valid transitions
        for (Transition t : net.getTransitions()) {
            file.copyValues(t.getId() + NEW_VALUE_OF_LATCH_SUFFIX, VALID_TRANSITION_PREFIX + t.getId());
        }
        return file.toString();
    }

    @Override
    public AigerFile render(PetriNet net) {
        AigerFile f = super.render(net);
        updateStuttering(f, net);
        return f;
    }

    @Override
    void addLatches(AigerFile file, PetriNet net) {
        super.addLatches(file, net);
        // add the stuttering latch
        file.addLatch(STUTT_LATCH);
    }

    @Override
    void addOutputs(AigerFile file, PetriNet net) {
        super.addOutputs(file, net);
        // add the init latch as output
        file.addOutput(OUTPUT_PREFIX + INIT_LATCH);
        // add the stuttering latch as output
        file.addOutput(OUTPUT_PREFIX + STUTT_LATCH);
    }

    @Override
    void setOutputs(AigerFile file, PetriNet net) {
        // init latch is the old value
        file.copyValues(OUTPUT_PREFIX + INIT_LATCH, INIT_LATCH);
        // for stuttering it is the old value
        file.copyValues(OUTPUT_PREFIX + STUTT_LATCH, STUTT_LATCH);
        // the valid transitions are already the output (initially it is not important what the output is)
        for (Transition t : net.getTransitions()) {
            file.copyValues(OUTPUT_PREFIX + t.getId(), VALID_TRANSITION_PREFIX + t.getId());
        }
        // if it is not the initial step
        // the place outputs are the saved output of the place latches
        // otherwise it is the new value of the places
        for (Place p : net.getPlaces()) {
            file.addGate(OUTPUT_PREFIX + p.getId() + "_bufA", "!" + INIT_LATCH, "!" + p.getId() + NEW_VALUE_OF_LATCH_SUFFIX);
            file.addGate(OUTPUT_PREFIX + p.getId() + "_bufB", INIT_LATCH, "!" + p.getId());
            file.addGate(OUTPUT_PREFIX + p.getId(), "!" + OUTPUT_PREFIX + p.getId() + "_bufA", "!" + OUTPUT_PREFIX + p.getId() + "_bufB");
        }
    }

    void updateStuttering(AigerFile file, PetriNet net) {
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            inputs[i++] = "!" + ENABLED_PREFIX + t.getId();
        }
        file.addGate(STUTT_LATCH + "_buf", inputs);
        file.addGate(STUTT_LATCH + NEW_VALUE_OF_LATCH_SUFFIX, ALL_TRANS_NOT_TRUE, "!" + STUTT_LATCH + "_buf");
    }

    @Override
    public CounterExample parseCounterExample(PetriNet net, String path, CounterExample cex) throws IOException {
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
//                Logger.getInstance().addMessage(cropped.toString(), true);
            // create the counter example
            int timestep = 0;
            while (timestep >= 0) {
                CounterExampleElement cexe = new CounterExampleElement(timestep, true);
                boolean found = false;
                for (int i = 0; i < cropped.size(); i++) {
                    String elem = cropped.get(i);
                    if (elem.contains("@" + timestep)) {
                        elem = elem.replace("@" + timestep, "");
                        char val = elem.charAt(elem.length() - 1);
                        if (elem.startsWith(AigerRenderer.INIT_LATCH)) {
                            cexe.setInit(val == '1');
                        } else if (elem.startsWith(STUTT_LATCH)) {
                            cexe.setStutter(val == '1');
                        } else if (elem.startsWith("entered_lasso")) {
                            cexe.setStartsLoop(val == '1');
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
