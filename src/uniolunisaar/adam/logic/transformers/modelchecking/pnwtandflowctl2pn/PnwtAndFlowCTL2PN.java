package uniolunisaar.adam.logic.transformers.modelchecking.pnwtandflowctl2pn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.abta.AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.ds.automata.BuchiAutomaton;
import uniolunisaar.adam.ds.automata.BuchiState;
import uniolunisaar.adam.ds.automata.LabeledEdge;
import uniolunisaar.adam.ds.automata.NodeLabel;
import uniolunisaar.adam.ds.automata.StringLabel;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.forall.RunCTLForAllFormula;
import uniolunisaar.adam.ds.modelchecking.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.ds.modelchecking.kripkestructure.PnwtKripkeStructure;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLModelcheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.ds.petrinetwithtransits.Transit;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.modelchecking.NotTransformableException;
import uniolunisaar.adam.logic.transformers.automata.NDet2DetAutomatonTransformer;
import uniolunisaar.adam.logic.transformers.ctl.CTL2AlternatingBuchiTreeAutomaton;
import uniolunisaar.adam.logic.transformers.modelchecking.ABA2NDetTransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.abtaxkripke2aba.ABTAxKripke2ABATransformer;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2kripkestructure.Pnwt2KripkeStructureTransformer;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPNSequential.NEXT_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNNoInit.ACTIVATION_PREFIX_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNNoInit.INIT_TOKENFLOW_ID;
import static uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNNoInit.TOKENFLOW_SUFFIX_ID;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.Tools;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class PnwtAndFlowCTL2PN {

    public static final String NORMAL_MODE = "<N>";
    public static final String STUTTERING_MODE = "<S>";

    private final Map<Integer, List<Transition>> skippingTransitions = new HashMap<>();
    private final Map<Integer, List<Transition>> stutteringTransitions = new HashMap<>();

    PetriNetWithTransits createOriginalPartOfTheNet(PetriNetWithTransits orig) {
        // Copy the original net
        PetriNetWithTransits out = new PetriNetWithTransits(orig);
        out.setName(orig.getName() + "_mc");

        // delete the fairness assumption of all original transitions
        // and mark them as original
        for (Transition t : orig.getTransitions()) {
            Transition outT = out.getTransition(t.getId());
            PetriNetExtensionHandler.setOriginal(outT);
            out.removeStrongFair(outT);
            out.removeWeakFair(outT);
        }

        // mark also the places as original (for CEX)
        for (Place place : orig.getPlaces()) {
            Place outP = out.getPlace(place.getId());
            PetriNetExtensionHandler.setOriginal(outP);
        }
        return out;
    }

    /**
     *
     * @param orig
     * @param out
     * @param formula
     * @return
     */
    PetriNetWithTransits addSubNet(PetriNetWithTransits orig, PetriNetWithTransits out, ICTLFormula formula, int nb_ff, FlowCTLModelcheckingSettings settings) throws NotConvertableException, NotTransformableException, IOException, InterruptedException {
        // create alternating Buchi tree automaton of the formula
        AlternatingBuchiTreeAutomaton<Set<NodeLabel>> abta = CTL2AlternatingBuchiTreeAutomaton.transform(formula, orig);
        System.out.println("Tree Automaton: ");
        System.out.println(abta);
        // create the Kripke structure of the Petri net with transits
        // todo: add a method getTransition atoms and put this to the create function and therewith improve the construction
        PnwtKripkeStructure k = Pnwt2KripkeStructureTransformer.create(orig, true);
        if (settings.getOutputData().isVerbose()) {
            Tools.save2PDF(settings.getOutputData().getPath() + k.getName(), k);
        }
        // create the alternating Buchi product automaton
        GeneralAlternatingBuchiAutomaton aba = ABTAxKripke2ABATransformer.transform(abta, k);
        if (settings.getOutputData().isVerbose()) {
            Tools.save2PDF(settings.getOutputData().getPath() + aba.getName(), aba);
        }
        // create the nondeterministic Buchi automaton
        BuchiAutomaton nba = ABA2NDetTransformer.transform(aba, true);
        if (settings.getOutputData().isVerbose()) {
            Tools.save2PDF(settings.getOutputData().getPath() + nba.getName(), nba);
        }
        // determinize
        BuchiAutomaton det = NDet2DetAutomatonTransformer.transform(nba);
        if (settings.getOutputData().isVerbose()) {
            Tools.save2PDF(settings.getOutputData().getPath() + det.getName(), det);
        }

        // create the switch from normal to stuttering mode
        Place normal = out.createPlace(NORMAL_MODE + "_" + nb_ff);
        normal.setInitialToken(1);
        out.setPartition(normal, nb_ff + 1);
        Place stuttering = out.createPlace(STUTTERING_MODE + "_" + nb_ff);
        out.setPartition(stuttering, nb_ff + 1);
        Transition switchT = out.createTransition();
        switchT.setLabel("switch");
        out.createFlow(normal, switchT);
        out.createFlow(switchT, stuttering);

        // having the special behavior added for the given approach
        // sequential: this means the activation places of the transitions
        //              and the next transitions
        addApproachesPreWork(orig, out, formula, nb_ff);

        // create an initial place representing the chosen flow chain
        Place init = out.createPlace(INIT_TOKENFLOW_ID + "_" + nb_ff);
        init.setInitialToken(1);
        out.setPartition(init, nb_ff + 1);

        LinkedList<Place> todo = new LinkedList<>();

        // create the guessing of the in this run considered flow chain
        for (Transition t : orig.getTransitions()) {
            Transit tfl = orig.getInitialTransit(t);
            if (tfl == null) { // not initial transit -> skip
                continue;
            }
            // for all chains which are created during the time
            for (Place post : tfl.getPostset()) {
                Transition tout = out.createTransition();
//                System.out.println("LABEL: "+t.getId());
                tout.setLabel(t.getId());
                out.createFlow(init, tout);
                // do the preset connection
                connectApproachesPreset(out, tout, t.getId(), nb_ff);
                // find the corresponding initial state
                for (BuchiState initialState : det.getInitialStates()) {
                    // todo: could have saved it nicer within the states 
                    // this parsing is very unrobust against any changes
                    // in the labeling of the states
                    String[] ids = initialState.getId().split(",");
                    for (String id : ids) {
                        if (id.equals(post.getId()) || id.equals(post.getId() + "]")) {
                            String key = initialState.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                            Place pout;
                            if (out.containsPlace(key)) {
                                pout = out.getPlace(key);
                            } else {
                                pout = out.createPlace(key);
                                out.setOrigID(pout, initialState.getId());
                                out.setPartition(pout, nb_ff + 1);
                                if (initialState.isBuchi()) {
                                    out.setBuchi(pout);
                                }
                                todo.add(pout);
                            }
                            out.createFlow(tout, pout);
                        }
                    }
                }
            }
        }

        // create the states and transition of the buchi automaton
        while (!todo.isEmpty()) {
            Place pre = todo.poll();
            List<LabeledEdge<BuchiState, StringLabel>> postset = det.getPostset(out.getOrigID(pre));
            if (postset == null) {
                // that the state has no successors this corresponds to a situation with a false edge
                // i.e. the stuttering here should not be good!
                out.removeBuchi(pre);
                continue;
            }
            for (LabeledEdge<BuchiState, StringLabel> edge : postset) {
                BuchiState postState = edge.getPost();
                // create or get the postset place
                String key = postState.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
                Place post;
                if (out.containsPlace(key)) {
                    post = out.getPlace(key);
                } else {
                    post = out.createPlace(key);
                    out.setOrigID(post, postState.getId());
                    out.setPartition(post, nb_ff + 1);
                    if (postState.isBuchi()) {
                        out.setBuchi(post);
                    }
                    todo.add(post);
                }
                // create the transitions
                String transitionID = edge.getLabel().getText();
                if (transitionID.equals("-")) { // this is the transition for the stuttering mode (todo: use a key for this)
                    Transition tout = out.createTransition();
                    tout.setLabel(STUTTERING_MODE);
                    out.createFlow(pre, tout);
                    out.createFlow(tout, post);
                    // can only fire when in stuttering mode
                    Flow flow = out.createFlow(normal, tout);
                    out.setInhibitor(flow);
                    connectApproachesPresetForStuttering(out, tout, det.getPostset(edge.getPre().getId()), nb_ff);
                    List<Transition> list = stutteringTransitions.get(nb_ff);
                    if (list == null) {
                        list = new ArrayList<>();
                        stutteringTransitions.put(nb_ff, list);
                    }
                    list.add(tout);
                } else { // a normal transition
                    Transition tout = out.createTransition();
                    tout.setLabel(out.getTransition(transitionID).getId());
//                    System.out.println("LABEL : " + out.getTransition(transitionID).getId());
                    connectApproachesPreset(out, tout, transitionID, nb_ff);
                    out.createFlow(pre, tout);
                    out.createFlow(tout, post);
                    // can only fire when in normal mode
                    Flow flow = out.createFlow(stuttering, tout);
                    out.setInhibitor(flow);
                }
            }
        }

        // add the post work for this approach
        // sequential: connect the skipping transitions such that they can only
        //              fire when no other transition can fire
        addApproachesPostWork(out, det, nb_ff);

        return out;
    }

    void addApproachesPreWork(PetriNetWithTransits orig, PetriNetWithTransits out, ICTLFormula formula, int nb_ff) {
        // add an activation place for each original transition and the skipping transition
        for (Transition t : orig.getTransitions()) {
            // activation place
            String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff;
            Place act = out.createPlace(id);
            out.setPartition(act, nb_ff + 1);
            // next transition
            Transition tout = out.createTransition(t.getId() + NEXT_ID + "_" + nb_ff);
            tout.setLabel(t.getId());
            tout.putExtension("subformula", nb_ff);
            // the next transition also takes the active token
            out.createFlow(act, tout);
            List<Transition> list = skippingTransitions.get(nb_ff);
            if (list == null) {
                list = new ArrayList<>();
                skippingTransitions.put(nb_ff, list);
            }
            list.add(tout);
        }
    }

    void addApproachesPostWork(PetriNetWithTransits out, BuchiAutomaton det, int nb_ff) {
        // allow the skipping transitions only to fire when no other transition can fire and also only in the normal mode
        for (Transition skippingTransition : skippingTransitions.get(nb_ff)) {
            // add an inhibitor arc to every state which has a transition with the same label
            for (BuchiState state : det.getStates().values()) {
                boolean found = false;
                List<LabeledEdge<BuchiState, StringLabel>> postset = det.getPostset(state.getId());
                if (postset == null) {
                    continue;
                }
                for (LabeledEdge<BuchiState, StringLabel> edge : postset) {
                    if (edge.getLabel().getText().equals(skippingTransition.getLabel())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    Flow f = out.createFlow(out.getPlace(state.getId() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff), skippingTransition);
                    out.setInhibitor(f);
                }
            }
            // add an inhibitor arc to the stuttering mode
            Flow f = out.createFlow(out.getPlace(STUTTERING_MODE + "_" + nb_ff), skippingTransition);
            out.setInhibitor(f);
        }
    }

    void connectApproachesPreset(PetriNetWithTransits out, Transition tout, String id, int nb_ff) {
        out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + id + TOKENFLOW_SUFFIX_ID + "_" + nb_ff), tout);
    }

    void connectApproachesPresetForStuttering(PetriNetWithTransits out, Transition tout,
            List<LabeledEdge<BuchiState, StringLabel>> postEdges, int nb_ff) {
        // can only fire when no other transition would be firable
        // i.e. the activation places of the transition of any other successor are empty
        for (LabeledEdge<BuchiState, StringLabel> labeledEdge : postEdges) {
            if (!labeledEdge.getLabel().getText().equals("-")) {
                Flow flow = out.createFlow(out.getPlace(ACTIVATION_PREFIX_ID + labeledEdge.getLabel().getText() + TOKENFLOW_SUFFIX_ID + "_" + nb_ff), tout);
                out.setInhibitor(flow);
            }
        }
    }

    public PetriNetWithTransits createSequential(PetriNetWithTransits pnwt, RunCTLForAllFormula formula, FlowCTLModelcheckingSettings settings) throws NotConvertableException, NotTransformableException, IOException, InterruptedException {
        // create the copy of the original net
        PetriNetWithTransits out = createOriginalPartOfTheNet(pnwt);

        // create one activation place for all original transitions
        Place actO = out.createPlace(ACTIVATION_PREFIX_ID + "orig");
        actO.setInitialToken(1);

        // create the place and transitions of the subnets and already connect 
        // the transitions to their preset regarding the activation place
        List<FlowCTLFormula> flowFormulas = LogicsTools.getFlowCTLFormulas(formula);
        for (int nb_ff = 0; nb_ff < flowFormulas.size(); nb_ff++) {
            FlowCTLFormula get = flowFormulas.get(nb_ff);
            addSubNet(pnwt, out, get.getPhi(), nb_ff, settings);
        }

        if (!flowFormulas.isEmpty()) {
            // do the postsets for the transitions regarding the activation places

            // the original net (here also the preset)
            for (Transition t : pnwt.getTransitions()) {
                // take the active token
                out.createFlow(actO, t);
                String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "_" + 0;
                out.createFlow(t, out.getPlace(id));
            }

            // move the active token through the subnets
            for (int i = 1; i < flowFormulas.size(); i++) {
                for (Transition t : pnwt.getTransitions()) {
                    String id = ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "_" + (i - 1);
                    Place acti = out.getPlace(id);
                    Place actii = out.getPlace(ACTIVATION_PREFIX_ID + t.getId() + TOKENFLOW_SUFFIX_ID + "_" + i);
                    for (Flow f : acti.getPostsetEdges()) {
                        if (!out.isInhibitor(f)) {
                            out.createFlow(f.getTransition(), actii);
                        }
                    }
                }
            }

            // move it back to the original net
            for (Transition tOrig : pnwt.getTransitions()) {
                String id = ACTIVATION_PREFIX_ID + tOrig.getId() + TOKENFLOW_SUFFIX_ID + "_" + (flowFormulas.size() - 1);
                Place actLast = out.getPlace(id);
                for (Flow f : actLast.getPostsetEdges()) {
                    if (!out.isInhibitor(f)) {
                        out.createFlow(f.getTransition(), actO);
                    }
                }
            }
        } else {
            // should not really be used, since the normal model checking should be used in these cases
            Logger.getInstance().addMessage("[WARNING] No flow subformula within '" + formula.toSymbolString() + "'."
                    + " The standard CTL model checker should be used.", false);
            // but to have still some meaningful output add for all transition the
            // connections to the act places
            for (Transition t : out.getTransitions()) {
                Place act = out.getPlace(ACTIVATION_PREFIX_ID + t.getId());
                out.createFlow(t, act);
                out.createFlow(act, t);
            }
        }

        // clean up        
        // delete all token flows
        for (Transition t : pnwt.getTransitions()) {
            for (Place p : t.getPreset()) {
                out.removeTransit(p, t);
            }
            for (Place p : t.getPostset()) {
                out.removeTransit(t, p);
            }
        }
        // and the initial token flow markers
        for (Place place : pnwt.getPlaces()) {
            if (place.getInitialToken().getValue() > 0 && pnwt.isInitialTransit(place)) {
                out.removeInitialTransit(out.getPlace(place.getId()));
            }
        }

        return out;
    }

}
