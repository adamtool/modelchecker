package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure;

import java.util.LinkedList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.kripkestructure.KripkeState;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.kripkestructure.TransitionLabel;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;

/**
 * The class corresponds to the ingoing semantics and implements the
 * construction of the label Kripke structure of the thesis.
 *
 * @author Manuel Gieseking
 */
public class Pnwt2KripkeStructureTransformerIngoingFull {

    // stuttering is currently done with a transition label with a null as transition
//    public static final char stutterSymbol = '#';
    // this symbol is used for the stutter transitions s_p
    public static final char STUTTERSYMBOL = '#';

    public static PnwtKripkeStructure create(PetriNetWithTransits pnwt, List<String> AP) {
        PnwtKripkeStructure k = new PnwtKripkeStructure(pnwt.getName() + "_ks");

        // the working list
        LinkedList<KripkeState<NodeLabel>> todo = new LinkedList<>();

        // For each place in which a transit starts add an initial state 
        // (if starting transition is in AP, (t,p) otherwise (p))
        for (Transition transition : pnwt.getTransitions()) {
            Transit initialTransit = pnwt.getInitialTransit(transition);
            if (initialTransit != null) { // all initial transits
                Transition t = initialTransit.getTransition();
                boolean withTransition = AP.contains(t.getId()); // the starting transition is contained in the formula
                for (Place place : initialTransit.getPostset()) { // all places in which a transit is started
                    KripkeState<NodeLabel> init = createAndAddSuccessorState(k, pnwt, AP, withTransition ? t : null, place, todo);
                    k.addInitialState(init);
                }
            }
        }

        // add all successors as long as there are some
        while (!todo.isEmpty()) {
            // take the next state
            KripkeState<NodeLabel> pre = todo.pop();
            // Get the corresponding place
            String id = pre.getId();
            if (id.contains(",")) {
                id = id.substring(id.indexOf(",") + 1, id.length());
            }
            Place place = pnwt.getPlace(id);
            // for all successors reached by a transit add the corresponding state and edge
            for (Transition t : place.getPostset()) { // get all transiting successors
                Transit succTransit = pnwt.getTransit(t, place);
                if (succTransit != null) { // this transition can further transit the token
//                    t = succTransit.getTransition();
                    boolean withTransition = AP.contains(t.getId()); // the starting transition is contained in the formula
                    for (Place post : succTransit.getPostset()) {
                        KripkeState<NodeLabel> succ = createAndAddSuccessorState(k, pnwt, AP, withTransition ? t : null, post, todo);
                        k.createAndAddEdge(pre.getId(), new TransitionLabel(t), succ.getId());
                    }
                }
            }
        }
        return k;
    }

    private static KripkeState<NodeLabel> createAndAddSuccessorState(PnwtKripkeStructure k, PetriNetWithTransits pnwt, List<String> AP,
            Transition tin, Place post,
            LinkedList<KripkeState<NodeLabel>> todo) {
        String id = tin != null ? tin.getId() + "," + post.getId() : post.getId();
        KripkeState<NodeLabel> succ;
        System.out.println("----- id " + id);
        System.out.println(AP.toString());
        System.out.println("contains");
        System.out.println(post.getId());
        if (!k.stateExists(id)) { // the state does not exist, create a new one
            if (tin != null && AP.contains(post.getId())) { // create the state with the transition
                succ = k.createAndAddState(id, new NodeLabel(tin), new NodeLabel(post));
            } else if (AP.contains(post.getId())) {
                succ = k.createAndAddState(id, new NodeLabel(post));
            } else if (tin != null) {
                succ = k.createAndAddState(id, new NodeLabel(tin));
            } else {
                succ = k.createAndAddState(id);
            }
            // Add also the corresponding stutterstate p_s
            String idStutterState = post.getId() + STUTTERSYMBOL;
            if (!k.stateExists(idStutterState)) {
                // add the stutter state
                KripkeState<NodeLabel> stutterState;
                if (AP.contains(post.getId())) {
                    stutterState = k.createAndAddState(idStutterState, new NodeLabel(post));
                } else {
                    stutterState = k.createAndAddState(idStutterState);
                }
                // add the edge and the selfloops
                k.createAndAddEdge(succ.getId(), new TransitionLabel(null), stutterState.getId()); // it's kind of hacky but here we know its #_p, because it's not a loop in the stutter state
                k.createAndAddEdge(stutterState.getId(), new TransitionLabel(null), stutterState.getId()); // here we know its # because it's a loop in the stutter state...
                for (Transition transition : pnwt.getTransitions()) {
                    k.createAndAddEdge(stutterState.getId(), new TransitionLabel(transition), stutterState.getId());
                }
            }
            todo.add(succ);
        } else {
            succ = k.getState(id);
            // add also the edge to the stutter place
            k.createAndAddEdge(succ.getId(), new TransitionLabel(null), post.getId() + STUTTERSYMBOL); // it's kind of hacky but here we know its #_p, because it's not a loop in the stutter state              
        }
        return succ;
    }
}
