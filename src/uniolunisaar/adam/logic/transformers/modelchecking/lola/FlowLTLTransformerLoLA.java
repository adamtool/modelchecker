package uniolunisaar.adam.logic.transformers.modelchecking.lola;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerLoLA {

    // todo: should do the thing with the other initialization of the flow chains
    public static String createFormula4ModelChecking4LoLA(PetriNet net, String formula) {
        for (Place p : net.getPlaces()) {
            formula = formula.replaceAll(p.getId(), p.getId() + "_tf");
        }
        return "A(G init_tfl > 0 OR " + formula + ")";
    }

}
