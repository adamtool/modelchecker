package uniolunisaar.adam.ds.modelchecking.aba;

import uniolunisaar.adam.ds.automaton.BuchiState;

/**
 *
 * @author Manuel Gieseking
 */
public class ABAState extends BuchiState {

    ABAState(String id) {
        super(id);
    }

    @Override
    protected void setBuchi(boolean buchi) {
        super.setBuchi(buchi);
    }

    public String toDot() {
        StringBuilder sb = new StringBuilder();
        String color = "black";
        String shape = isBuchi() ? "doublecircle" : "circle";
        sb.append(getId().hashCode()).append("[shape=").append(shape).append(", color=").append(color);
        sb.append(", height=0.5, width=0.5, fixedsize=false,  penwidth=").append(1);
        sb.append(", label=\"").append(getId()).append("\"");
        sb.append("];\n");
        return sb.toString();
    }

}
