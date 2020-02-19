package uniolunisaar.adam.util.mc.sdn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import uniol.apt.adt.pn.Marking;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.SDNTools;
import uniolunisaar.adam.util.logics.FormulaCreator;

/**
 *
 * @author Manuel Gieseking
 */
public class SDNFormelCreator {

    public static RunFormula createConnectivity(PetriNetWithTransits pnwt) {
        List<ILTLFormula> egresses = new ArrayList<>();
        for (Place place : pnwt.getPlaces()) {
            if (place.hasExtension(SDNTools.egressExtension)) {
                egresses.add(new LTLAtomicProposition(place));
            }
        }
        return new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, FormulaCreator.bigWedgeOrVeeObject(egresses, false)));
    }

    public static RunFormula createLoopFreedom(PetriNetWithTransits pnwt, boolean weak) {
        List<ILTLFormula> sws = new ArrayList<>();
        for (Place place : pnwt.getPlaces()) {
            if (place.hasExtension(SDNTools.switchExtension) && !place.hasExtension(SDNTools.egressExtension)) {
                sws.add(new LTLFormula(new LTLAtomicProposition(place), LTLOperators.Binary.IMP,
                        new LTLFormula(new LTLAtomicProposition(place), LTLOperators.Binary.U,
                                new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(place))))));
            }
        }
        LTLFormula f = new LTLFormula(LTLOperators.Unary.G, FormulaCreator.bigWedgeOrVeeObject(sws, true));
        if (!weak) {
            return new RunFormula(FlowFormula.FlowOperator.A, f);
        } else {
            return new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.F, f));
        }
    }

    public static RunFormula createDropFreedom(PetriNetWithTransits pnwt) {
        List<ILTLFormula> egrs = new ArrayList<>();
        for (Place place : pnwt.getPlaces()) {
            if (place.hasExtension(SDNTools.egressExtension)) {
                egrs.add(new LTLFormula(LTLOperators.Unary.NEG, new LTLAtomicProposition(place)));
            }
        }
        List<ILTLFormula> fwds = new ArrayList<>();
        for (Transition t : pnwt.getTransitions()) {
            if (t.hasExtension(SDNTools.fwdExtension)) {
                fwds.add(new LTLAtomicProposition(t));
            }
        }
        return new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(LTLOperators.Unary.G,
                new LTLFormula(FormulaCreator.bigWedgeOrVeeObject(egrs, true),
                        LTLOperators.Binary.IMP,
                        FormulaCreator.bigWedgeOrVeeObject(fwds, false))));
    }

    private static Set<Place> getPath(PetriNetWithTransits pnwt, Marking m) {
        Set<Place> path = new HashSet<>();
        for (Transition t : pnwt.getTransitions()) {
            if (t.hasExtension(SDNTools.fwdExtension)) {
                Set<Place> sws = new HashSet<>();
                boolean isActive = false;
                for (Place place : t.getPreset()) {
                    if (place.getId().contains(SDNTools.infixActPlace)) {
                        if (m.getToken(place).getValue() > 0) {
                            isActive = true;
                        }
                    } else {
                        sws.add(place);
                    }
                }
                if (isActive) {
                    path.addAll(sws);
                }
            }
        }
        return path;
    }

    public static RunFormula createPackageCoherence(PetriNetWithTransits pnwt) {
        // Get Path_1
        // all places connected to a fwd transition with an initially 
        // marked activation place
        Set<Place> path = getPath(pnwt, pnwt.getInitialMarking());
        List<ILTLFormula> path1 = new ArrayList<>();
        for (Place place : path) {
            path1.add(new LTLAtomicProposition(place));
        }

        // Get Path_2
        // the switches as for Path_1 but after all updates have fired 
        Place init = pnwt.getPlace(SDNTools.updateStartID);
        Marking m = pnwt.getInitialMarking();
        // Search and fire the transitions in breadth-first order
        Queue<Place> todo = new LinkedList<>();
        todo.add(init);
        while (!todo.isEmpty()) {
            Place start = todo.poll();
            Iterator<Transition> post = start.getPostset().iterator();
            if (!post.hasNext()) { // this subupdate finished
                continue;
            }
            Transition t = post.next(); // each update place can only have at most one successor transition
            if (!t.isFireable(m)) {
                // this is only possible for a merge transition of concurrent update, which cannot yet fire because not every
                // sequential update has finished. Just jump over this place, the last sequential update which activates
                // a place of a preset of the transition will enable the firing
                continue;
            }
            m = m.fireTransitions(t);
            for (Place place : t.getPostset()) {
                if (!place.getId().contains(SDNTools.infixActPlace)) {
                    todo.add(place);
                }
            }
        }
        // get the now activated switches
        path = getPath(pnwt, m);
        List<ILTLFormula> path2 = new ArrayList<>();
        for (Place place : path) {
            path2.add(new LTLAtomicProposition(place));
        }
        return new RunFormula(FlowFormula.FlowOperator.A, new LTLFormula(
                new LTLFormula(LTLOperators.Unary.G, FormulaCreator.bigWedgeOrVeeObject(path1, false)),
                LTLOperators.Binary.OR,
                new LTLFormula(LTLOperators.Unary.G, FormulaCreator.bigWedgeOrVeeObject(path2, false))));
    }
}
