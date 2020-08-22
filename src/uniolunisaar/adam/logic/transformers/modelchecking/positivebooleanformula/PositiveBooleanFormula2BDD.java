package uniolunisaar.adam.logic.transformers.modelchecking.positivebooleanformula;

import java.util.Map;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import uniol.apt.adt.exception.StructureException;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormulaAtom;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanConstants;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaBinary;
import uniolunisaar.adam.ds.abta.posbooleanformula.PositiveBooleanFormulaOperators.Binary;

/**
 *
 * @author Manuel Gieseking
 */
public class PositiveBooleanFormula2BDD {

    public static BDD transform(IPositiveBooleanFormula formula, Map<IPositiveBooleanFormulaAtom, Integer> map) {
        // Initialize BDD factory        
        //"buddy", "cudd", "cal", "j", "java", "jdd", "test", "typed",
        String libName = "java";
        int nodenum = 1000000;
        int cachesize = 1000000;
        int maxIncrease = 100000000;

        BDDFactory bddfac = BDDFactory.init(libName, nodenum, cachesize);
        // todo: JavaBDDCallback is in the package bdd (if this works well make it properly)
//        // Redirect the GCSTats stream from standard System.err to Logger
//        // and the Reorder and ResizeStats from System.out to Logger
//        try {
//            Method m = JavaBDDCallback.class.getMethod("outGCStats", Integer.class, BDDFactory.GCStats.class);
//            bddfac.registerGCCallback(new JavaBDDCallback(bddfac), m);
//            m = JavaBDDCallback.class.getMethod("outReorderStats", Integer.class, BDDFactory.ReorderStats.class);
//            bddfac.registerReorderCallback(new JavaBDDCallback(bddfac), m);
//            m = JavaBDDCallback.class.getMethod("outResizeStats", Integer.class, Integer.class);
//            bddfac.registerResizeCallback(new JavaBDDCallback(bddfac), m);
//        } catch (NoSuchMethodException | SecurityException ex) {
//        }

        bddfac.setMaxIncrease(maxIncrease);
        bddfac.setVarNum(map.size());

        return createBDD(bddfac, formula, map);
    }

    private static BDD createBDD(BDDFactory bddfac, IPositiveBooleanFormula formula, Map<IPositiveBooleanFormulaAtom, Integer> map) {
        if (formula instanceof IPositiveBooleanFormulaAtom) {
            return bddfac.ithVar(map.get((IPositiveBooleanFormulaAtom) formula));
        } else if (formula instanceof PositiveBooleanConstants.True) {
            return bddfac.one();
        } else if (formula instanceof PositiveBooleanConstants.False) {
            return bddfac.zero();
        } else if (formula instanceof PositiveBooleanFormulaBinary) {
            PositiveBooleanFormulaBinary f = (PositiveBooleanFormulaBinary) formula;
            BDD phi1 = createBDD(bddfac, f.getPhi1(), map);
            BDD phi2 = createBDD(bddfac, f.getPhi2(), map);
            return f.getOp() == Binary.AND ? phi1.and(phi2) : phi1.or(phi2);
        }
        throw new StructureException("The type of the formula '" + formula + "' is not considered.");
    }
}
