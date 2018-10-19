package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.ds.petrigame.TokenFlow;
import uniolunisaar.adam.logic.flowltl.FlowFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.modelchecker.util.ModelCheckerTools;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class PetriNetTransformerFlowLTLSequential extends PetriNetTransformerFlowLTL {
//     /**
//     * Adds maximally a new copy for each place and each sub flow formula. Thus,
//     * each sub flow formula yields one new block, where we check each flow
//     * chain by creating a run for each chain. The succeeding of the flow chains
//     * is done sequentially. The original net choses a transition and this
//     * choice is sequentially given to each sub net concerning the belonging
//     * flow subformula.
//     *
//     * @param net
//     * @param formula
//     * @return
//     */
//    public static PetriGame createNet4ModelCheckingSequential(PetriGame net, IRunFormula formula) {
//        return PetriNetTransformerFlowLTL.createNet4ModelCheckingSequential(net, formula);
//    }

    /**
     * Adds maximally a new copy for each place and each sub flow formula. Thus,
     * each sub flow formula yields one new block, where we check each flow
     * chain by creating a run for each chain. The succeeding of the flow chains
     * is done sequentially. The original net choses a transition and this
     * choice is sequentially given to each sub net concerning the belonging
     * flow subformula.
     *
     * @param net
     * @param formula
     * @return
     */
    public static PetriGame createNet4ModelCheckingSequential(PetriGame net, IRunFormula formula) {
        // Copy the original net 
        PetriGame out = new PetriGame(net);
        out.setName(net.getName() + "_mc");
        // Add to each original transition a place such that we can disable these transitions
        // to give the token to the checking of the subflowformulas
        for (Transition t : net.getTransitions()) {
            if (!net.getTokenFlows(t).isEmpty()) { // only for those which have tokenflows
                Place act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId());
                act.setInitialToken(1);
            }
        }

        // Add a subnet for each flow formula where each run of this subnet represents a flow chain of the original net
        List<FlowFormula> flowFormulas = ModelCheckerTools.getFlowFormulas(formula);
        for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) {
            // create an initial place representing the chosen place
            Place init = out.createPlace(INIT_TOKENFLOW_ID + "-" + nb_ff);
            init.setInitialToken(1);

            // collect the places which are newly created to only copy the necessary part of the net
            List<Place> todo = new ArrayList<>();

            // %%%%% add places and transitions for the new creation of token flows
            // %% via initial places
            for (Place place : net.getPlaces()) {
                if (place.getInitialToken().getValue() > 0 && net.isInitialTokenflow(place)) {
                    // create the place which is states that the chain is currently in this place
                    Place p = out.createPlace(place.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                    out.setOrigID(p, place.getId());
                    // create the dual place of p
                    Place pNot = out.createPlace("!" + place.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                    pNot.setInitialToken(1);
                    out.setOrigID(pNot, place.getId());
                    // create a transition which move the token for the chain to this new initial place of a token chain
                    Transition t = out.createTransition(INIT_TOKENFLOW_ID + "-" + place.getId() + "-" + nb_ff);
                    out.createFlow(init, t);
                    out.createFlow(pNot, t);
                    out.createFlow(t, p);
                    todo.add(p);
                }
            }
            //%% via transitions
            for (Transition t : net.getTransitions()) {
                TokenFlow tfl = net.getInitialTokenFlows(t);
                if (tfl == null) { // not initial token flows -> skip
                    continue;
                }
                // if there is an initial token flow for this transition create
                // a place which is used to activate or deactivate all of these transitions.
                Place act = null;
                if (!tfl.getPostset().isEmpty()) {
                    act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff);
                }
                // for all token flows which are created during the game
                for (Place post : tfl.getPostset()) {
                    // create for all newly created flows a place (and its dual) (if not already existent)
                    // and a transition moving the initial flow token to the place
                    String id = post.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                    Place p, pNot;
                    if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                        // create the place itself
                        p = out.createPlace(id);
                        out.setOrigID(p, post.getId());
                        todo.add(p);
                        // create the dual place of p
                        pNot = out.createPlace("!" + id);
                        pNot.setInitialToken(1);
                        out.setOrigID(pNot, post.getId());
                    } else {
                        // get the belonging places
                        p = out.getPlace(id);
                        pNot = out.getPlace("!" + id);
                    }
                    // Create a copy of the transition which creates this flow
                    // which creates the flow (moves the init token in the new place)
                    // and gives the move token to the next formula (takes it, gives it later when the places are created)
                    Transition tout = out.createTransition();
                    tout.setLabel(t.getId());
                    out.createFlow(init, tout);
                    out.createFlow(pNot, tout);
                    out.createFlow(act, tout);
                    out.createFlow(tout, p);
                }
            }
            // %%%% end add the stuff for the initial token thingys

            // %%%% Inductively add for all newly created places the succeeding places
            //      and transitions of the token flows
            while (!todo.isEmpty()) {
                Place pPre = todo.remove(todo.size() - 1); // do it for the next one
                Place pPreOrig = net.getPlace(out.getOrigID(pPre)); // get the original place
                // for all post transitions of the place add for all token flows a new transition
                // and possibly the corresponding places which follow the flow
                for (Transition t : pPreOrig.getPostset()) {
                    TokenFlow tfl = net.getTokenFlow(t, pPreOrig);
                    if (tfl == null) {
                        continue;
                    }

                    // if there is a token flow for this transition create
                    // a place which is used to activate or deactivate all of these transitions.
                    Place act = null;
                    if (!tfl.getPostset().isEmpty()) {
                        String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                        if (!out.containsNode(id)) {
                            act = out.createPlace(id);
                        } else {
                            act = out.getPlace(id);
                        }
                    }

                    // for all succeeding token flows
                    for (Place post : tfl.getPostset()) {
                        // create for all newly created flows a place (and its dual) (if not already existent)
                        // and a transition moving the initial flow token to the place
                        String id = post.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                        Place pPost, pPostNot;
                        if (!out.containsPlace(id)) { // create or get the place in which the chain is created
                            // create the place itself
                            pPost = out.createPlace(id);
                            out.setOrigID(pPost, post.getId());
                            todo.add(pPost);
                            // create the dual place of p
                            pPostNot = out.createPlace("!" + id);
                            pPostNot.setInitialToken(1);
                            out.setOrigID(pPostNot, post.getId());
                        } else {
                            // get the belonging places
                            pPost = out.getPlace(id);
                            pPostNot = out.getPlace("!" + id);
                        }
                        // Create a copy of the transition which creates this flow
                        // which creates the flow (moves the init token in the new place)
                        // and gives the move token to the next formula (takes it, gives it later when the places are created)
                        Transition tout = out.createTransition();
                        tout.setLabel(t.getId());
                        // move the chain token
                        out.createFlow(pPre, tout);
                        out.createFlow(tout, pPost);
                        // update the negation places accordingly
                        Place pPreNot = out.getPlace("!" + pPre.getId());
                        if (pPreNot != pPostNot) {
                            out.createFlow(pPostNot, tout);
                            out.createFlow(tout, pPreNot);
                        }
                        // deactivate the transitions
                        out.createFlow(act, tout);
                    }
                }
            }
            // Add a transition for every original transition which moves (takes, move is done later)
            // the activation token to the next token flow subnet, when no
            // chain is active
            for (Transition t : net.getTransitions()) {
                Transition tout = out.createTransition(t.getId() + NEXT_ID + "-" + nb_ff);
                tout.setLabel(t.getId());
                // all complements of places which can succeed the tokenflows of the transition
                // Only the transition is used for a token flow
                String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                if (out.containsNode(id)) {
                    Place act = out.getPlace(id);
                    for (Transition tpost : act.getPostset()) { // transitions which take the active token
                        for (Place p : tpost.getPreset()) { // consider all places from which those transitions take a token
                            if (p != act && !p.getId().startsWith("!")) { // but not the active itself and the negation
                                Place negInput = out.getPlace("!" + p.getId());
                                if (!tout.getPreset().contains(negInput)) { // if not already created before
                                    out.createFlow(negInput, tout);
                                    out.createFlow(tout, negInput);
                                }
                            }
                        }
                    }
                    // deactivate the transitions
                    out.createFlow(act, tout);
                }
            }
        }
        if (!flowFormulas.isEmpty()) {
            // Move the active token through the subnets of the flow formulas
            // deactivate all orginal transitions whenever an original transition fires
            for (Transition t : net.getTransitions()) {
                if (!net.getTokenFlows(t).isEmpty()) { // only for those which have tokenflows                        
                    for (Transition t2 : net.getTransitions()) {
                        if (!net.getTokenFlows(t2).isEmpty()) { // only for those which have tokenflows                        
                            out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + t2.getId()), t);
                        }
                    }
                    // and move the active token to the first subnet
                    out.createFlow(t, out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + 0));
                }
            }
            // move the active token through the subnets
            for (int i = 1; i < flowFormulas.size(); i++) {
                for (Transition t : net.getTransitions()) {
                    Place acti = out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + (i - 1));
                    Place actii = out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + i);
                    for (Transition tr : acti.getPostset()) {
                        out.createFlow(tr, actii);
                    }
                }
            }
            // give the token back to the original net (means reactivate all transitions
            for (Transition tOrig : net.getTransitions()) {
                Place actLast = out.getPlace(ACTIVATION_PREFIX_ID + tOrig.getId() + TOKENFLOW_SUFFIX_ID + "-" + (flowFormulas.size() - 1));
                for (Transition tr : actLast.getPostset()) {
                    for (Transition transition : net.getTransitions()) {
                        out.createFlow(tr, out.getPlace(ACTIVATION_PREFIX_ID + transition.getId()));
                    }
                }
            }
        } else {
            // should not really be used, since the normal model checking should be used in these cases
            Logger.getInstance().addMessage("[WARNING] No flow subformula within '" + formula.toSymbolString() + "."
                    + " The standard LTL model checker should be used.", false);
            // but to have still some meaningful output add for all transition the
            // connections to the act places
            for (Transition t : out.getTransitions()) {
                Place act = out.getPlace(ACTIVATION_PREFIX_ID + t.getId());
                out.createFlow(t, act);
                out.createFlow(act, t);
            }
        }
        // delete the fairness assumption of all original transitions
        for (Transition t : net.getTransitions()) {
            out.removeStrongFair(out.getTransition(t.getId()));
            out.removeWeakFair(out.getTransition(t.getId()));
        }
        // delete all token flows
        for (Transition t : net.getTransitions()) {
            for (Place p : t.getPreset()) {
                out.removeTokenFlow(p, t);
            }
            for (Place p : t.getPostset()) {
                out.removeTokenFlow(t, p);
            }
        }
        // and the initial token flow markers
        for (Place place : net.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && net.isInitialTokenflow(place)) {
                out.removeInitialTokenflow(out.getPlace(place.getId()));
            }
        }
        return out;
    }
}
