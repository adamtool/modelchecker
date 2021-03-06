package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.kripkestructure.KripkeState;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.kripkestructure.TransitionLabel;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;

/**
 * This transformer also adds an additional initial state and loops for every
 * transition not transiting the token chain.
 * 
 * This class corresponds to the outgoing semantics.
 * The outgoing semantics makes it difficult to properly handle the branching
 * because every state branches with the same transition into several branches,
 * another one for each different choice of the next outgoing transition.
 * For the ABA construction we want to collect all equally labeled transitions
 * together and treat those as the proper branching (because this is also the
 * branching in the transition which we really want to consider).
 * The solution for handling this is way more complicated, this is why we 
 * didn't introduce a reduction method into an ABA for this construction.
 * 
 *
 * @author Manuel Gieseking
 */
@Deprecated
public class Pnwt2KripkeStructureTransformerOutgoingLarge {

    public static PnwtKripkeStructure create(PetriNetWithTransits pnwt) {
        PnwtKripkeStructure k = new PnwtKripkeStructure(pnwt.getName() + "_ks");
        // initial state
        KripkeState<NodeLabel> init = k.createAndAddState("I", new NodeLabel[0]);
        k.addInitialState(init);

        LinkedList<KripkeState<NodeLabel>> todo = new LinkedList<>();

        // add all newly created transits
        for (Transition transition : pnwt.getTransitions()) {
            Transit initialTransit = pnwt.getInitialTransit(transition);
            if (initialTransit != null) { // all initial transits
                addSuccessor(k, pnwt, init, transition, initialTransit, todo);
            }
        }

        // add all successors as long as there are some
        while (!todo.isEmpty()) {
            KripkeState<NodeLabel> pre = todo.pop();
            Set<NodeLabel> labels = pre.getLabels();
            if (labels.size() == 1) { // this can only happen if it's just a place (and we handled the successors already)
                continue;
            }
            // get the place and transition of the labels
            Place p = null;
            Transition t = null;
            for (NodeLabel label : labels) {
                Node n = label.getNode();
                if (n instanceof Place) {
                    p = (Place) n;
                } else {
                    t = (Transition) n;
                }
            }
            Transit transit = pnwt.getTransit(t, p); // must have a real transit otherwise the transition would've not been added
            addSuccessor(k, pnwt, pre, t, transit, todo);
        }
        return k;
    }

    /**
     * transit must be !=null
     *
     * @param k
     * @param preState
     * @param t
     * @param transit
     * @param todo
     */
    private static void addSuccessor(PnwtKripkeStructure k, PetriNetWithTransits pnwt, KripkeState<NodeLabel> preState, Transition t, Transit transit, LinkedList<KripkeState<NodeLabel>> todo) {
        for (Place place : transit.getPostset()) { // in which place created
            List<Transition> notTransitingTransitions = new ArrayList<>();
            for (Transition t2 : place.getPostset()) { // get all transit successors
                Transit succTransit = pnwt.getTransit(t2, place);
                if (succTransit != null) { // this transition can further transit the token
                    String id = "(" + place.getId() + "," + t2.getId() + ")";
                    KripkeState<NodeLabel> succ;
                    if (!k.stateExists(id)) {
                        succ = k.createAndAddState(id, new NodeLabel(place), new NodeLabel(t2));
                        todo.add(succ);
                    } else {
                        succ = k.getState(id);
                    }
                    k.createAndAddEdge(preState.getId(), new TransitionLabel(t), succ.getId());
                } else {
                    notTransitingTransitions.add(t2);
                }
            }
            // add the state when the run is not choosing a successor
            String id = "(" + place.getId() + ")";
            KripkeState<NodeLabel> succ;
            if (!k.stateExists(id)) {
                succ = k.createAndAddState(id, new NodeLabel(place));
                todo.add(succ);
                // add loops to this state for all transitions not succeeding a chain
                for (Transition notTransitingTransition : notTransitingTransitions) {
                    k.createAndAddEdge(succ.getId(), new TransitionLabel(notTransitingTransition), succ.getId());
                }
            } else {
                succ = k.getState(id);
            }
            k.createAndAddEdge(preState.getId(), new TransitionLabel(t), succ.getId());

        }
    }

}
