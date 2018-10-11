package uniolunisaar.adam.modelchecker.transformers;

import java.util.ArrayList;
import java.util.Collection;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.petrigame.PetriGame;
import uniolunisaar.adam.logic.flowltl.AtomicProposition;
import uniolunisaar.adam.logic.flowltl.ILTLFormula;
import uniolunisaar.adam.logic.flowltl.IRunFormula;
import uniolunisaar.adam.logic.flowltl.LTLFormula;
import uniolunisaar.adam.logic.flowltl.LTLOperators;
import uniolunisaar.adam.logic.flowltlparser.FlowLTLParser;
import uniolunisaar.adam.logic.util.FormulaCreator;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformer {

    public static String toMCHyperFormat(PetriNet net, String formula) throws ParseException {
        // convert to prefix
        formula = FlowLTLParser.parse(net, formula).toPrefixString();
        System.out.println(formula);
        formula = formula.replace("IMP(", "(Implies ");
        formula = formula.replace("AND(", "(And ");
        formula = formula.replace("OR(", "(Or ");
        formula = formula.replace("NEG(", "(Neg ");
        formula = formula.replace("G(", "(G ");
        formula = formula.replace("F(", "(F ");
        formula = formula.replace(",", " ");

        for (Place p : net.getPlaces()) {
            formula = formula.replace(" " + p.getId() + " ", "@#" + p.getId() + "#@ "); //todo; make it better with regular expressions
            formula = formula.replace(" " + p.getId() + ")", "@#" + p.getId() + "#@)");
        }
        for (Transition t : net.getTransitions()) {
            formula = formula.replace(" " + t.getId() + " ", "@#" + t.getId() + "#@ ");//todo; make it better with regular expressions
            formula = formula.replace(" " + t.getId() + ")", "@#" + t.getId() + "#@)");
        }
        for (Place p : net.getPlaces()) {
            formula = formula.replace("@#" + p.getId() + "#@", "(AP \"#out#_" + p.getId() + "\" 0)");
        }
        for (Transition t : net.getTransitions()) {
            formula = formula.replace("@#" + t.getId() + "#@", "(AP \"#out#_" + t.getId() + "\" 0)");
        }

        formula = "Forall " + formula;
        return formula;
    }

    public static IRunFormula getMaximaliltyStandardObject(PetriNet net) {
        String formula = getMaximaliltyStandard(net);
        try {
            return FlowLTLParser.parse(net, formula);
        } catch (ParseException ex) {
//            ex.printStackTrace();
            // Cannot happen
            return null;
        }
    }

    public static String getMaximaliltyStandard(PetriNet net) {
        StringBuilder sb = new StringBuilder("G ((");

        // big wedge no transition fires
        Collection<String> elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            elements.add("!(" + t.getId() + ")");
        }
        sb.append(FormulaCreator.bigWedgeOrVee(elements, true));

        // implies
        sb.append(" -> ");

        // big wedge no transition is enabled
        elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            elements.add("!(" + FormulaCreator.enabled(t) + ")");
        }
        sb.append(FormulaCreator.bigWedgeOrVee(elements, true));

        // closing implies and globally
        sb.append("))");

        return sb.toString();
    }

    public static IRunFormula getMaximaliltyReisigObject(PetriNet net) {
        String formula = getMaximaliltyReisig(net);
        try {
            return FlowLTLParser.parse(net, formula);
        } catch (ParseException ex) {
            System.out.println(formula);
            ex.printStackTrace();
            // Cannot happen
            return null;
        }
    }

    public static String getMaximaliltyReisig(PetriNet net) {
        // all transitions have to globally be eventually not enabled or another transition with a place in the transitions preset fires
        Collection<String> elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            StringBuilder sb = new StringBuilder("G(F(");
            sb.append("(!(").append(FormulaCreator.enabled(t)).append(") OR ");
            Collection<String> elems = new ArrayList<>();
            for (Place p : t.getPreset()) {
                for (Transition t2 : p.getPostset()) {
                    elems.add(t2.getId());
                }
            }
            sb.append(FormulaCreator.bigWedgeOrVee(elems, false));
            sb.append(")))");
            elements.add(sb.toString());
        }
        return FormulaCreator.bigWedgeOrVee(elements, true);
    }

    public static ILTLFormula getMaximaliltyReisigDirectAsObject(PetriNet net) {
        // all transitions have to globally be eventually not enabled or another transition with a place in the transitions preset fires
        Collection<ILTLFormula> elements = new ArrayList<>();
        for (Transition t : net.getTransitions()) {
            Collection<ILTLFormula> elems = new ArrayList<>();
            for (Place p : t.getPreset()) {
                for (Transition t2 : p.getPostset()) {
                    elems.add(new AtomicProposition(t2));
                }
            }
            ILTLFormula bigvee = FormulaCreator.bigWedgeOrVeeObject(elems, false);
            ILTLFormula f = new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, FormulaCreator.enabledObject(t)), LTLOperators.Binary.OR, bigvee);
            f = new LTLFormula(LTLOperators.Unary.F, f);
            f = new LTLFormula(LTLOperators.Unary.G, f);
            elements.add(f);
        }
        return FormulaCreator.bigWedgeOrVeeObject(elements, true);
    }

    public static String createFormula4ModelChecking4LoLA(PetriGame game, String formula) {
        for (Place p : game.getPlaces()) {
            formula = formula.replaceAll(p.getId(), p.getId() + "_tf");
        }
        return "A(G init_tfl > 0 OR " + formula + ")";
    }
}
