package uniolunisaar.adam.ds.aba;

import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormula;

/**
 *
 * @author Manuel Gieseking
 */
public interface IABAEdge {

    public IPositiveBooleanFormula getPositiveBooleanFormula();

    public Successors getSuccessors();

    public String toDot();
}
