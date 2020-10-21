package uniolunisaar.adam.util;

import java.util.ArrayList;
import java.util.List;
import uniol.apt.adt.IGraph;
import uniol.apt.adt.exception.StructureException;
import uniol.apt.adt.pn.Flow;
import uniol.apt.adt.pn.Node;
import uniol.apt.adt.pn.PetriNet;
import uniol.apt.analysis.bounded.Bounded;
import uniol.apt.analysis.bounded.BoundedResult;
import uniol.apt.util.interrupt.UncheckedInterruptedException;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators.Binary;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators.Unary;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.exceptions.pnwt.NetNotSafeException;
import uniolunisaar.adam.exceptions.pnwt.CalculationInterruptedException;
import uniolunisaar.adam.tools.Logger;

/**
 * This class is used to check and store all preconditions of a Petri net with
 * transits to be used in the model checking approach.
 *
 * This is - whether the net is 1-bounded or not - no names of nodes which are
 * keywords in the formula appear.
 *
 * @author Manuel Gieseking
 */
public class PnwtPreconditionChecker extends PreconditionChecker {

    private BoundedResult bounded = null;
    private Boolean noFormulaKeywordsUsedAsNodeNames = null;
    private StructureException formulaKeywordsUsedAsNodeNamesException = null;

    private final List<String> formulaKeywords;

    public PnwtPreconditionChecker(PetriNetWithTransits pnwt) {
        super(pnwt);
        // add the forbidden keywords
        formulaKeywords = new ArrayList<>();

        // run operators keywords
        RunOperators.Binary[] runOps = RunOperators.Binary.values();
        for (RunOperators.Binary runOp : runOps) {
            formulaKeywords.add(runOp.toString());
            formulaKeywords.add(runOp.toSymbol());
        }
        RunOperators.Implication[] runImpOps = RunOperators.Implication.values();
        for (RunOperators.Implication runImpOp : runImpOps) {
            formulaKeywords.add(runImpOp.toString());
            formulaKeywords.add(runImpOp.toSymbol());
        }
        // flow operator keywords
        FlowLTLFormula.FlowLTLOperator[] flowOps = FlowLTLFormula.FlowLTLOperator.values();
        for (FlowLTLFormula.FlowLTLOperator flowOp : flowOps) {
            formulaKeywords.add(flowOp.toString());
            formulaKeywords.add(flowOp.toSymbol());
        }
        // ltl operator keywords
        Binary[] ltlBinOps = LTLOperators.Binary.values();
        for (Binary ltlBinOp : ltlBinOps) {
            formulaKeywords.add(ltlBinOp.toString());
            formulaKeywords.add(ltlBinOp.toSymbol());
        }
        Unary[] ltlUnaryOps = LTLOperators.Unary.values();
        for (Unary op : ltlUnaryOps) {
            formulaKeywords.add(op.toString());
            formulaKeywords.add(op.toSymbol());
        }
    }

    @Override
    public boolean check() throws NetNotSafeException, StructureException, CalculationInterruptedException {
        boolean safe = isSafe();
        if (!safe) {
            throw new NetNotSafeException(bounded.unboundedPlace.getId(), bounded.sequence.toString());
        }
        boolean noNameClash = noFormulaKeywordsUsedAsNodeNames();
        if (!noNameClash) {
            throw formulaKeywordsUsedAsNodeNamesException;
        }
        return true;
    }

    public boolean noFormulaKeywordsUsedAsNodeNames() throws StructureException, CalculationInterruptedException {
        if (noFormulaKeywordsUsedAsNodeNames == null) {
            noFormulaKeywordsUsedAsNodeNames = true;
            for (Node node : getNet().getNodes()) {
                if (Thread.interrupted()) {
                    CalculationInterruptedException e = new CalculationInterruptedException();
                    Logger.getInstance().addError(e.getMessage(), e);
                    throw e;
                }
//                InterrupterRegistry.throwIfInterruptRequestedForCurrentThread();
                if (formulaKeywords.contains(node.getId())) {
                    noFormulaKeywordsUsedAsNodeNames = false;
                    formulaKeywordsUsedAsNodeNamesException = new StructureException("The node '" + node.getId() + "' is a keyword in the syntax of the formula.");
                    throw formulaKeywordsUsedAsNodeNamesException;
                }
            }
        }
        return noFormulaKeywordsUsedAsNodeNames;
    }

    public boolean isSafe() throws CalculationInterruptedException {
        if (bounded == null) {
            try {
                bounded = Bounded.checkBounded(getNet());
            } catch (UncheckedInterruptedException ex) {
                CalculationInterruptedException e = new CalculationInterruptedException(ex.getMessage());
                Logger.getInstance().addError(e.getMessage(), e);
                throw e;
            }
        }
        return bounded.isSafe();
    }

    @Override
    public boolean changeOccurred(IGraph<PetriNet, Flow, Node> graph) {
        super.changeOccurred(graph);
        bounded = null;
        noFormulaKeywordsUsedAsNodeNames = null;
        formulaKeywordsUsedAsNodeNamesException = null;
        return true;
    }

}
