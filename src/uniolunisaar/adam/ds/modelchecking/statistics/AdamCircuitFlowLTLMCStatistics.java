package uniolunisaar.adam.ds.modelchecking.statistics;

import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;

/**
 *
 * @author Manuel Gieseking
 */
public class AdamCircuitFlowLTLMCStatistics extends AdamCircuitLTLMCStatistics {

    // input transformed
    private PetriNetWithTransits transformedNet;
    private ILTLFormula transformedFormula;
//    private long mc_nb_places;
//    private long mc_nb_transitions;
//    private long mc_size_formula;

    public AdamCircuitFlowLTLMCStatistics() {
    }

    public AdamCircuitFlowLTLMCStatistics(String path) {
        super(path);
    }

    public long getMc_nb_places() {
        if (transformedNet == null) {
            return -1;
        }
        return transformedNet.getPlaces().size();
    }

//    public void setMc_nb_places(long mc_nb_places) {
//        this.mc_nb_places = mc_nb_places;
//    }
    public long getMc_nb_transitions() {
        if (transformedNet == null) {
            return -1;
        }
        return transformedNet.getTransitions().size();
    }

//    public void setMc_nb_transitions(long mc_nb_transitions) {
//        this.mc_nb_transitions = mc_nb_transitions;
//    }
    public long getMc_size_formula() {
        if (transformedFormula == null) {
            return -1;
        }
        return transformedFormula.getSize();
    }

//    public void setMc_size_formula(long mc_size_formula) {
//        this.mc_size_formula = mc_size_formula;
//    }
    public PetriNetWithTransits getMc_net() {
        return transformedNet;
    }

    public void setMc_net(PetriNetWithTransits mc_net) {
        this.transformedNet = mc_net;
    }

    public ILTLFormula getMc_formula() {
        return transformedFormula;
    }

    public void setMc_formula(ILTLFormula mc_formula) {
        this.transformedFormula = mc_formula;
    }

    @Override
    public String getInputSizes() { // todo: when it's only LTL?
        StringBuilder sb = new StringBuilder();
        sb.append("#P, #T, #F, #Pmc, #Tmc, #Fmc, #L, #G, #Lt, #Gt, |=\n");
        sb.append("sizes:")
                .append(getIn_nb_places()).append("  &  ")
                .append(getIn_nb_transitions()).append("  &  ")
                .append(getIn_size_formula()).append("  &  ")
                .append(getMc_nb_places()).append("  &  ")
                .append(getMc_nb_transitions()).append("  &  ")
                .append(getMc_size_formula()).append("  &  ");
        if (isPrintSysCircuitSizes()) {
            sb.append(getSys_nb_latches()).append("  &  ")
                    .append(getSys_nb_gates()).append("  &  ");
        }
        sb.append(getTotal_nb_latches()).append("  &  ")
                .append(getTotal_nb_gates()).append("\n");
        sb.append("formula: ").append(getFormulaToCheck().toString());
        return sb.toString();
    }

}
