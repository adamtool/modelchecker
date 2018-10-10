package uniolunisaar.adam.modelchecker.transformers;

import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.petrigame.PetriGame;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformer {

    public static String createFormula4ModelChecking4LoLA(PetriGame game, String formula) {
        for (Place p : game.getPlaces()) {
            formula = formula.replaceAll(p.getId(), p.getId() + "_tf");
        }
        return "A(G init_tfl > 0 OR " + formula + ")";
    }
}
