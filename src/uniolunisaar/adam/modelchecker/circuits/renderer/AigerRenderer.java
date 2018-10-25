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
public class AigerRenderer {

    public static final String INIT_LATCH = "#initLatch#";
    public static final String INPUT_PREFIX = "#in#_";
    public static final String OUTPUT_PREFIX = "#out#_";
    public static final String NEW_VALUE_OF_LATCH_SUFFIX = "_#new#";
    public static final String ENABLED_PREFIX = "#enabled#_";
    public static final String VALID_TRANSITION_PREFIX = "#chosen#_";
    public static final String ALL_TRANS_NOT_TRUE = "#allTransitionsNotTrue#";
    public static final String SUCCESSOR_REGISTER_PREFIX = "#succReg#_";
    public static final String SUCCESSOR_PREFIX = "#succ#_";

    /**
     * Adds inputs for all transitions.
     *
     * @param file
     * @param net
     */
    void addInputs(AigerFile file, PetriNet net) {
        // Add an input for all transitions
        for (Transition t : net.getTransitions()) {
            file.addInput(INPUT_PREFIX + t.getId());
        }
    }

    /**
     * Adds latches for the init latch and all places
     *
     * @param file
     * @param net
     */
    void addLatches(AigerFile file, PetriNet net) {
        // Add the latches
        // initialization latch
        file.addLatch(INIT_LATCH);
        // places
        for (Place p : net.getPlaces()) {
            file.addLatch(p.getId());
        }
    }

    /**
     * Adds the outputs for the places and transitions.
     *
     * @param file
     * @param net
     */
    void addOutputs(AigerFile file, PetriNet net) {
        //Add outputs
        // for the places
        for (Place p : net.getPlaces()) {
            file.addOutput(OUTPUT_PREFIX + p.getId());
        }
        // and the transitions
        for (Transition t : net.getTransitions()) {
            file.addOutput(OUTPUT_PREFIX + t.getId());
        }
    }

    private String addEnabled(AigerFile file, Transition t) {
        String outId = ENABLED_PREFIX + t.getId();
        if (t.getPreset().size() == 1) {
            Place p = t.getPreset().iterator().next();
            file.copyValues(outId, p.getId());
            return outId;
        }
        String[] inputs = new String[t.getPreset().size()];
        int i = 0;
        for (Place p : t.getPreset()) {
            inputs[i++] = p.getId();
        }
        file.addGate(outId, inputs);
        return outId;
    }

    void addEnablednessOfTransitions(AigerFile file, PetriNet net) {
        // Create the general circuits for getting the enabledness of a transition
        for (Transition t : net.getTransitions()) {
            addEnabled(file, t);
        }
    }

    void addChosingOfValidTransitions(AigerFile file, PetriNet net) {
        //%%%%%%%%%%%%% Create the output for the transitions
        // Choose that only one transition at a time can be fired
        //
        // todo: add other semantics (choose every not conflicting transition)
        // or put this choosing in the formula
        //
        // The transition is only choosen if it is enabled
        for (Transition t1 : net.getTransitions()) {
            String[] inputs = new String[net.getTransitions().size() + 1];
            int i = 0;
            inputs[i++] = INPUT_PREFIX + t1.getId();
            for (Transition t2 : net.getTransitions()) {
                if (!t1.getId().equals(t2.getId())) {
                    inputs[i++] = "!" + INPUT_PREFIX + t2.getId();
                }
            }
            inputs[i++] = ENABLED_PREFIX + t1.getId(); // this one added to have only the enabled choosen
            file.addGate(VALID_TRANSITION_PREFIX + t1.getId(), inputs);
        }
    }

    void addUpdateInitLatch(AigerFile file, PetriNet net) {
        // Update the init flag just means set it to true
        file.copyValues(INIT_LATCH + NEW_VALUE_OF_LATCH_SUFFIX, AigerFile.TRUE);
    }

