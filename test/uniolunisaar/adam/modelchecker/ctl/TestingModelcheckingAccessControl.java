package uniolunisaar.adam.modelchecker.ctl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.parser.ParseException;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.util.Pair;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.ds.modelchecking.results.CTLModelcheckingResult;
import uniolunisaar.adam.ds.modelchecking.results.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.settings.ModelCheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.CTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.modelchecking.settings.ctl.FlowCTLLoLAModelcheckingSettings;
import uniolunisaar.adam.ds.petrinet.PetriNetExtensionHandler;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.generators.pnwt.util.acencoding.AccessControl;
import uniolunisaar.adam.logic.modelchecking.ctl.ModelCheckerCTL;
import uniolunisaar.adam.logic.modelchecking.ctl.ModelCheckerFlowCTL;
import uniolunisaar.adam.logic.parser.logics.flowctl.FlowCTLParser;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingModelcheckingAccessControl {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/ctl/";

    @BeforeClass
    public void createFolder() {
        (new File(outputDir)).mkdirs();
    }

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(true);
//        Logger.getInstance().setVerbose(false);
//        Logger.getInstance().setShortMessageStream(null);
//        Logger.getInstance().setVerboseMessageStream(null);
//        Logger.getInstance().setWarningStream(null);
    }

    @BeforeClass
    public void setProperties() {
        if (System.getProperty("examplesfolder") == null) {
            System.setProperty("examplesfolder", "examples");
        }
    }
    
    public ModelCheckerFlowCTL getModelChecker(String name) {
        FlowCTLLoLAModelcheckingSettings settings = new FlowCTLLoLAModelcheckingSettings(outputDir + name, true);
//        settings.setApproach(ModelCheckingSettings.Approach.PARALLEL_INHIBITOR);
        settings.setApproach(ModelCheckingSettings.Approach.SEQUENTIAL_INHIBITOR);
        return new ModelCheckerFlowCTL(settings);
    }

    @Test
    void testAccessControl() throws Exception {
    	String name = "officeSmall";
		Set<String> groups = new HashSet<>();
		groups.add("it");
		
		Set<String> locations = new HashSet<>();
		locations.add("out");
		locations.add("lob");
		locations.add("cor");
		locations.add("mr");
		locations.add("bur");
		
		Map<String, String> starts = new HashMap<>();
		starts.put("it", "out");
		
		Set<Pair<String, String>> connections = new HashSet<>();
		connections.add(new Pair<String, String>("out", "lob"));
		connections.add(new Pair<String, String>("lob", "out"));
		
		connections.add(new Pair<String, String>("out", "cor"));
		connections.add(new Pair<String, String>("cor", "out"));
		
		connections.add(new Pair<String, String>("lob", "cor"));
		connections.add(new Pair<String, String>("cor", "lob"));

		connections.add(new Pair<String, String>("cor", "mr"));
		connections.add(new Pair<String, String>("mr", "cor"));
		
		connections.add(new Pair<String, String>("cor", "bur"));
		connections.add(new Pair<String, String>("bur", "cor"));
		
		Map<String, Set<Pair<String, String>>> open = new HashMap<>();
		
		Set<Pair<String, String>> itOpen = new HashSet<>();
		itOpen.add(new Pair<String, String>("out", "lob"));
		itOpen.add(new Pair<String, String>("lob", "out"));
		itOpen.add(new Pair<String, String>("out", "cor"));
		itOpen.add(new Pair<String, String>("cor", "out"));
		itOpen.add(new Pair<String, String>("lob", "cor"));
		itOpen.add(new Pair<String, String>("cor", "lob"));
		itOpen.add(new Pair<String, String>("cor", "mr"));
		itOpen.add(new Pair<String, String>("mr", "cor"));
		itOpen.add(new Pair<String, String>("cor", "bur"));
		itOpen.add(new Pair<String, String>("bur", "cor"));
		open.put("it", itOpen);
		
		PetriNetWithTransits net = new AccessControl(name, groups, locations, starts, connections, open).createAccessControlExample();
        
		PNWTTools.savePnwt2PDF(outputDir + net.getName(), net, true);
		
		String witnessPath, witnessState;

		// new ExistsEventually(new AP(bu1));
		// new ExistsEventually(new AP(bu2));
		// new ExistsEventually(new AP(bu3));
		// new ExistsEventually(new AP(bu4));
		// new ExistsEventually(new AP(bu5));
		// new ExistsEventually(new AP(bu7));
		// new ExistsEventually(new AP(lob));
		// new ExistsEventually(new AP(ser));
		// new ForallGlobally(new Not(new AP(pos)));
		// new ForallGlobally(new Not(new AP(buS)));
		
		ModelCheckerFlowCTL mc = getModelChecker(net.getName());

        RunCTLFormula formula = FlowCTLParser.parse(net, "𝔸EF itATbur");
        CTLModelcheckingResult result = mc.check(net, formula);
//        Logger.getInstance().addMessage("ERROR:");
//        Logger.getInstance().addMessage(result.getLolaError());
//        Logger.getInstance().addMessage("OUTPUT:");
//        Logger.getInstance().addMessage(result.getLolaOutput());
        Logger.getInstance().addMessage("Sat: " + result.getSatisfied().name(), true);
        witnessState = result.getWitnessState();
        if (witnessState != null) {
            Logger.getInstance().addMessage("Witness state: " + witnessState, true);
        }
        witnessPath = result.getWitnessPath();
        if (witnessPath != null) {
            Logger.getInstance().addMessage("Witness path: " + witnessPath, true);
        }
        Assert.assertEquals(result.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);
	}
}
