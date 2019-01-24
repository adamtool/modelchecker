package uniolunisaar.adam.modelchecker.circuits;

import java.io.File;
import java.io.IOException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.io.parser.ParseException;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunFormula;
import uniolunisaar.adam.ds.modelchecking.ModelCheckingResult;
import uniolunisaar.adam.ds.modelchecking.ModelcheckingStatistics;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exception.logics.NotConvertableException;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.generators.pnwt.SmartFactory;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc;
import static uniolunisaar.adam.logic.externaltools.modelchecking.Abc.LOGGER_ABC_OUT;
import uniolunisaar.adam.logic.modelchecking.circuits.ModelCheckerFlowLTL;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndFlowLTLtoCircuit;
import uniolunisaar.adam.logic.transformers.pnandformula2aiger.PnAndLTLtoCircuit;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.ProcessNotStartedException;

/**
 *
 * @author Manuel Gieseking
 */
public class TestingSmartFactory {

    private static final String outputDir = System.getProperty("testoutputfolder") + "/";
    private static final String outputDirInCircuit = outputDir + "sequential/max_in_circuit/smartFactory/";
    private static final String outputDirInFormula = outputDir + "sequential/max_in_formula/smartFactory/";

    @BeforeClass
    public void createFolder() {
        Logger.getInstance().setVerbose(true);
        Logger.getInstance().addMessageStream(LOGGER_ABC_OUT, System.out);

        (new File(outputDirInCircuit)).mkdirs();
        (new File(outputDirInFormula)).mkdirs();
    }

    @Test
    public void testSmartFactoryFixed() throws ParseException, InterruptedException, IOException, NotConvertableException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = SmartFactory.createMillingDrillingDeburringValidationExample(false);

        RunFormula f;
        ModelCheckingResult ret;
        String name;

        // All workpieces had been milled, deburred, and validated
        LTLAtomicProposition milling = new LTLAtomicProposition(net.getPlace("m_w"));
        LTLAtomicProposition deburring = new LTLAtomicProposition(net.getPlace("db_w"));
        LTLAtomicProposition validating = new LTLAtomicProposition(net.getPlace("vA_w"));
        RunFormula allMilledDeburredValidated = new RunFormula(new FlowFormula(
                new LTLFormula(
                        new LTLFormula(
                                new LTLFormula(LTLOperators.Unary.F, milling),
                                LTLOperators.Binary.AND,
                                new LTLFormula(LTLOperators.Unary.F, deburring)
                        ),
                        LTLOperators.Binary.AND,
                        new LTLFormula(LTLOperators.Unary.F, validating)
                )
        ));
        // all typeA are drilled with holes and never with T-slots 
        LTLAtomicProposition start_A = new LTLAtomicProposition(net.getPlace("sm_0"));
        LTLAtomicProposition drillingH = new LTLAtomicProposition(net.getPlace("dH_w"));
        LTLAtomicProposition drillingT = new LTLAtomicProposition(net.getPlace("dT_w"));
        RunFormula typeACorrect = new RunFormula(new FlowFormula(
                new LTLFormula(start_A, LTLOperators.Binary.IMP,
                        new LTLFormula(new LTLFormula(LTLOperators.Unary.F, drillingH),
                                LTLOperators.Binary.AND,
                                new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, drillingT))
                        )
                )));
        // for typeB the other way round
        LTLAtomicProposition start_B = new LTLAtomicProposition(net.getPlace("sm_1"));
        RunFormula typeBCorrect = new RunFormula(new FlowFormula(
                new LTLFormula(start_B, LTLOperators.Binary.IMP,
                        new LTLFormula(new LTLFormula(LTLOperators.Unary.F, drillingT),
                                LTLOperators.Binary.AND,
                                new LTLFormula(LTLOperators.Unary.G, new LTLFormula(LTLOperators.Unary.NEG, drillingH))
                        )
                )));
        // all process steps had been done in the correct order
//        RunFormula correctOrder = new RunFormula(new FlowFormula(
//                new LTLFormula(
//                new LTLFormula( // all not
//                        new LTLFormula(LTLOperators.Unary.NEG, drillingH),
//                        LTLOperators.Binary.AND,
//                        new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, drillingT),
//                                LTLOperators.Binary.AND,
//                                new LTLFormula(new LTLFormula(LTLOperators.Unary.NEG, deburring),
//                                        LTLOperators.Binary.AND,
//                                        new LTLFormula(LTLOperators.Unary.NEG, validating)
//                                )
//                        )
//                ),
//                        LTLOperators.Binary.U, // until milling
//                        new LTLFormula(milling, LTLOperators.Binary.AND,
//                                
//                                
//                                
//                                
//                                
//                                )                       
//                        
//        ));

        f = allMilledDeburredValidated;
        name = net.getName() + "_" + f.toString().replace(" ", "");

        // maximality in circuit
        ModelCheckerFlowLTL mc = new ModelCheckerFlowLTL(
                PnAndLTLtoCircuit.TransitionSemantics.OUTGOING,
                PnAndFlowLTLtoCircuit.Approach.SEQUENTIAL,
                PnAndLTLtoCircuit.Maximality.MAX_NONE,
                PnAndLTLtoCircuit.Stuttering.PREFIX_REGISTER,
                Abc.VerificationAlgo.IC3,
                true);
        ModelcheckingStatistics stats = new ModelcheckingStatistics();
        ret = mc.check(net, f, outputDirInCircuit + name, true, stats);
        Assert.assertEquals(ret.getSatisfied(), ModelCheckingResult.Satisfied.TRUE);

        System.out.println(stats.toString());
    }
}
