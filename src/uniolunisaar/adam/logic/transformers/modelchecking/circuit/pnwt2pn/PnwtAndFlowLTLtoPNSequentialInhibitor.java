package uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn;

import java.util.List;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.IRunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import static uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnwt2pn.PnwtAndFlowLTLtoPNSequential.NEXT_ID;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndFlowLTLtoPNSequentialInhibitor extends PnwtAndFlowLTLtoPN {

    /**
     * Adds maximally a new copy for each place and each sub flow formula. Thus,
     * each sub flow formula yields one new block, where we check each flow
     * chain by creating a run for each chain. The succeeding of the flow chains
     * is done sequentially. The original net chooses a transition and this
     * choice is sequentially given to each sub net concerning the belonging
     * flow subformula.
     *
     * @param orig
     * @param formula
     * @param initFirstStep
     * @return
     */
    public static PetriNetWithTransits createNet4ModelCheckingSequential(PetriNetWithTransits orig, IRunFormula formula, boolean initFirstStep) {
        PetriNetWithTransits out = createOriginalPartOfTheNet(orig, initFirstStep);

        // create one activation place for all original transitions
        Place actO = out.createPlace(ACTIVATION_PREFIX_ID + "orig");

        List<FlowFormula> flowFormulas = LogicsTools.getFlowFormulas(formula);
        for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) {
            // adds the subnet which only creates places and copies of transitions for each flow
            addSubFlowFormulaNet(orig, out, nb_ff, initFirstStep);

            if (initFirstStep) {
                // create an activation place for the initialization
                Place act = out.createPlace(ACTIVATION_PREFIX_ID + INIT_TOKENFLOW_ID + "-" + nb_ff);
                out.setPartition(act, nb_ff + 1);
                if (nb_ff == 0) {
                    act.setInitialToken(1);
                }
                // create the nxt transition for the initialization to allow not chosing any chain
                Transition tout = out.createTransition(INIT_TOKENFLOW_ID + NEXT_ID + "-" + nb_ff);
                tout.putExtension("subformula", nb_ff);
            }

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
                    out.setPartition(act, nb_ff + 1);

                    // %%% begin nxt transition
                    // Add a transition which moves (takes, move is done later)
                    // the activation token to the next token flow subnet, when no
                    // chain is active
                    Transition tout = out.createTransition(t.getId() + NEXT_ID + "-" + nb_ff);
                    tout.setLabel(t.getId());
                    tout.putExtension("subformula", nb_ff);
                    // %%% end next transition

                    // add the activation token to every created token for the chains
                    for (Transition tflow : out.getTransitions()) {
                        if (!tflow.getId().equals(t.getId()) && tflow.getLabel().equals(t.getId())
                                && tflow.hasExtension("subformula") && tflow.getExtension("subformula").equals(nb_ff)) {
                            out.createFlow(act, tflow);
                        }
                    }

                    // %%% do the connection for the nxt transition
                    // add for all places which can succeed the tokenflows of the transition an inhibitor edge
                    for (Transition tpost : act.getPostset()) { // transitions which take the active token
                        for (Place p : tpost.getPreset()) { // consider all places from which those transitions take a token
                            if (p != act) { // but not the active place itself 
                                if ((initFirstStep && !p.getId().equals(NEW_TOKENFLOW_ID + "-" + nb_ff)) // also not for place for newly created chains in the initFirstCase
                                        || (!initFirstStep && !p.getId().equals(INIT_TOKENFLOW_ID + "-" + nb_ff))) { // also not for place for newly created chains in the !initFirstCase
                                    if (!tout.getPreset().contains(p)) { // if the flow is not already created before
                                        Flow f = out.createFlow(p, tout);
                                        out.setInhibitor(f);
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

//                   // this is the version where I removed the initial marking and added it later
//                // Get the original initial Marking
//                List<Place> initialMarking = new ArrayList<>();
//                for (Place p : orig.getPlaces()) {
//                    if (p.getInitialToken().getValue() > 0) {
//                        initialMarking.add(p);
//                    }
//                }
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
                    // the transitions moving the init token, i.e., deciding on a chain or a later created new chain                  
                    for (Transition t : init.getPostset()) {
                        out.createFlow(initAct, t);
                        if (initActNext != null) {
                            out.createFlow(t, initActNext);
                        } else {
//                   // this is the version where I removed the initial marking and added it later
//                            for (Place place : initialMarking) {
//                                out.createFlow(t, out.getPlace(place.getId()));
//                            }
                            out.createFlow(t, actO);
                        }
                    }
                    // the next transition
                    Transition nxt = out.getTransition(INIT_TOKENFLOW_ID + NEXT_ID + "-" + i);
                    out.createFlow(initAct, nxt);
                    if (initActNext != null) {
                        out.createFlow(nxt, initActNext);
                    } else {
//                   // this is the version where I removed the initial marking and added it later
//                        for (Place place : initialMarking) {
//                            out.createFlow(nxt, out.getPlace(place.getId()));
//                        }
                        out.createFlow(nxt, actO);
                    }
                }
            }
//            // this is the version when every original transition has its own token
//            // Move the active token through the subnets of the flow formulas
//            // deactivate all orginal transitions whenever an original transition fires
//            for (Transition t : orig.getTransitions()) {
//                if (!orig.getTransits(t).isEmpty()) { // only for those which have tokenflows                        
//                    for (Transition t2 : orig.getTransitions()) {
//                        if (!orig.getTransits(t2).isEmpty()) { // only for those which have tokenflows                        
//                            out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + t2.getId()), t);
//                        }
//                    }
//                    // and move the active token to the first subnet
//                    out.createFlow(t, out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + 0));
//                }
//            }
            for (Transition t : orig.getTransitions()) {
                // take the active token
                out.createFlow(actO, t);
                if (!orig.getTransits(t).isEmpty()) { // if this transition has a token flow
                    // and move the active token to the first subnet
                    String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + 0;
                    if (out.containsPlace(id)) {
                        out.createFlow(t, out.getPlace(id));
                    }
                } else {
                    // directly put it back
                    out.createFlow(t, actO);
                }
            }
            // move the active token through the subnets
            for (int i = 1; i < flowFormulas.size(); i++) {
                for (Transition t : orig.getTransitions()) {
                    if (!orig.getTransits(t).isEmpty()) { // if this transition has a token flow
                        String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + (i - 1);
                        if (out.containsPlace(id)) {
                            Place acti = out.getPlace(id);
                            Place actii = out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "-" + i);
                            for (Transition tr : acti.getPostset()) {
                                out.createFlow(tr, actii);
                            }
                        }
                    }
                }
            }
//            // this is the version when every original transition has its own token
//            // give the token back to the original net (means reactivate all transitions
//            for (Transition tOrig : orig.getTransitions()) {
//                String id = ACTIVATION_PREFIX_ID + tOrig.getId() + TOKENFLOW_SUFFIX_ID + "-" + (flowFormulas.size() - 1);
//                if (out.containsPlace(id)) { // only if the transition has a token flow
//                    Place actLast = out.getPlace(id);
//                    for (Transition tr : actLast.getPostset()) {
//                        for (Transition transition : orig.getTransitions()) {
//                            if (out.containsPlace(ACTIVATION_PREFIX_ID + transition.getId())) { // only if it had a token flow
//                                out.createFlow(tr, out.getPlace(ACTIVATION_PREFIX_ID + transition.getId()));
//                            }
//                        }
//                    }
//                }
//            }
            for (Transition tOrig : orig.getTransitions()) {
                String id = ACTIVATION_PREFIX_ID + tOrig.getId() + TOKENFLOW_SUFFIX_ID + "-" + (flowFormulas.size() - 1);
                if (out.containsPlace(id)) { // only if the transition has a token flow
                    Place actLast = out.getPlace(id);
                    for (Transition tr : actLast.getPostset()) {
                        out.createFlow(tr, actO);
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
}
