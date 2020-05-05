package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.KripkeState;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.TransitionLabel;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;

/**
 *
 * @author Manuel Gieseking
 */
public class Pnwt2KripkeStructureTransformer {

    public static PnwtKripkeStructure create(PetriNetWithTransits pnwt, boolean onlyPlacesInAP) {
        PnwtKripkeStructure k = new PnwtKripkeStructure(pnwt.getName() + "_ks");

        LinkedList<KripkeState<NodeLabel>> todo = new LinkedList<>();

        // For each place in which a transit starts add an initial state
        // (if with transitions as atomic propositions also for the combi with the outgoing transiting transitions)
        for (Transition transition : pnwt.getTransitions()) {
            Transit initialTransit = pnwt.getInitialTransit(transition);
            if (initialTransit != null) { // all initial transits
                for (Place place : initialTransit.getPostset()) { // all places in which a transit is started
                    if (onlyPlacesInAP) {
                        createAndAddStatesForOnlyPlacesInAP(k, pnwt, place, todo);
                    } else {
                        createAndAddStatesForAlsoTransitionsInAP(k, pnwt, place, todo);
                    }
                }
            }
        }
        // all states can now be initial
        for (KripkeState<NodeLabel> state : k.getStates().values()) {
            k.addInitialState(state);
        }

        // add all successors as long as there are some
        while (!todo.isEmpty()) {
            KripkeState<NodeLabel> pre = todo.pop();
            Set<NodeLabel> labels = pre.getLabels();

            if (onlyPlacesInAP || labels.size() == 1) {
                // Get the corresponding place
                Place place = pnwt.getPlace(pre.getId());
                // for all successors reached by a transit add a place and an edge
                for (Transition t : place.getPostset()) { // get all transiting successors
                    Transit succTransit = pnwt.getTransit(t, place);
                    if (succTransit != null) { // this transition can further transit the token
                        for (Place post : succTransit.getPostset()) {
                            KripkeState<NodeLabel> succ = createAndAddStatesForOnlyPlacesInAP(k, pnwt, post, todo);
                            k.createAndAddEdge(place.getId(), new TransitionLabel(t), succ.getId());
                        }
                    }
                }
                // add the special loop to keep it smaller
                // with the transition of the transition label == null to state all 
                // other transitions which are not transiting in this place
                k.createAndAddEdge(place.getId(), new TransitionLabel(null), place.getId());
            } else {
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
                // t!= null since otherwise labels.size()<2 s.o.
                Transit succTransit = pnwt.getTransit(t, p); // must have a real transit otherwise the transition would've not been added (thus != null)
                for (Place post : succTransit.getPostset()) {
                    List<KripkeState<NodeLabel>> succs = createAndAddStatesForAlsoTransitionsInAP(k, pnwt, post, todo);
                    for (KripkeState<NodeLabel> succ : succs) {
                        k.createAndAddEdge(p.getId(), new TransitionLabel(t), succ.getId());
                    }
                }
            }
        }
        return k;
    }

    private static KripkeState<NodeLabel> createAndAddStatesForOnlyPlacesInAP(PnwtKripkeStructure k, PetriNetWithTransits pnwt, Place place, LinkedList<KripkeState<NodeLabel>> todo) {
        String id = place.getId();
        KripkeState<NodeLabel> succ;
        if (!k.stateExists(id)) {
            succ = k.createAndAddState(id, new NodeLabel(place));
            todo.add(succ);
        } else {
            succ = k.getState(id);
        }
        return succ;
    }

    private static List<KripkeState<NodeLabel>> createAndAddStatesForAlsoTransitionsInAP(PnwtKripkeStructure k, PetriNetWithTransits pnwt, Place place, LinkedList<KripkeState<NodeLabel>> todo) {
        List<KripkeState<NodeLabel>> states = new ArrayList<>();
        // when we also have transitions as atomic proposition we have to 
        // add those to the state. We use the outgoing semantics, thus
        // we have one state (place, t) for each transition t which can
        // transit the chain from place
        for (Transition t : place.getPostset()) { // get all transiting successors
            Transit succTransit = pnwt.getTransit(t, place);
            if (succTransit != null) { // this transition can further transit the token
                String id = place.getId() + "," + t.getId();
                KripkeState<NodeLabel> succ;
                if (!k.stateExists(id)) {
                    succ = k.createAndAddState(id, new NodeLabel(place), new NodeLabel(t));
                    todo.add(succ);
                } else {
                    succ = k.getState(id);
                }
                states.add(succ);
            }
        }
        // also add one state which only contains the place itself for the 
        // case the run never chooses one of the transiting transitions anymore
        String id = place.getId();
        KripkeState<NodeLabel> succ;
        if (!k.stateExists(id)) {
            succ = k.createAndAddState(id, new NodeLabel(place));
            todo.add(succ);
        } else {
            succ = k.getState(id);
        }
        states.add(succ);
        return states;
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
