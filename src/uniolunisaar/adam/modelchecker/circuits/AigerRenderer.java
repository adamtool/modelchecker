package uniolunisaar.adam.modelchecker.circuits;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRenderer {

    static final String INIT_LATCH = "#initLatch#";

    public static String render(PetriNet net) {
        AigerFile file = new AigerFile();
        //%%%%%%%%% Add inputs
        for (Transition t : net.getTransitions()) {
            file.addInput("#in#_" + t.getId());
        }
        //%%%%%%%%%% Add the latches
        // initialization latch
        file.addLatch(INIT_LATCH);
        // places
        for (Place p : net.getPlaces()) {
            file.addLatch(p.getId());
        }
//        for (Transition t : net.getTransitions()) {
//            file.addLatch(t.getId());
//        }
        //%%%%%%%%% Add outputs
        for (Place p : net.getPlaces()) {
            file.addOutput("#out#_" + p.getId());
        }
        for (Transition t : net.getTransitions()) {
            file.addOutput("#out#_" + t.getId());
        }

        //%%%%%%%%%%%%% Create the output for the transitions
        // Create the general circuits for getting the enabledness of a transition
        for (Transition t : net.getTransitions()) {
            createEnabled(file, t);
        }

        // Choose that only one transition at a time can be fired
        //
        // todo: add other semantics (choose every not conflicting transition)
        // or put this choosing in the formula
        //
        // The transition is only choosen if it is enabled
        for (Transition t1 : net.getTransitions()) {
            String[] inputs = new String[net.getTransitions().size() + 1];
            int i = 0;
            inputs[i++] = "#in#_" + t1.getId();
            for (Transition t2 : net.getTransitions()) {
                if (!t1.getId().equals(t2.getId())) {
                    inputs[i++] = "!#in#_" + t2.getId();
                }
            }
            inputs[i++] = t1.getId() + "_enabled"; // this one added to have only the enabled choosen
            file.addGate("#out#_" + t1.getId(), inputs);
        }

        // %%%%%%%%%% Update the init flag
        file.copyValues(INIT_LATCH + "_new", AigerFile.TRUE);

        // %%%%%%%%%% Update the place latches      
        // Not needed in the situation when we already only chosed enabled transitions
//        // Create for each place and each transition what happens, when "firing"
//        for (Place p : net.getPlaces()) {
//            for (Transition t : net.getTransitions()) {
//                createDoFiring(file, p, t);
//            }
//        }
        // Create for each place the chosing and the test if s.th. has fired
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            inputs[i++] = "!#out#_" + t.getId();
        }
        file.addGate("#allNegatedTransitions#", inputs);

        for (Place p : net.getPlaces()) {
            // Create for each place the choosing of the transition
//            createChooseTransition(file, net, p); // use this when not already checked that the transition is enabled
            createChooseTransitionOfEnabled(file, net, p); // F2
            // Create for each place the check if s.th. has fired
            createSthFired(file, p); // F1
        }
        // Do the final update for the places
        for (Place p : net.getPlaces()) {
            if (p.getInitialToken().getValue() > 0) { // is initial place
                // !(!init_latch AND !F)
                file.addGate(p.getId() + "_new_buf", INIT_LATCH, "!" + "#sthFired#_" + p.getId());
                file.copyValues(p.getId() + "_new", "!" + p.getId() + "_new_buf");
            } else {
                file.addGate(p.getId() + "_new", INIT_LATCH, "#sthFired#_" + p.getId());
            }
        }

        // Set the outputs
        // the place outputs are directly the output of the latches
        for (Place p : net.getPlaces()) {
            file.copyValues("#out#_" + p.getId(), p.getId() + "_new");
        }

        return file.toString();
    }

    private static String createEnabled(AigerFile file, Transition t) {
        String outId = t.getId() + "_enabled";
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
    private static String createDoFiring(AigerFile file, Place p, Transition t) {
        String id = p.getId() + "_" + t.getId() + "_fired";
        if (!t.getPreset().contains(p) && !t.getPostset().contains(p)) {
            file.copyValues(id, p.getId());
        } else if (t.getPreset().contains(p) && !t.getPostset().contains(p)) {
            file.addGate(id, "!" + t.getId() + "_enabled", p.getId());
        } else {
            file.addGate(id + "_buf", "!" + t.getId() + "_enabled", "!" + p.getId());
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
    private static String createChooseTransition(AigerFile file, PetriNet net, Place p) {
        String id = "#transChoosen#_" + p.getId();
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            file.addGate(id + "_" + t.getId() + "_buf", "#out#_" + t.getId(), "!" + p.getId() + "_" + t.getId() + "_fired");
            inputs[i++] = "!" + id + "_" + t.getId() + "_buf";
        }
        file.addGate(id, inputs);
        return id;
    }

    private static String createChooseTransitionOfEnabled(AigerFile file, PetriNet net, Place p) {
        String id = "#transChoosen#_" + p.getId();
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
            if (net.getTransitions().size() == 1) {
                file.addGate(id, "#out#_" + t.getId(), firingResult);
            } else {
                file.addGate(id + "_" + t.getId() + "_buf", "#out#_" + t.getId(), firingResult);
                inputs[i++] = "!" + id + "_" + t.getId() + "_buf";
            }
        }
        if (net.getTransitions().size() > 1) {
            file.addGate(id, inputs);
        }
        return id;
    }

    private static String createSthFired(AigerFile file, Place p) {
        String id = "#sthFired#_" + p.getId();
        // create A
        String idA = id + "_A";
        file.addGate(idA, "#allNegatedTransitions#", "!" + p.getId());
        // create B
        String idB = id + "_B";
        file.addGate(idB, "!#allNegatedTransitions#", "!#transChoosen#_" + p.getId());
        // total
        file.addGate(id, "!" + idA, "!" + idB);
        return id;
    }

}
