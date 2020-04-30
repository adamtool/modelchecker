package uniolunisaar.adam.ds.modelchecking.aba;

import uniolunisaar.adam.ds.automata.BuchiState;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAState extends BuchiState implements IABANode {

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

}
