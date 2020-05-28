package uniolunisaar.adam.logic.modelchecking.ltl.circuits;

import java.io.FileNotFoundException;
import uniolunisaar.adam.ds.modelchecking.results.LTLModelCheckingResult;
import java.io.IOException;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.AdamCircuitFlowLTLMCSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ltl.LoLASettings;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import uniolunisaar.adam.logic.modelchecking.ltl.lola.ModelCheckerLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.pnandformula2aiger.PnAndFlowLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndNbFlowFormulas2PNLoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.lola.FlowLTLTransformerLoLA;
import uniolunisaar.adam.util.logics.LogicsTools;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowLTL {

    private final ModelCheckingSettings settings;

    public ModelCheckerFlowLTL(ModelCheckingSettings settings) {
        this.settings = settings;
    }

    /**
     *
     * @param net
     * @param formula
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws uniol.apt.io.parser.ParseException
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     */
    public LTLModelCheckingResult check(PetriNetWithTransits net, RunLTLFormula formula) throws InterruptedException, IOException, ParseException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        switch (settings.getSolver()) {
            case ADAM_CIRCUIT:
                AdamCircuitFlowLTLMCSettings props = (AdamCircuitFlowLTLMCSettings) settings;
                PnAndFlowLTLtoCircuit.createCircuit(net, formula, props);
                props.fillAbcData(net);
                return Abc.call(props.getAbcSettings(), props.getOutputData(), props.getStatistics());
            case LOLA:
                PetriNetWithTransits mcNet = PnwtAndNbFlowFormulas2PNLoLA.createNet4ModelCheckingSequential(net, LogicsTools.getFlowLTLFormulas(formula).size());
                String f = FlowLTLTransformerLoLA.createFormula4ModelChecking4LoLASequential(net, mcNet, formula);
                try {
                    return ModelCheckerLoLA.check(mcNet, f, ((LoLASettings) settings).getOutputPath());
                } catch (RenderException | FileNotFoundException ex) {
                    throw new ExternalToolException("LoLA didn't finish correctly.", ex);
                }
            default:
                throw new UnsupportedOperationException("Solver " + settings.getSolver() + " is not supported yet.");
        }

    }
}
