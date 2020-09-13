package uniolunisaar.adam.util;

import uniol.apt.adt.IGraph;
import uniol.apt.adt.IGraphListener;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.analysis.bounded.Bounded;
import uniol.apt.analysis.bounded.BoundedResult;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 * This class is used to check and store all preconditions of a Petri net with
 * transits to be used in the model checking approach.
 *
 * This is only whether the net is 1-bounded or not.
 *
 * @author Manuel Gieseking
 */
public class PreconditionChecker implements IGraphListener<PetriNet, Flow, Node> {

    private final PetriNetWithTransits pnwt;
    private BoundedResult bounded = null;
    private boolean isChanged = true;

    public PreconditionChecker(PetriNetWithTransits pnwt) {
        this.pnwt = pnwt;
        this.pnwt.addListener(this);
    }

    public boolean check() {
        return isSafe();
    }

    public boolean isSafe() {
        if (bounded == null) {
            bounded = Bounded.checkBounded(pnwt);
        }
        return bounded.isSafe();
    }

    public boolean isIsChanged() {
        return isChanged;
    }

    @Override
    public boolean changeOccurred(IGraph<PetriNet, Flow, Node> graph) {
        bounded = null;
        isChanged = true;
        return true;
    }

}
