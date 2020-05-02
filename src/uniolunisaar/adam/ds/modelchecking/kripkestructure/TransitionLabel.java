package uniolunisaar.adam.ds.modelchecking.kripkestructure;

import java.util.Objects;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.automata.ILabel;

/**
 *
 * @author Manuel Gieseking
 */
public class TransitionLabel implements ILabel {

    private final Transition t;

    public TransitionLabel(Transition t) {
        this.t = t;
    }

    public Transition getTransition() {
        return t;
    }

    @Override
    public String toString() {
        return t.getId();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.t.getId());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransitionLabel other = (TransitionLabel) obj;
        if (!Objects.equals(this.t.getId(), other.t.getId())) {
            return false;
        }
        return true;
    }
    
    
}
