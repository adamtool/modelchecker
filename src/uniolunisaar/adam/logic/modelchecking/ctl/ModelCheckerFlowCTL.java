package uniolunisaar.adam.logic.modelchecking.ctl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.io.renderer.impl.LoLAPNRenderer;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.CTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;
import uniolunisaar.adam.logic.externaltools.modelchecking.LoLA;
import uniolunisaar.adam.util.PNTools;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerFlowCTL {

    private final CTLLoLAModelcheckingSettings settings;

    public ModelCheckerFlowCTL(CTLLoLAModelcheckingSettings settings) {
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
    public CTLModelcheckingResult check(PetriNet net, ICTLFormula formula) throws RenderException, FileNotFoundException, NotConvertableException, ExternalToolException, InterruptedException, IOException, ProcessNotStartedException {

        // Render the net in LoLA syntax into a file
        String file = new LoLAPNRenderer().render(net);
        //net add the fairness additionally (the APT renderer don't handle this)
        for (Transition t : net.getTransitions()) {
            if (PetriNetExtensionHandler.isWeakFair(t)) {
                file = file.replaceAll("TRANSITION " + t.getId() + "\n", "TRANSITION " + t.getId() + " WEAK FAIR\n");
            }
            if (PetriNetExtensionHandler.isStrongFair(t)) {
                file = file.replaceAll("TRANSITION " + t.getId() + "\n", "TRANSITION " + t.getId() + " STRONG FAIR\n");
            }
        }
        String path = settings.getOutputPath() + ".lola";
        try (PrintStream out = new PrintStream(path)) {
            out.println(file);
        }
        PNTools.annotateProcessFamilyID(net);
        return LoLA.call(path, formula.toLoLA(), settings, PetriNetExtensionHandler.getProcessFamilyID(net));
    }

}
