package uniolunisaar.adam.modelchecker.circuits;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRenderer {

    private static final String INIT_LATCH = "#initLatch#";

    public static String render(PetriNet net) {
        AigerFile file = new AigerFile();
        // Add inputs
        for (Transition t : net.getTransitions()) {
            file.addInput("in_" + t.getId());
        }
        // Add the latches
        // initialization latch
        file.addLatch(INIT_LATCH);
        // places
        for (Place p : net.getPlaces()) {
            file.addLatch(p.getId());
        }
//        for (Transition t : net.getTransitions()) {
//            file.addLatch(t.getId());
//        }
        // Add outputs
        for (Place p : net.getPlaces()) {
            file.addOutput("out_" + p.getId());
        }
        for (Transition t : net.getTransitions()) {
            file.addOutput("out_" + t.getId());
        }

        // Choose that only one transition at a time can be fired
        // todo: add other semantics (choose every not conflicting transition)
        // or put this choosing in the formula
        for (Transition t1 : net.getTransitions()) {
            String[] inputs = new String[net.getTransitions().size()];
            int i = 0;
            inputs[i++] = "in_" + t1.getId();
            for (Transition t2 : net.getTransitions()) {
                if (!t1.getId().equals(t2.getId())) {
                    inputs[i++] = "!in_" + t2.getId();
                }
            }
            file.addGate("out_" + t1.getId(), inputs);
        }

        // Update the init flag
        file.copyValues(INIT_LATCH + "_new", AigerFile.TRUE);

        // Update the place latches
        // Create the general circuits for getting the enabledness of a transition
        for (Transition t : net.getTransitions()) {
            createEnabled(file, t);
        }
        // Create for each place and each transition what happens, when "firing"
        for (Place p : net.getPlaces()) {
            for (Transition t : net.getTransitions()) {
                createDoFiring(file, p, t);
            }
        }
        // Create for each place the chosing and the test if s.th. has fired
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            inputs[i++] = "!out_" + t.getId();
        }
        file.addGate("#allNegatedTransitions#", inputs);
        for (Place p : net.getPlaces()) {
            // Create for each place the choosing of the transition
            createChooseTransition(file, net, p);
            // Create for each place the check if s.th. has fired
            createSthFired(file, net, p);
        }
        // Do the final update for the places
        for (Place p : net.getPlaces()) {
            if (p.getInitialToken().getValue() > 0) { // is initial place
                // !(!init_latch AND !F)
                file.addGate(p.getId() + "_new_buf", "!" + INIT_LATCH, "!" + "#sthFired#_" + p.getId());
                file.copyValues(p.getId() + "_new", "!" + p.getId() + "_new_buf");
            } else {
                file.addGate(p.getId() + "_new", "!" + INIT_LATCH, "#sthFired#_" + p.getId());
            }
        }

        // Set the outputs
        // the place outputs are directly the output of the latches
        for (Place p : net.getPlaces()) {
            file.copyValues("out_" + p.getId(), p.getId() + "_new");
        }
        // for the transition output this is already done by the correcting 
        // choosing strategy
//        // copy input transitions directly to the output
//        for (Transition t : net.getTransitions()) {
//            file.copyValues("out_" + t.getId(), t.getId());
//        }

//        // copy the latches
//        for (Place p : net.getPlaces()) {
//            file.copyValues(p.getId() + "_new", p.getId());
//        }
//        for (Transition t : net.getTransitions()) {
//            file.copyValues(t.getId() + "_new", t.getId());
//        }
//        // copy input transitions directly to the output
//        for (Transition t : net.getTransitions()) {
//            file.copyValues("out_" + t.getId(), t.getId());
//        }
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

    private static String createChooseTransition(AigerFile file, PetriNet net, Place p) {
        String id = "#transChoosen#_" + p.getId();
        String[] inputs = new String[net.getTransitions().size()];
        int i = 0;
        for (Transition t : net.getTransitions()) {
            file.addGate(id + "_" + t.getId() + "_buf", "out_" + t.getId(), "!" + p.getId() + "_" + t.getId() + "_fired");
            inputs[i++] = "!" + id + "_" + t.getId() + "_buf";
        }
        file.addGate(id, inputs);
        return id;
    }

    private static String createSthFired(AigerFile file, PetriNet net, Place p) {
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
