package uniolunisaar.adam.modelchecker.transformers.petrinet;

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

    public static final String NEXT_ID = "_<nxt>";

    /**
     * Adds maximally a new copy for each place and each sub flow formula. Thus,
     * each sub flow formula yields one new block, where we check each flow
     * chain by creating a run for each chain. The succeeding of the flow chains
     * is done sequentially. The original net choses a transition and this
     * choice is sequentially given to each sub net concerning the belonging
     * flow subformula.
     *
     * @param orig
     * @param formula
     * @param initFirstStep
     * @return
     */
    public static PetriGame createNet4ModelCheckingSequential(PetriGame orig, IRunFormula formula, boolean initFirstStep) {
        PetriGame out = createOriginalPartOfTheNet(orig, initFirstStep);

        // add for all weak fairness a strong fairness assumptions, because now transitions cannot be 
        // enabled infinitely long since the active token is move to the subformulas
        // and also add again the strong fairness, since we deleted them in the super method
//        for (Transition t : orig.getTransitions()) {
//            if (orig.isWeakFair(t) || orig.isStrongFair(t)) {
//                out.setStrongFair(out.getTransition(t.getId()));
//            }
//        } todo: did it in the modelchecker but think of a better way
        List<FlowFormula> flowFormulas = ModelCheckerTools.getFlowFormulas(formula);
        for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) {
            // adds the subnet which only creates places and copies of transitions for each flow
            addSubFlowFormulaNet(orig, out, nb_ff, initFirstStep);

            if (initFirstStep) {
                // create an activation place for the initialization
                Place act = out.createPlace(ACTIVATION_PREFIX_ID + INIT_TOKENFLOW_ID + "-" + nb_ff);
                out.setPartition(act, nb_ff);
                if (nb_ff == 0) {
                    act.setInitialToken(1);
                }
            }

            // additionally add the negations of each place and connect them accordingly
            for (Place p : orig.getPlaces()) {
                String id = p.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                if (out.containsNode(id)) {
                    Place place = out.getPlace(id);
                    // create the dual place of p
                    Place pNot = out.createPlace("!" + id);
                    pNot.setInitialToken(1);
                    out.setOrigID(pNot, place.getId());
                    out.setPartition(pNot, nb_ff);
                    for (Transition t : place.getPreset()) {
                        if (!place.getPostset().contains(t)) {
                            out.createFlow(pNot, t);
                        }
                    }
                    for (Transition t : place.getPostset()) {
                        if (!place.getPreset().contains(t)) {
                            out.createFlow(t, pNot);
                        }
                    }
                }
            }
// not necessary since when a new chain is chosen all negation places are occupied and thus the active token can be passed to the next subformula anyways            
//            // also for the place which is used for the newly creation of chains if it exists
//            if (out.containsPlace(NEW_TOKENFLOW_ID + "-" + nb_ff)) {
//                Place newTflPlace = out.getPlace(NEW_TOKENFLOW_ID + "-" + nb_ff);
//                // create the dual place of p
//                Place pNot = out.createPlace("!" + newTflPlace.getId());
//                pNot.setInitialToken(1);
//                out.setOrigID(pNot, newTflPlace.getId());
//                out.setPartition(pNot, nb_ff);
//                for (Transition t : newTflPlace.getPreset()) {
//                    if (!newTflPlace.getPostset().contains(t)) {
//                        out.createFlow(pNot, t);
//                    }
//                }
//                for (Transition t : newTflPlace.getPostset()) {
//                    if (!newTflPlace.getPreset().contains(t)) {
//                        out.createFlow(t, pNot);
//                    }
//                }
//            }

// CODE when I give all transitions a proper name, so fare I don't have it so do a little bit more searching in the net
//            // for each original transition which is used create an active place 
//            // from which all copies are dependent
//            for (Transition t : orig.getTransitions()) {
//                String idPref = t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff + "-";
//                Place act = null;
//                if (out.containsNode(idPref + 0)) { // for all transition which create or succeed a token chain
//                    // create the activation place                    
//                    String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
//                    act = out.createPlace(id);
//                    // Add a transition which moves (takes, move is done later)
//                    // the activation token to the next token flow subnet, when no
//                    // chain is active
//                    Transition tout = out.createTransition(t.getId() + NEXT_ID + "-" + nb_ff);
//                    tout.setLabel(t.getId());
//                    // all complements of places which can succeed the tokenflows of the transition
//                    for (Transition tpost : act.getPostset()) { // transitions which take the active token
//                        for (Place p : tpost.getPreset()) { // consider all places from which those transitions take a token
//                            if (p != act && !p.getId().startsWith("!")) { // but not the active itself and the negation
//                                Place negInput = out.getPlace("!" + p.getId());
//                                if (!tout.getPreset().contains(negInput)) { // if the flow is not already created before
//                                    out.createFlow(negInput, tout);
//                                    out.createFlow(tout, negInput);
//                                }
//                            }
//                        }
//                    }
//                    // deactivate the transitions
//                    out.createFlow(act, tout);
//                }
//                // add the activation token to every created token for the chains
//                int count = 0;
//                while (out.containsNode(idPref + count)) {
//                    Transition tout = out.getTransition(idPref + (count++));
//                    out.createFlow(act, tout);
//                }
//            }
///// HERE END CODE
            // for each original transition which is used create an active place 
            // from which all copies are dependent
            for (Transition t : orig.getTransitions()) {
                // search for a transition with the same label of my subformula
                boolean found = false;
                for (Transition t1 : out.getTransitions()) {
                    if (!t1.getId().equals(t.getId()) && t1.getLabel().equals(t.getId())
                            && t1.hasExtension("subformula") && t1.getExtension("subformula").equals(nb_ff)) {
                        found = true;
                    }
                }
                if (found) { // for all transition which create or succeed a token chain
                    // create the activation place                    
                    String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + nb_ff;
                    Place act = out.createPlace(id);
                    out.setPartition(act, nb_ff);
                    // Add a transition which moves (takes, move is done later)
                    // the activation token to the next token flow subnet, when no
                    // chain is active
                    Transition tout = out.createTransition(t.getId() + NEXT_ID + "-" + nb_ff);
                    tout.setLabel(t.getId());
                    tout.putExtension("subformula", nb_ff);
                    // add the activation token to every created token for the chains
                    for (Transition tflow : out.getTransitions()) {
                        if (!tflow.getId().equals(t.getId()) && tflow.getLabel().equals(t.getId())
                                && tflow.hasExtension("subformula") && tflow.getExtension("subformula").equals(nb_ff)) {
                            out.createFlow(act, tflow);
                        }
                    }
                    // all complements of places which can succeed the tokenflows of the transition
                    for (Transition tpost : act.getPostset()) { // transitions which take the active token
                        for (Place p : tpost.getPreset()) { // consider all places from which those transitions take a token
                            if (p != act && !p.getId().startsWith("!")) { // but not the active place itself and its negation
                                if ((initFirstStep && !p.getId().equals(NEW_TOKENFLOW_ID + "-" + nb_ff)) // also not for place for newly created chains in the initFirstCase
                                        || (!initFirstStep && !p.getId().equals(INIT_TOKENFLOW_ID + "-" + nb_ff))) { // also not for place for newly created chains in the !initFirstCase
                                    Place negInput = out.getPlace("!" + p.getId());
                                    if (!tout.getPreset().contains(negInput)) { // if the flow is not already created before
                                        out.createFlow(negInput, tout);
                                        out.createFlow(tout, negInput);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!flowFormulas.isEmpty()) {
            if (initFirstStep) {
                // Get the original initial Marking
                List<Place> initialMarking = new ArrayList<>();
                for (Place p : orig.getPlaces()) {
                    if (p.getInitialToken().getValue() > 0) {
                        initialMarking.add(p);
                    }
                }
                // all initialization transitions of each subformula already have an active place added (each subformula one)
                // from which they are dependent by firing they give it to the next subformula (this is done here)
                // the last one puts the initial marking to the original net
                for (int i = 0; i < flowFormulas.size(); i++) {
                    Place init = out.getPlace(INIT_TOKENFLOW_ID + "-" + i);
                    Place initAct = out.getPlace(ACTIVATION_PREFIX_ID + INIT_TOKENFLOW_ID + "-" + i);
                    Place initActNext = null;
                    if (i + 1 < flowFormulas.size()) {
                        initActNext = out.getPlace(ACTIVATION_PREFIX_ID + INIT_TOKENFLOW_ID + "-" + (i + 1));
                    }
                    for (Transition t : init.getPostset()) {
                        out.createFlow(initAct, t);
                        if (initActNext != null) {
                            out.createFlow(t, initActNext);
                        } else {
                            for (Place place : initialMarking) {
                                out.createFlow(t, out.getPlace(place.getId()));
                            }
                        }
                    }
                }
            }
            // Move the active token through the subnets of the flow formulas
            // deactivate all orginal transitions whenever an original transition fires
            for (Transition t : orig.getTransitions()) {
                if (!orig.getTokenFlows(t).isEmpty()) { // only for those which have tokenflows                        
                    for (Transition t2 : orig.getTransitions()) {
                        if (!orig.getTokenFlows(t2).isEmpty()) { // only for those which have tokenflows                        
                            out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + t2.getId()), t);
                        }
                    }
                    // and move the active token to the first subnet
                    out.createFlow(t, out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + 0));
                }
            }
            // move the active token through the subnets
            for (int i = 1; i < flowFormulas.size(); i++) {
                for (Transition t : orig.getTransitions()) {
                    Place acti = out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + (i - 1));
                    Place actii = out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + i);
                    for (Transition tr : acti.getPostset()) {
                        out.createFlow(tr, actii);
                    }
                }
            }
            // give the token back to the original net (means reactivate all transitions
            for (Transition tOrig : orig.getTransitions()) {
                String id = ACTIVATION_PREFIX_ID + tOrig.getId() + TOKENFLOW_SUFFIX_ID + "-" + (flowFormulas.size() - 1);
                if (out.containsPlace(id)) { // only if the transition has a token flow
                    Place actLast = out.getPlace(id);
                    for (Transition tr : actLast.getPostset()) {
                        for (Transition transition : orig.getTransitions()) {
                            if (out.containsPlace(ACTIVATION_PREFIX_ID + transition.getId())) { // only if it had a token flow
                                out.createFlow(tr, out.getPlace(ACTIVATION_PREFIX_ID + transition.getId()));
                            }
                        }
                    }
                }
            }
        } else {
            // should not really be used, since the normal model checking should be used in these cases
            Logger.getInstance().addMessage("[WARNING] No flow subformula within '" + formula.toSymbolString() + "'."
                    + " The standard LTL model checker should be used.", false);
            // but to have still some meaningful output add for all transition the
            // connections to the act places
            for (Transition t : out.getTransitions()) {
                Place act = out.getPlace(ACTIVATION_PREFIX_ID + t.getId());
                out.createFlow(t, act);
                out.createFlow(act, t);
            }
        }
        return out;
    }

    /**
     * Adds maximally a new copy for each place and each sub flow formula. Thus,
     * each sub flow formula yields one new block, where we check each flow
     * chain by creating a run for each chain. The succeeding of the flow chains
     * is done sequentially. The original net choses a transition and this
     * choice is sequentially given to each sub net concerning the belonging
     * flow subformula.
     *
     * Is deprecated since most of the code is copied for a different
     * initilization or the parallel approach. Now it is split into the common
     * things and the special sequential thing in
     * createNet4ModelCheckingSequential.
     *
     * This version is from 19.10.2018 identically to the semantics of
     * createNet4ModelCheckingSequential.
     *
     * @param net
     * @param formula
     * @return
     */
    @Deprecated
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
            Logger.getInstance().addMessage("[WARNING] No flow subformula within '" + formula.toSymbolString() + "'."
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

    /**
     * This is the version of createNet4ModelCheckingSequential where for one
     * flow subformula the initialization step of the flow chains is done as a
     * first step in the net.
     *
     * @param net
     * @param formula
     * @return
     * @deprecated
     */
    @Deprecated
    public static PetriGame createNet4ModelCheckingSequentialInitFirst(PetriGame net, IRunFormula formula
    ) {
        // Copy the original net
        PetriGame out = new PetriGame(net);
        out.setName(net.getName() + "_mc");
        // remember the initial marking
        List<Place> initialMarking = new ArrayList<>();
        // and delete it
        for (Place p : out.getPlaces()) {
            if (p.getInitialToken().getValue() > 0) {
                p.setInitialToken(0);
                initialMarking.add(p);
            }
        }

        // Add to each original transition a place such that we can disable these transitions
        // to give the token to the checking of the subflowformulas
        for (Transition t : net.getTransitions()) {
            if (!net.getTokenFlows(t).isEmpty()) { // only for those which have tokenflows
                Place act = out.createPlace(ACTIVATION_PREFIX_ID + t.getId());
                act.setInitialToken(1);
            }
        }

        // Add a subnet for each flow formula where each run of this subnet represents a flow chain of the original net
        // here only start the net by adding a token which represents the flow
        // this token has firstly be put onto a place which belong to one place marked as initially starting a token chain
        // or on a place where the transitions which start new token chains have to start from
        // after deciding which new chain should start or that a new chain should be started later by a transition
        // we put back the initial marking.
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
                    // put the places into the former initial marking
                    for (Place in : initialMarking) {
                        out.createFlow(t, in);
                    }
                    todo.add(p);
                }
            }
            // create the place and transition which choses that a new chain will be created during the processing of the net
            Transition newTfl = out.createTransition(INIT_TOKENFLOW_ID + "-" + NEW_TOKENFLOW_ID + "-" + nb_ff);
            Place newTflPlace = out.createPlace(NEW_TOKENFLOW_ID + "-" + nb_ff);
            out.createFlow(INIT_TOKENFLOW_ID + "-" + nb_ff, newTfl.getId());
            out.createFlow(newTfl, newTflPlace);
            // put the places into the former initial marking
            for (Place in : initialMarking) {
                out.createFlow(newTfl, in);
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
                    out.createFlow(newTflPlace, tout);
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
