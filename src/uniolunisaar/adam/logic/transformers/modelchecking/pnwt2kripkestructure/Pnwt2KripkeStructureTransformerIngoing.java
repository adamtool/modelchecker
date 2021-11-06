package uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.kripkestructure.KripkeState;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.kripkestructure.TransitionLabel;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformerIngoingFull.STUTTERSYMBOL;

/**
 * The class corresponds to the ingoing semantics and implements the
 * construction presented in the ATVA'20 paper.
 *
 * @author Manuel Gieseking
 */
public class Pnwt2KripkeStructureTransformerIngoing {

    // stuttering is currently done with a transition label with a null as transition
//    public static final char stutterSymbol = '#';
    public static PnwtKripkeStructure create(PetriNetWithTransits pnwt, List<Transition> trAPs) {
        PnwtKripkeStructure k = new PnwtKripkeStructure(pnwt.getName() + "_ks");

        // the working list
        LinkedList<KripkeState<NodeLabel>> todo = new LinkedList<>();
        // remember the t,p states (memory vs. running time) to add the stuttering
        // edges at the end (don't do it during the creating, because we do not 
        // know if we need the successors of the p states)
        List<Pair<Place, KripkeState<NodeLabel>>> tpStates = new ArrayList<>();

        // For each place in which a transit starts add an initial state
        // (if starting transition is in AP, then also for the combi with t)
        for (Transition transition : pnwt.getTransitions()) {
            Transit initialTransit = pnwt.getInitialTransit(transition);
            if (initialTransit != null) { // all initial transits
                Transition t = initialTransit.getTransition();
                boolean withTransition = trAPs.contains(t); // the starting transition is contained in the formula
                for (Place place : initialTransit.getPostset()) { // all places in which a transit is started
                    KripkeState<NodeLabel> init = createAndAddSuccessorState(k, pnwt, withTransition ? t : null, place, todo, tpStates);
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
                    boolean withTransition = trAPs.contains(t); // the starting transition is contained in the formula
                    for (Place post : succTransit.getPostset()) {
                        KripkeState<NodeLabel> succ = createAndAddSuccessorState(k, pnwt, withTransition ? t : null, post, todo, tpStates);
                        k.createAndAddEdge(pre.getId(), new TransitionLabel(t), succ.getId());
                    }
                }
            }
        }

        // add the stutter edges (and, if necessary the additional stutter states)
        for (Pair<Place, KripkeState<NodeLabel>> tpState : tpStates) {
            Place place = tpState.getFirst();
            KripkeState<NodeLabel> succ;
            if (!k.stateExists(place.getId())) { // if the p state does not exist create the state with the stutter edge
                succ = k.createAndAddState(place.getId(), new NodeLabel(place));
                // add the stutter selfloop
                k.createAndAddEdge(succ.getId(), new TransitionLabel("" + STUTTERSYMBOL), succ.getId());
            } else {
                succ = k.getState(place.getId());
            }
            // add the stutter edge 
            k.createAndAddEdge(tpState.getSecond().getId(), new TransitionLabel(STUTTERSYMBOL + place.getId()), succ.getId());
        }
        return k;
    }

    private static KripkeState<NodeLabel> createAndAddSuccessorState(PnwtKripkeStructure k, PetriNetWithTransits pnwt,
            Transition tin, Place post,
            LinkedList<KripkeState<NodeLabel>> todo, List<Pair<Place, KripkeState<NodeLabel>>> tpStates) {
        String id = tin != null ? tin.getId() + "," + post.getId() : post.getId();
        KripkeState<NodeLabel> succ;
        if (!k.stateExists(id)) { // the state does not exist, create a new one
            if (tin != null) { // create the state with the transition
                succ = k.createAndAddState(id, new NodeLabel(tin), new NodeLabel(post));
                // not do it directly here, this could lead to unnecessary larger Kripke structures
                // because it could possible be that we only need the additional place state for the stuttering
                // and don't want to check it further on from there. But we can also not leave it completely out.
                // To not have to check all states again we add the needed states here for a postprocess to add the edges
                tpStates.add(new Pair<>(post, succ));
//                            // add the stutter edge to the state which only contains the place
//                            if (!k.stateExists(place.getId())) {
//                                KripkeState<NodeLabel> placeSucc = k.createAndAddState(id, new NodeLabel(place));
//                                // add the stutter selfloop
//                                k.createAndAddEdge(placeSucc.getId(), new TransitionLabel(null), placeSucc.getId());
//                                // add the stutter edge 
//                                k.createAndAddEdge(succ.getId(), new TransitionLabel(null), placeSucc.getId());
//                                todo.add(placeSucc);
//                            }
            } else {
                succ = k.createAndAddState(id, new NodeLabel(post));
                // add the stutter selfloop
                k.createAndAddEdge(succ.getId(), new TransitionLabel("" + STUTTERSYMBOL), succ.getId());
            }
            todo.add(succ);
        } else {
            succ = k.getState(id);
        }
        return succ;
    }
}
