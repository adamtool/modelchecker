package uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ctl;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.logics.Constants;
import uniolunisaar.adam.ds.logics.FormulaBinary;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.IOperatorBinary;
import uniolunisaar.adam.ds.logics.ctl.CTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ctl.CTLFormula;
import uniolunisaar.adam.ds.logics.ctl.CTLOperators;
import uniolunisaar.adam.ds.logics.ctl.ICTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.FlowCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLFormula;
import uniolunisaar.adam.ds.logics.ctl.flowctl.RunCTLOperators;
import uniolunisaar.adam.ds.logics.flowlogics.IFlowFormula;
import uniolunisaar.adam.ds.logics.flowlogics.IRunFormula;
import uniolunisaar.adam.exceptions.logics.NotConvertableException;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLTransformer {

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% FLOW FORMULA PART 
    // Group: Buffermethods which does nothing but recursivly calling the replacement methods
    //          can be used when inheriting from this method to only implement those method
    //          which should really replace s.th.
    ICTLFormula replaceConstantInFlowFormula(PetriNet orig, PetriNet net, ICTLFormula phi, int nb_ff) throws NotConvertableException {
        return phi;
    }

    ICTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, CTLAtomicProposition phi, int nb_ff) throws NotConvertableException {
        return phi;
    }

    ICTLFormula replaceCTLFormulaInFlowFormula(PetriNet orig, PetriNet net, CTLFormula phi, int nb_ff) throws NotConvertableException {
        return new CTLFormula(replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff));
    }

    ICTLFormula replaceFormulaUnaryInFlowFormula(PetriNet orig, PetriNet net, FormulaUnary<ICTLFormula, CTLOperators.Unary> phi, int nb_ff) throws NotConvertableException {
        ICTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff);
        return new CTLFormula(phi.getOp(), substChildPhi);
    }

    ICTLFormula replaceFormulaBinaryInFlowFormula(PetriNet orig, PetriNet net, FormulaBinary<ICTLFormula, CTLOperators.Binary, ICTLFormula> phi, int nb_ff) throws NotConvertableException {
        ICTLFormula subst1 = replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff);
        ICTLFormula subst2 = replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff);
        return new CTLFormula(subst1, phi.getOp(), subst2);
    }

    // End Group: Buffermethods
    ICTLFormula replaceInFlowFormula(PetriNet orig, PetriNet net, ICTLFormula phi, int nb_ff) throws NotConvertableException {
        if (phi instanceof Constants) {
            return replaceConstantInFlowFormula(orig, net, phi, nb_ff);
        } else if (phi instanceof CTLAtomicProposition) {
            return replaceAtomicPropositionInFlowFormula(orig, net, (CTLAtomicProposition) phi, nb_ff);
        } else if (phi instanceof CTLFormula) {
            return replaceCTLFormulaInFlowFormula(orig, net, (CTLFormula) phi, nb_ff);
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ICTLFormula, CTLOperators.Unary> castPhi = (FormulaUnary<ICTLFormula, CTLOperators.Unary>) phi; // since we are in the scope of a FlowFormula
            return replaceFormulaUnaryInFlowFormula(orig, net, castPhi, nb_ff);
        } else if (phi instanceof FormulaBinary) {
            FormulaBinary<ICTLFormula, CTLOperators.Binary, ICTLFormula> castPhi = (FormulaBinary<ICTLFormula, CTLOperators.Binary, ICTLFormula>) phi;
            return replaceFormulaBinaryInFlowFormula(orig, net, castPhi, nb_ff);
        }
        throw new RuntimeException("The given formula '" + phi + "' is not a CTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    FlowCTLFormula replaceInFlowFormula(PetriNet orig, PetriNet net, FlowCTLFormula flowFormula, int nb_ff) throws NotConvertableException {
        ICTLFormula phi = flowFormula.getPhi();
        return new FlowCTLFormula(flowFormula.getOp(), replaceInFlowFormula(orig, net, phi, nb_ff));
    }

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% RUN FORMULA PART 
    // Group: Buffermethods which does nothing but recursivly calling the replacement methods
    //          can be used when inheriting from this method to only implement those method
    //          which should really replace s.th.
    IFormula replaceConstantsInRunFormula(PetriNet orig, PetriNet net, Constants phi, int nbFlowFormulas) throws NotConvertableException {
        return phi;
    }

    IFormula replaceCTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, CTLAtomicProposition phi, int nbFlowFormulas) throws NotConvertableException {
        return phi;
    }

    IFormula replaceFlowFormulaInRunFormula(PetriNet orig, PetriNet net, IFlowFormula phi, int nbFlowFormulas) throws NotConvertableException {
        return phi;
    }

    IFormula replaceRunFormulaInRunFormula(PetriNet orig, PetriNet net, RunCTLFormula phi, int nbFlowFormulas) throws NotConvertableException {
        return new RunCTLFormula(replaceInRunFormula(orig, net, phi.getPhi(), nbFlowFormulas));
    }

    /**
     * Attention: everything replaced in a CTL formula should be replaced as an
     * CTLFormula otherwise the cast would fail
     *
     * @param orig
     * @param net
     * @param phi
     * @param scopeEventually
     * @param nbFlowFormulas
     * @return
     */
    CTLFormula replaceCTLFormulaInRunFormula(PetriNet orig, PetriNet net, CTLFormula phi, int nbFlowFormulas) throws NotConvertableException {
        IFormula f = replaceInRunFormula(orig, net, phi.getPhi(), nbFlowFormulas);
        return new CTLFormula((ICTLFormula) f); // cast no problem since the next is replace by an CTLFormula
    }

    ICTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ICTLFormula, CTLOperators.Unary> phi, int nbFlowFormulas) throws NotConvertableException {
        ICTLFormula substChildPhi = (ICTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), nbFlowFormulas); // since castPhi is of type ICTLFormula this must result an ICTLFormula
        return new CTLFormula(phi.getOp(), substChildPhi);
    }

    IFormula replaceFormulaBinaryInRunFormula(PetriNet orig, PetriNet net, FormulaBinary<IFormula, IOperatorBinary<IFormula, IFormula>, IFormula> phi, int nbFlowFormulas) throws NotConvertableException {
        IFormula subst1 = replaceInRunFormula(orig, net, phi.getPhi1(), nbFlowFormulas);
        IFormula subst2 = replaceInRunFormula(orig, net, phi.getPhi2(), nbFlowFormulas);
        IOperatorBinary<? extends IFormula, ? extends IFormula> op = phi.getOp();
        if (phi instanceof ICTLFormula) {
            return new CTLFormula((ICTLFormula) subst1, (CTLOperators.Binary) op, (ICTLFormula) subst2);
        } else if (phi instanceof IRunFormula) {
            return new RunCTLFormula((IRunFormula) subst1, (RunCTLOperators.Binary) op, (IRunFormula) subst2);
        }
        throw new RuntimeException("The given formula '" + phi + "' is neither a CTLFormula nor FormulaUnary nor FormulaBinary. This should not be possible.");
    }
    // EndGroup: BufferMethods

    IFormula replaceInRunFormula(PetriNet orig, PetriNet net, IFormula phi, int nbFlowFormulas) throws NotConvertableException {
        if (phi instanceof Constants) {
            return replaceConstantsInRunFormula(orig, net, (Constants) phi, nbFlowFormulas);
        } else if (phi instanceof CTLAtomicProposition) {
            return replaceCTLAtomicPropositionInRunFormula(orig, net, (CTLAtomicProposition) phi, nbFlowFormulas);
        } else if (phi instanceof IFlowFormula) {
            return replaceFlowFormulaInRunFormula(orig, net, (IFlowFormula) phi, nbFlowFormulas);
        } else if (phi instanceof RunCTLFormula) {
            return replaceRunFormulaInRunFormula(orig, net, (RunCTLFormula) phi, nbFlowFormulas);
        } else if (phi instanceof CTLFormula) {
            return replaceCTLFormulaInRunFormula(orig, net, (CTLFormula) phi, nbFlowFormulas);
        } else if (phi instanceof FormulaUnary<?, ?>) {
            FormulaUnary<ICTLFormula, CTLOperators.Unary> castPhi = (FormulaUnary<ICTLFormula, CTLOperators.Unary>) phi; // Since Unary can only be ICTLFormula since IFlowFormula was already checked
            return replaceFormulaUnaryInRunFormula(orig, net, castPhi, nbFlowFormulas);
        } else if (phi instanceof FormulaBinary<?, ?, ?>) {
            return replaceFormulaBinaryInRunFormula(orig, net, (FormulaBinary<IFormula, IOperatorBinary<IFormula, IFormula>, IFormula>) phi, nbFlowFormulas);
        }
        throw new RuntimeException(
                "The given formula '" + phi + "' is not a CTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    RunCTLFormula replaceInRunFormula(PetriNet orig, PetriNet net, RunCTLFormula phi, int nbFlowFormulas) throws NotConvertableException {
        return new RunCTLFormula(replaceInRunFormula(orig, net, phi.getPhi(), nbFlowFormulas));
    }

}
