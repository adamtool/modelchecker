package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import uniol.apt.adt.pn.Transition;

/**
 *
 * @author Manuel Gieseking
 */
public class TransitionLabel implements ILabel {

    private final Transition t;

    public TransitionLabel(Transition t) {
        this.t = t;
    }

    public Transition getTransitiong() {
        return t;
    }

    @Override
    public String toString() {
        return t.getId();
    }
}
