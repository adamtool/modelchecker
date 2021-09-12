package uniolunisaar.adam.logic.solver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sf.javabdd.BDD;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormulaAtom;
import uniolunisaar.adam.logic.transformers.modelchecking.positivebooleanformula.PositiveBooleanFormula2BDD;

/**
 *
 * @author Manuel Gieseking
 */
public class PositiveBooleanFormulaSolverBDD {

    public static List<List<IPositiveBooleanFormulaAtom>> solve(IPositiveBooleanFormula formula) {
        Set<IPositiveBooleanFormulaAtom> atoms = formula.getAtoms();
        // create the variable atoms mapping
        BidiMap<IPositiveBooleanFormulaAtom, Integer> map = new DualHashBidiMap<>();
        int j = 0;
        for (Iterator<IPositiveBooleanFormulaAtom> it = atoms.iterator(); it.hasNext();) {
            IPositiveBooleanFormulaAtom atom = it.next();
            map.put(atom, j++);
        }
        BDD bdd = PositiveBooleanFormula2BDD.transform(formula, map);

        List<List<IPositiveBooleanFormulaAtom>> solutions = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<byte[]> allsat = bdd.allsat();
        for (byte[] solution : allsat) {
            // required for buddy library
            if (solution == null) {
                continue;
            }
            List<List<IPositiveBooleanFormulaAtom>> currentSolutions = new ArrayList<>(); // can be more solutions in one BDD solution because of the -1
            currentSolutions.add(new ArrayList<>()); // add the first solution
            for (int i = 0; i < solution.length; i++) {
                byte b = solution[i];
                if (b == 1) {
                    for (List<IPositiveBooleanFormulaAtom> currentSolution : currentSolutions) { // add it to all solutions
                        currentSolution.add(map.getKey(i));
                    }
                } else if (b == -1) { // it is a solution this variable to be true as well as to be true
                    List< List<IPositiveBooleanFormulaAtom>> newSolutions = new ArrayList<>();
                    for (List<IPositiveBooleanFormulaAtom> currentSolution : currentSolutions) {
                        newSolutions.add(new ArrayList<>(currentSolution)); // copy the solution for the negative case
                        currentSolution.add(map.getKey(i));// add it to the solution for the positive case
                    }
                    currentSolutions.addAll(newSolutions);
                }
            }
            solutions.addAll(currentSolutions);
        }
        return solutions;
    }

}
