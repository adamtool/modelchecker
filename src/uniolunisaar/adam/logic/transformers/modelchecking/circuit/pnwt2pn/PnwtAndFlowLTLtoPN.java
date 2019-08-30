package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndFlowLTLtoPN {

    public static final String ACTIVATION_PREFIX_ID = "<act>_";
    public static final String INIT_TOKENFLOW_ID = "<init_tfl>";
    public static final String NEW_TOKENFLOW_ID = "<new_tfl>";
//    public static final String NO_CHAIN_ID = "<no_tfl>"; // is subsumed by the new_tokenflow_id place
    public static final String TOKENFLOW_SUFFIX_ID = "_<tfl>";

    static PetriNetWithTransits createOriginalPartOfTheNet(PetriNetWithTransits orig, boolean initFirstStep) {
        // Copy the original net
        PetriNetWithTransits out = new PetriNetWithTransits(orig);
        out.setName(orig.getName() + "_mc");

        // delete the fairness assumption of all original transitions
        for (Transition t : orig.getTransitions()) {
            out.removeStrongFair(out.getTransition(t.getId()));
            out.removeWeakFair(out.getTransition(t.getId()));
        }
        return out;
    }

    /**
     *
     * @param orig
     * @param out
     * @param nb_ff
     * @param initFirstStep
     * @return
     */
    static PetriNetWithTransits addSubFlowFormulaNet(PetriNetWithTransits orig, PetriNetWithTransits out, int nb_ff, boolean initFirstStep) {
        // create an initial place representing the chosen flow chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID + "-" + nb_ff);
        init.setInitialToken(1);
        out.setPartition(init, nb_ff + 1);

        // collect the places which are newly created to only copy the necessary part of the net
        List<Place> todo = new ArrayList<>();

        // %%%%% add places and transitions for the new creation of token flows
        // %% via initial places
        for (Place place : orig.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && orig.isInitialTransit(place)) {
                // create the place which states that the chain is currently in this place
                Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                out.setPartition(p, nb_ff + 1);
                out.setOrigID(p, place.getId());
                // create a transition which move the token for the chain to this new initial place of a token chain
                Transition t = out.createTransition(INIT_TOKENFLOW_ID + "-" + place.getId() + "-" + nb_ff);
                t.putExtension("subformula", nb_ff);
                out.createFlow(init, t);
                out.createFlow(t, p);
                todo.add(p);
            }
        }
        if (initFirstStep) {
            // create the place and transition which choses that a new chain will be created during the processing of the net
            // if there is any // just do it in every case this can subsume the place that no_chain had been chosen.
//            boolean newFlowCanBeCreated = false;
//            for (Transition t : orig.getTransitions()) {
//                if (orig.getInitialTransit(t) != null) {
//                    newFlowCanBeCreated = true;
//                }
//            }
//            if (newFlowCanBeCreated) {
            Transition newTfl = out.createTransition(INIT_TOKENFLOW_ID + "-" + NEW_TOKENFLOW_ID + "-" + nb_ff);
            newTfl.putExtension("subformula", nb_ff);
            Place newTflPlace = out.createPlace(NEW_TOKENFLOW_ID + "-" + nb_ff);
            out.setPartition(newTflPlace, nb_ff + 1);
            out.createFlow(init, newTfl);
            out.createFlow(newTfl, newTflPlace);
//            }
            // create the place and transition which states that no chain exists at all
            // is no subsumed of the above case
//            Transition noChain = out.createTransition(INIT_TOKENFLOW_ID + "-" + NO_CHAIN_ID + "-" + nb_ff);
//            Place noChainPlace = out.createPlace(NO_CHAIN_ID + "-" + nb_ff);
//            out.setPartition(noChainPlace, nb_ff);
//            out.createFlow(init, noChain);
//            out.createFlow(noChain, noChainPlace);
        }

        //%% via transitions
        for (Transition t : orig.getTransitions()) {
            Transit tfl = orig.getInitialTransit(t);
            if (tfl == null) { // not initial token flows -> skip
                continue;
            }
            // for all token flows which are created during the game
            int count = 0;
            for (Place post : tfl.getPostset()) {
                // create for all newly created flows a place (if not already existent)
                // and a transition moving the initial flow token to the place
                String id = post.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                Place p;
                if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                    // create the place itself
                    p = out.createPlace(id);
                    out.setPartition(p, nb_ff + 1);
                    out.setOrigID(p, post.getId());
                    todo.add(p);
                } else {
                    // get the belonging place
                    p = out.getPlace(id);
                }
                // Create a copy of the transition which creates this flow
                // (moves the init token in the new place)
                Transition tout = out.createTransition(t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff + "-" + (count++));
                tout.setLabel(t.getId());
                tout.putExtension("subformula", nb_ff);
                if (initFirstStep) {
                    out.createFlow(out.getPlace(NEW_TOKENFLOW_ID + "-" + nb_ff), tout);
                } else {
                    out.createFlow(init, tout);
                }
                out.createFlow(tout, p);
            }
        }
        // %%%% end handling creation of token flows

        // %%%% Inductively add for all newly created places the succeeding places
        //      and transitions of the token flows
        while (!todo.isEmpty()) {
            Place pPre = todo.remove(todo.size() - 1); // do it for the next one
            Place pPreOrig = orig.getPlace(out.getOrigID(pPre)); // get the original place
            // for all post transitions of the place add for all token flows a new transition
            // and possibly the corresponding places which follow the flow
            for (Transition t : pPreOrig.getPostset()) {
                Transit tfl = orig.getTransit(t, pPreOrig);
                if (tfl == null) {
                    continue;
                }

                // for all succeeding token flows
//                int count = 0;
                for (Place post : tfl.getPostset()) {
                    // create for all flows a place (if not already existent)
                    // and a transition moving the flow token to the place
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                    Place pPost;
                    if (!out.containsPlace(id)) { // create or get the place in which the chain is moved
                        // create the place itself
                        pPost = out.createPlace(id);
                        out.setPartition(pPost, nb_ff + 1);
                        out.setOrigID(pPost, post.getId());
                        todo.add(pPost);
                    } else {
                        // get the belonging places
                        pPost = out.getPlace(id);
                    }
                    // Create a copy of the transition which moves this flow
//                    Transition tout = out.createTransition(t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff + "-" + (count++));
                    // label is not working so easily since also in a next while run the same transition could be used from another place
                    Transition tout = out.createTransition();
                    tout.setLabel(t.getId());
                    tout.putExtension("subformula", nb_ff);
                    // move the chain token
                    out.createFlow(pPre, tout);
                    out.createFlow(tout, pPost);
                }
            }
        }

        // delete all token flows
        for (Transition t : orig.getTransitions()) {
            for (Place p : t.getPreset()) {
                out.removeTransit(p, t);
            }
            for (Place p : t.getPostset()) {
                out.removeTransit(t, p);
            }
        }
        // and the initial token flow markers
        for (Place place : orig.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && orig.isInitialTransit(place)) {
                out.removeInitialTokenflow(out.getPlace(place.getId()));
            }
        }
        return out;
    }
}
