package uniolunisaar.adam.modelchecker.positivebooleanformula;

import java.util.List;
import org.testng.annotations.Test;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormulaAtom;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaFactory;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators;
import uniolunisaar.adam.ds.modelchecking.aba.ABAState;
import uniolunisaar.adam.ds.modelchecking.aba.GeneralAlternatingBuchiAutomaton;
import uniolunisaar.adam.logic.solver.PositiveBooleanFormulaSolverBDD;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestPositiveBooleanFormulaBDDSolver {

    @Test
    public void firstTest() {
        GeneralAlternatingBuchiAutomaton aba = new GeneralAlternatingBuchiAutomaton("asdf");
        ABAState x1 = aba.createAndAddState("x1");
        ABAState x2 = aba.createAndAddState("x2");
        ABAState x3 = aba.createAndAddState("x3");
        IPositiveBooleanFormula f = PositiveBooleanFormulaFactory.createBinaryFormula(
                PositiveBooleanFormulaFactory.createBinaryFormula(x1, PositiveBooleanFormulaOperators.Binary.AND, x2),
                PositiveBooleanFormulaOperators.Binary.OR,
                x3);
        List<List<IPositiveBooleanFormulaAtom>> solutions = PositiveBooleanFormulaSolverBDD.solve(f);
//        System.out.println(f.toString());
//        System.out.println(solutions.toString());

        f = PositiveBooleanFormulaFactory.createBinaryFormula(x1, PositiveBooleanFormulaOperators.Binary.AND, x2);
        solutions = PositiveBooleanFormulaSolverBDD.solve(f);
//        System.out.println(f.toString());
//        System.out.println(solutions.toString());

    }
}
