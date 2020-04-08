package uniolunisaar.adam.logic.modelchecking.ctl;

import java.io.FileNotFoundException;
import java.io.IOException;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.externaltools.modelchecking.LoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPNParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPNParallelInhibitor;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPNSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.PnwtAndFlowLTLtoPNSequentialInhibitor;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowCTL {

    private final FlowCTLLoLAModelcheckingSettings settings;

    public ModelCheckerFlowCTL(FlowCTLLoLAModelcheckingSettings settings) {
        this.settings = settings;
    }

    /**
     *
     * @param net
     * @param formula
     * @return
     * @throws uniol.apt.io.renderer.RenderException
     * @throws java.io.FileNotFoundException
     * @throws uniolunisaar.adam.exceptions.logics.NotConvertableException
     * @throws uniolunisaar.adam.exceptions.ExternalToolException
     * @throws java.lang.InterruptedException
     * @throws uniolunisaar.adam.exceptions.ProcessNotStartedException
     */
    public CTLModelcheckingResult check(PetriNetWithTransits net, RunCTLFormula formula) throws RenderException, FileNotFoundException, NotConvertableException, ExternalToolException, InterruptedException, IOException, ProcessNotStartedException {
        // transform the net        
        PetriNet mcNet;
        switch (settings.getApproach()) { // todo: choose extra algos dependent on number flow formulas
            case PARALLEL:
                mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
                break;
            case PARALLEL_INHIBITOR:
                mcNet = PnwtAndFlowLTLtoPNParallelInhibitor.createNet4ModelCheckingParallel(net, formula);
                break;
            case SEQUENTIAL:
                mcNet = PnwtAndFlowLTLtoPNSequential.createNet4ModelCheckingSequential(net, formula, true);
                break;
            case SEQUENTIAL_INHIBITOR:
                mcNet = PnwtAndFlowLTLtoPNSequentialInhibitor.createNet4ModelCheckingSequential(net, formula, true);
                break;
            default:
                throw new NotConvertableException("Didn't consider the approach: " + settings.getApproach().name());
        }
        // transform the formula
        ICTLFormula f=null;
        String path = ModelCheckerCTL.renderLoLAtoFile(mcNet, settings);
        return LoLA.call(path, f.toLoLA(), settings, PetriNetExtensionHandler.getProcessFamilyID(net));
    }

}
