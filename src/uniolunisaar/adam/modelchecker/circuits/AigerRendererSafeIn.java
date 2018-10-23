package uniolunisaar.adam.modelchecker.circuits;

import uniol.apt.adt.pn.PetriNet;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRendererSafeIn extends AigerRenderer {

    // todo: problem with enabledness since in the ingoing the transition has
    // fired thus, do I check the enabledness for the next transition?
    public String renderToString(PetriNet net) {
        return super.render(net).toString();
    }

}