    private void addNegationOfAllTransitions(AigerFile file, PetriNet net) {
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            inputs[i++] = "!" + VALID_TRANSITION_PREFIX + t.getId();
        }
        file.addGate(ALL_TRANS_NOT_TRUE, inputs);
    }

    private String createSuccessorRegister(AigerFile file, PetriNet net, Place p) {
        String id = SUCCESSOR_REGISTER_PREFIX + p.getId();
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            String firingResult;
            if (!t.getPreset().contains(p) && !t.getPostset().contains(p)) {
                firingResult = "!" + p.getId();
            } else if (t.getPreset().contains(p) && !t.getPostset().contains(p)) {
                firingResult = AigerFile.TRUE;
            } else {
                firingResult = AigerFile.FALSE;
            }
            file.addGate(id + "_" + t.getId() + "_buf", VALID_TRANSITION_PREFIX + t.getId(), firingResult);
            inputs[i++] = "!" + id + "_" + t.getId() + "_buf";
        }
        file.addGate(id, inputs);
        return id;
    }

    private String createSuccessor(AigerFile file, Place p) {
        String id = SUCCESSOR_PREFIX + p.getId();
        // create A
        String idA = id + "_A";
        file.addGate(idA, ALL_TRANS_NOT_TRUE, "!" + p.getId());
        // create B
        String idB = id + "_B";
        file.addGate(idB, "!" + ALL_TRANS_NOT_TRUE, "!" + SUCCESSOR_REGISTER_PREFIX + p.getId());
        // total
        file.addGate(id, "!" + idA, "!" + idB);
        return id;
    }

    void addSuccessors(AigerFile file, PetriNet net) {
        // needed for createSuccessor
        addNegationOfAllTransitions(file, net);

        // Create for each place the chosing and the test if s.th. has fired
        for (Place p : net.getPlaces()) {
            // Create for each place the choosing of the transition
//            createChooseTransition(file, net, p); // use this when not already checked that the transition is enabled
            createSuccessorRegister(file, net, p); // F2
            // Create for each place the check if s.th. has fired
            createSuccessor(file, p); // F1
        }

        // Do the final update for the places
        for (Place p : net.getPlaces()) {
            if (p.getInitialToken().getValue() > 0) { // is initial place
                // !(!init_latch AND !F)
                file.addGate(p.getId() + "_new_buf", INIT_LATCH, "!" + SUCCESSOR_PREFIX + p.getId());
                file.copyValues(p.getId() + NEW_VALUE_OF_LATCH_SUFFIX, "!" + p.getId() + "_new_buf");
            } else {
                file.addGate(p.getId() + NEW_VALUE_OF_LATCH_SUFFIX, INIT_LATCH, SUCCESSOR_PREFIX + p.getId());
            }
        }
    }

    void setOutputs(AigerFile file, PetriNet net) {
        // the valid transitions are already the output in the case that it is not init
        for (Transition t : net.getTransitions()) {
            file.addGate(OUTPUT_PREFIX + t.getId(), INIT_LATCH, VALID_TRANSITION_PREFIX + t.getId());
//            file.copyValues(OUTPUT_PREFIX + t.getId(), VALID_TRANSITION_PREFIX + t.getId());
        }
        // the place outputs are directly the output of the place latches
        for (Place p : net.getPlaces()) {
            file.copyValues(OUTPUT_PREFIX + p.getId(), p.getId() + NEW_VALUE_OF_LATCH_SUFFIX);
        }
    }

    public AigerFile render(PetriNet net) {
        AigerFile file = new AigerFile();
        //%%%%%%%%% Add inputs -> all transitions
        addInputs(file, net);
        //%%%%%%%%%% Add the latches -> init + all places
        addLatches(file, net);
        //%%%%%%%%% Add outputs -> all places and transitions
        addOutputs(file, net);

        //%%%%%%%%%%%%% Create the output for the transitions
        // Create the general circuits for getting the enabledness of a transition
        addEnablednessOfTransitions(file, net);

        // Choose that only one transition at a time can be fired
        //
        // todo: add other semantics (choose every not conflicting transition)
        // or put this choosing in the formula
        //
        // The transition is only choosen if it is enabled
        addChosingOfValidTransitions(file, net);

        // %%%%%%%%%% Update the latches
        // the init flag
        addUpdateInitLatch(file, net);
        // the places
        addSuccessors(file, net);

        // %%%%%%%%% Set the outputs
        setOutputs(file, net);

        return file;
    }

    /**
     * Not needed in the situation when we already only chosed enabled
     * transitions
     *
     * @param file
     * @param net
     * @return
     * @deprecated
     */
    @Deprecated
    void addFiring(AigerFile file, PetriNet net) {
        // Create for each place and each transition what happens, when "firing"
        for (Place p : net.getPlaces()) {
            for (Transition t : net.getTransitions()) {
                createDoFiring(file, p, t);
            }
        }
    }

    /**
     * Used if the enabledness is checked directly here and not at the beginning
     *
     * @param file
     * @param p
     * @param t
     * @return
     * @deprecated
     */
    @Deprecated
    private String createDoFiring(AigerFile file, Place p, Transition t) {
        String id = p.getId() + "_" + t.getId() + "_fired";
        if (!t.getPreset().contains(p) && !t.getPostset().contains(p)) {
            file.copyValues(id, p.getId());
        } else if (t.getPreset().contains(p) && !t.getPostset().contains(p)) {
            file.addGate(id, "!" + ENABLED_PREFIX + t.getId(), p.getId());
        } else {
            file.addGate(id + "_buf", "!" + ENABLED_PREFIX + t.getId(), "!" + p.getId());
            file.copyValues(id, "!" + id + "_buf");
        }
        return id;
    }

    /**
     * Used if the enabledness is checked directly here and not at the beginning
     *
     * @param file
     * @param net
     * @param p
     * @return
     */
    @Deprecated
    private String createChooseTransition(AigerFile file, PetriNet net, Place p) {
        String id = "#transChoosen#_" + p.getId();
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            file.addGate(id + "_" + t.getId() + "_buf", VALID_TRANSITION_PREFIX + t.getId(), "!" + p.getId() + "_" + t.getId() + "_fired");
            inputs[i++] = "!" + id + "_" + t.getId() + "_buf";
        }
        file.addGate(id, inputs);
        return id;
    }
    
      public CounterExample parseCounterExample(PetriNet net, String path) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(path)) {
            String cexText = IOUtils.toString(inputStream);
//            Logger.getInstance().addMessage(cexText, true);
            // crop counter example
            List<String> cropped = new ArrayList<>();
            String[] lines = cexText.split(" \n");
            // only take the inputs and registers                
            // and removes the only for hyperLTL relevant _0 at each identifier
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].startsWith(AigerRenderer.INPUT_PREFIX)) {
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
//                Logger.getInstance().addMessage(cropped.toString(), true);
            // create the counter example
            CounterExample cex = new CounterExample();
            int timestep = 0;
            while (timestep >= 0) {
                CounterExampleElement cexe = new CounterExampleElement(timestep, false);
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
            return cex;
        }
    }
}
