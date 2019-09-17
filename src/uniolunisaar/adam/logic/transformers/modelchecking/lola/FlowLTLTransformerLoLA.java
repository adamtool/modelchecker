package uniolunisaar.adam.logic.transformers.modelchecking.lola;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.flowltl2ltl.FlowLTLTransformerSequential;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerLoLA {

    public static String createFormula4ModelChecking4LoLASequential(PetriNet orig, PetriNet mc, RunFormula formula) throws NotConvertableException {
        ILTLFormula f = FlowLTLTransformerSequential.createFormula4ModelChecking4CircuitSequential(orig, mc, formula, true);
        String lola = f.toLoLA();
        return "ALLPATH " + lola;
    }

    // todo: should do the thing with the other initialization of the flow chains
    public static String createFormula4ModelChecking4LoLA(PetriNet net, String formula) {
        for (Place p : net.getPlaces()) {
            formula = formula.replaceAll(p.getId(), p.getId() + "_tf"); // todo: make it save that the name must excatly match!
        }
        return "A(G init_tfl > 0 OR " + formula + ")";
    }

}
