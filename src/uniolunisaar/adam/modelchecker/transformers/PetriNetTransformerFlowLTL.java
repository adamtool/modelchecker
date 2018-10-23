package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.TokenFlow;

/**
 *
 * @author Manuel Gieseking
 */
public class PetriNetTransformerFlowLTL {

    public static final String ACTIVATION_PREFIX_ID = "<act>_";
    public static final String INIT_TOKENFLOW_ID = "<init_tfl>";
    public static final String NEW_TOKENFLOW_ID = "<new_tfl>";
    public static final String TOKENFLOW_SUFFIX_ID = "_<tfl>";
    public static final String NEXT_ID = "_<nxt>";

    static PetriGame createOriginalPartOfTheNet(PetriGame orig, boolean initFirstStep) {
        // Copy the original net
        PetriGame out = new PetriGame(orig);
        out.setName(orig.getName() + "_mc");

        // delete the initial marking if init
        if (initFirstStep) {
            for (Place p : out.getPlaces()) {
                if (p.getInitialToken().getValue() > 0) {
                    p.setInitialToken(0);
                }
            }
        }

        // Add to each original transition a place such that we can disable the transitions
        for (Transition t : orig.getTransitions()) {
            if (!orig.getTokenFlows(t).isEmpty()) { // only for those which have tokenflows
                Place act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId());
                act.setInitialToken(1);
            }
        }

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
    static PetriGame addSubFlowFormulaNet(PetriGame orig, PetriGame out, int nb_ff, boolean initFirstStep) {
        // create an initial place representing the chosen flow chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID + "-" + nb_ff);
        init.setInitialToken(1);
        out.setPartition(init, nb_ff);

        // collect the places which are newly created to only copy the necessary part of the net
        List<Place> todo = new ArrayList<>();

        // %%%%% add places and transitions for the new creation of token flows
        // %% via initial places
        for (Place place : orig.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && orig.isInitialTokenflow(place)) {
                // create the place which states that the chain is currently in this place
                Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                out.setPartition(p, nb_ff);
                out.setOrigID(p, place.getId());
                // create a transition which move the token for the chain to this new initial place of a token chain
                Transition t = out.createTransition(INIT_TOKENFLOW_ID + "-" + place.getId() + "-" + nb_ff);
                out.createFlow(init, t);
                out.createFlow(t, p);
                todo.add(p);
            }
        }
        if (initFirstStep) {
            // create the place and transition which choses that a new chain will be created during the processing of the net
            Transition newTfl = out.createTransition(INIT_TOKENFLOW_ID + "-" + NEW_TOKENFLOW_ID + "-" + nb_ff);
            Place newTflPlace = out.createPlace(NEW_TOKENFLOW_ID + "-" + nb_ff);
            out.setPartition(newTflPlace, nb_ff);
            out.createFlow(init, newTfl);
            out.createFlow(newTfl, newTflPlace);
        }

        //%% via transitions
        for (Transition t : orig.getTransitions()) {
            TokenFlow tfl = orig.getInitialTokenFlows(t);
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
                    out.setPartition(p, nb_ff);
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
                TokenFlow tfl = orig.getTokenFlow(t, pPreOrig);
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
                        out.setPartition(pPost, nb_ff);
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
                    // move the chain token
                    out.createFlow(pPre, tout);
                    out.createFlow(tout, pPost);
                }
            }
        }

        // delete all token flows
        for (Transition t : orig.getTransitions()) {
            for (Place p : t.getPreset()) {
                out.removeTokenFlow(p, t);
            }
            for (Place p : t.getPostset()) {
                out.removeTokenFlow(t, p);
            }
        }
        // and the initial token flow markers
        for (Place place : orig.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && orig.isInitialTokenflow(place)) {
                out.removeInitialTokenflow(out.getPlace(place.getId()));
            }
        }
        return out;
    }
}
