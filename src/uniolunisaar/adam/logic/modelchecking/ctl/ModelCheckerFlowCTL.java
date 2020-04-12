package uniolunisaar.adam.logic.modelchecking.ctl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import uniol.apt.adt.pn.Place;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.externaltools.modelchecking.LoLA;
import uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ctl.FlowCTLTransformerParallel;
import uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ctl.FlowCTLTransformerSequential;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNParInhibitorNoInit;
import uniolunisaar.adam.logic.transformers.modelchecking.pnwt2pn.withoutinittflplaces.PnwtAndFlowCTLStarToPNSeqInhibitorNoInit;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.util.logics.LogicsTools;

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
        List<FlowCTLFormula> flowFormulas = LogicsTools.getFlowCTLFormulas(formula);
        // transform the net and the formula
        PetriNetWithTransits mcNet;
        ICTLFormula f = null;
        switch (settings.getApproach()) { // todo: choose extra algos dependent on number flow formulas
            case PARALLEL:
                throw new RuntimeException("Not yet implemented");
//                mcNet = PnwtAndFlowLTLtoPNParallel.createNet4ModelCheckingParallelOneFlowFormula(net);
//                f = new FlowCTLTransformerParallel().createFormula4ModelChecking4CircuitParallel(net, mcNet, formula);
//                break;
            case PARALLEL_INHIBITOR:
                mcNet = PnwtAndFlowCTLStarToPNParInhibitorNoInit.createNet4ModelCheckingParallel(net, formula, flowFormulas.size());
                f = new FlowCTLTransformerParallel().createFormula4ModelChecking4CircuitParallel(net, mcNet, formula);
                break;
            case SEQUENTIAL:
                throw new RuntimeException("Not yet implemented");
//                mcNet = PnwtAndFlowLTLtoPNSequential.createNet4ModelCheckingSequential(net, formula, true);
//                f = new FlowCTLTransformerSequential().createFormula4ModelChecking4CircuitParallel(net, mcNet, formula);
//                break;
            case SEQUENTIAL_INHIBITOR:
                mcNet = PnwtAndFlowCTLStarToPNSeqInhibitorNoInit.createNet4ModelCheckingSequential(net, formula, flowFormulas.size());
                f = new FlowCTLTransformerSequential().createFormula4ModelChecking4CircuitSequential(net, mcNet, formula);
                break;
            default:
                throw new NotConvertableException("Didn't consider the approach: " + settings.getApproach().name());
        }
        if (settings.isVerbose()) { // todo: should add an additional flag
            Logger.getInstance().addMessage(f.toSymbolString(), true);
            // color all original places
            for (Place p : mcNet.getPlaces()) {
                if (!mcNet.hasPartition(p)) {
                    mcNet.setPartition(p, 0);
                }
            }
            try {
                PNWTTools.saveAPT(settings.getOutputPath() + "_mc", mcNet, false);
            } catch (RenderException | FileNotFoundException ex) {
            }
            PNWTTools.savePnwt2PDF(settings.getOutputPath() + "_mc", mcNet, true, flowFormulas.size() + 1);
        }
        String path = ModelCheckerCTL.renderLoLAtoFile(mcNet, settings);
        return LoLA.call(path, f.toLoLA(), settings, PetriNetExtensionHandler.getProcessFamilyID(net));
    }

}
