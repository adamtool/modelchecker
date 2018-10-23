package uniolunisaar.adam.modelchecker.circuits;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRendererSafeOutStutterRegister extends AigerRenderer {

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
            inputs[i++] = "!" + INPUT_PREFIX + t.getId();
        }
        file.addGate(STUTT_LATCH + "_buf", inputs);
        file.addGate(STUTT_LATCH + NEW_VALUE_OF_LATCH_SUFFIX, ALL_TRANS_NOT_TRUE, "!" + STUTT_LATCH + "_buf");
    }

}
