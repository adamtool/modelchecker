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

    public String getId() {
        return t == null ? "-" : t.getId();
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (t != null ? Objects.hashCode(this.t.getId()) : 0);
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
        if (this.t == null && other.t != null) {
            return false;
        }
        if (this.t != null && other.t == null) {
            return false;
        }
        if (this.t == null && other.t == null) {
            return true;
        }
        if (!Objects.equals(this.t.getId(), other.t.getId())) {
            return false;
        }
        return true;
    }

}
