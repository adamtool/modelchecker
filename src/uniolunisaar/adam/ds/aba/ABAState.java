package uniolunisaar.adam.ds.aba;

import java.util.HashSet;
import java.util.Set;
import uniolunisaar.adam.ds.automata.BuchiState;
import uniolunisaar.adam.ds.abta.posbooleanformula.IPositiveBooleanFormulaAtom;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAState extends BuchiState implements IABANode, IPositiveBooleanFormulaAtom {

    ABAState(String id) {
        super(id);
    }

    @Override
    protected void setBuchi(boolean buchi) {
        super.setBuchi(buchi);
    }

    @Override
    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String color = "black";
        String shape = isBuchi() ? "doublecircle" : "circle";
        sb.append(getDotIdentifier()).append("[shape=").append(shape).append(", color=").append(color);
        sb.append(", height=0.5, width=0.5, fixedsize=false,  penwidth=").append(1);
        sb.append(", label=\"").append(getId()).append("\"");
        sb.append("];\n");
        return sb.toString();
    }

    @Override
    public int getDotIdentifier() {
        return this.getId().hashCode();
    }

    @Override
    public Set<IPositiveBooleanFormulaAtom> getAtoms() {
        Set<IPositiveBooleanFormulaAtom> list = new HashSet<>();
        list.add(this);
        return list;
    }
}
