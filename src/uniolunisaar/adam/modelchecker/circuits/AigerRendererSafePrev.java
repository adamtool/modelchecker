package uniolunisaar.adam.modelchecker.circuits;

import uniol.apt.adt.pn.PetriNet;

/**
 *
 * @author Manuel Gieseking
 */
public class AigerRendererSafePrev extends AigerRenderer {

    public String renderToString(PetriNet net) {
        return super.render(net).toString();
    }

}
