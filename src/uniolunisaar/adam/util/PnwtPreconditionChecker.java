package uniolunisaar.adam.util;

import uniol.apt.adt.IGraph;
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
public class PnwtPreconditionChecker extends PreconditionChecker {

    private BoundedResult bounded = null;

    public PnwtPreconditionChecker(PetriNetWithTransits pnwt) {
        super(pnwt);
    }

    @Override
    public boolean check() {
        return isSafe();
    }

    public boolean isSafe() {
        if (bounded == null) {
            bounded = Bounded.checkBounded(getNet());
        }
        return bounded.isSafe();
    }

    @Override
    public boolean changeOccurred(IGraph<PetriNet, Flow, Node> graph) {
        super.changeOccurred(graph);
        bounded = null;
        return true;
    }

}
