package uniolunisaar.adam.logic.transformers.modelchecking.lola;

import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Place;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitFlowLTLMCOutputData;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.transformers.modelchecking.flowltl2ltl.FlowLTLTransformerOutgoingSequential;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowLTLTransformerLoLA {
    
    public static String createFormula4ModelChecking4LoLASequential(PetriNet orig, PetriNet mc, RunLTLFormula formula) throws NotConvertableException {
        //todo: this is a hack since the method only uses it for the init and the next properties,
        // when in some point in time LoLA is able to really handle fairness, we have to add those parameters to the settings
        // we still have to ensure then that there are no transitions in the formular nor any next operator since we skip 
        // the next operator with transitions. Thus, here would be no difference between the ingoing or the outgoing 
        // semantics because LolA cannot handle transitions.
        AdamCircuitFlowLTLMCSettings settings = new AdamCircuitFlowLTLMCSettings(new AdamCircuitFlowLTLMCOutputData("./", false, false, false));
        ILTLFormula f = new FlowLTLTransformerOutgoingSequential().createFormula4ModelChecking4CircuitSequential(orig, mc, formula, settings);
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
