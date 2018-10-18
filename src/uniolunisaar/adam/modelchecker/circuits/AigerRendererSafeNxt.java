package uniolunisaar.adam.modelchecker.circuits;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRendererSafeNxt extends AigerRenderer {

    public String renderToString(PetriNet net) {
        return super.render(net).toString();
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
    void setOutputs(AigerFile file, PetriNet net) {
        // the valid transitions are already the output
        for (Transition t : net.getTransitions()) {
            file.copyValues(OUTPUT_PREFIX + t.getId(), VALID_TRANSITION_PREFIX + t.getId());
        }
        // the place outputs are the save output of the place latches
        for (Place p : net.getPlaces()) {
            file.copyValues(OUTPUT_PREFIX + p.getId(), p.getId());
        }
    }

}
