package uniolunisaar.adam.logic.transformers.modelchecking.flowctl2ctl;

import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.ds.logics.Constants;
import uniolunisaar.adam.ds.logics.FormulaBinary;
import uniolunisaar.adam.ds.logics.FormulaUnary;
import uniolunisaar.adam.ds.logics.IFormula;
import uniolunisaar.adam.ds.logics.IOperatorBinary;
import uniolunisaar.adam.ds.logics.ltl.ILTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLAtomicProposition;
import uniolunisaar.adam.ds.logics.ltl.LTLFormula;
import uniolunisaar.adam.ds.logics.ltl.LTLOperators;
import uniolunisaar.adam.ds.logics.ltl.flowltl.FlowLTLFormula;
import uniolunisaar.adam.ds.logics.flowlogics.IFlowFormula;
import uniolunisaar.adam.ds.logics.flowlogics.IRunFormula;
import uniolunisaar.adam.ds.logics.ltl.flowltl.RunLTLFormula;
import uniolunisaar.adam.ds.logics.flowlogics.RunOperators;

/**
 *
 * @author Manuel Gieseking
 */
public class FlowCTLTransformer {

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% FLOW FORMULA PART 
    // Group: Buffermethods which does nothing but recursivly calling the replacement methods
    //          can be used when inheriting from this method to only implement those method
    //          which should really replace s.th.
    ILTLFormula replaceConstantInFlowFormula(PetriNet orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually) {
        return phi;
    }

    ILTLFormula replaceAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, int nb_ff, boolean scopeEventually) {
        return phi;
    }
//    ILTLFormula replaceIAtomicPropositionInFlowFormula(PetriNet orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually) {
//        return phi;
//    }

    ILTLFormula replaceLTLFormulaInFlowFormula(PetriNet orig, PetriNet net, LTLFormula phi, int nb_ff, boolean scopeEventually) {
        return new LTLFormula(replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually));
    }

    ILTLFormula replaceFormulaUnaryInFlowFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, int nb_ff, boolean scopeEventually) {
        // check if it's in the scope of an eventually
        if (!scopeEventually && phi.getOp() == LTLOperators.Unary.F) {
            scopeEventually = true;
        } else if (scopeEventually && phi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
            scopeEventually = false;
        }
        ILTLFormula substChildPhi = replaceInFlowFormula(orig, net, phi.getPhi(), nb_ff, scopeEventually);
        return new LTLFormula(phi.getOp(), substChildPhi);
    }

    ILTLFormula replaceFormulaBinaryInFlowFormula(PetriNet orig, PetriNet net, FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> phi, int nb_ff, boolean scopeEventually) {
        ILTLFormula subst1 = replaceInFlowFormula(orig, net, phi.getPhi1(), nb_ff, scopeEventually);
        ILTLFormula subst2 = replaceInFlowFormula(orig, net, phi.getPhi2(), nb_ff, scopeEventually);
        return new LTLFormula(subst1, phi.getOp(), subst2);
    }

    // End Group: Buffermethods
    ILTLFormula replaceInFlowFormula(PetriNet orig, PetriNet net, ILTLFormula phi, int nb_ff, boolean scopeEventually) {
        if (phi instanceof Constants) {
            return replaceConstantInFlowFormula(orig, net, phi, nb_ff, scopeEventually);
        } else if (phi instanceof LTLAtomicProposition) {
            return replaceAtomicPropositionInFlowFormula(orig, net, (LTLAtomicProposition) phi, nb_ff, scopeEventually);
        } else if (phi instanceof LTLFormula) {
            return replaceLTLFormulaInFlowFormula(orig, net, (LTLFormula) phi, nb_ff, scopeEventually);
        } else if (phi instanceof FormulaUnary) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // since we are in the scope of a FlowFormula
            return replaceFormulaUnaryInFlowFormula(orig, net, castPhi, nb_ff, scopeEventually);
        } else if (phi instanceof FormulaBinary) {
            FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula> castPhi = (FormulaBinary<ILTLFormula, LTLOperators.Binary, ILTLFormula>) phi;
            return replaceFormulaBinaryInFlowFormula(orig, net, castPhi, nb_ff, scopeEventually);
        }
        throw new RuntimeException("The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    FlowLTLFormula replaceInFlowFormula(PetriNet orig, PetriNet net, FlowLTLFormula flowFormula, int nb_ff) {
        ILTLFormula phi = flowFormula.getPhi();
        return new FlowLTLFormula(replaceInFlowFormula(orig, net, phi, nb_ff, false));
    }

    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% RUN FORMULA PART 
    // Group: Buffermethods which does nothing but recursivly calling the replacement methods
    //          can be used when inheriting from this method to only implement those method
    //          which should really replace s.th.
    IFormula replaceConstantsInRunFormula(PetriNet orig, PetriNet net, Constants phi, boolean scopeEventually, int nbFlowFormulas) {
        return phi;
    }

    IFormula replaceLTLAtomicPropositionInRunFormula(PetriNet orig, PetriNet net, LTLAtomicProposition phi, boolean scopeEventually, int nbFlowFormulas) {
        return phi;
    }

    IFormula replaceFlowFormulaInRunFormula(PetriNet orig, PetriNet net, IFlowFormula phi, boolean scopeEventually, int nbFlowFormulas) {
        return phi;
    }

    IFormula replaceRunFormulaInRunFormula(PetriNet orig, PetriNet net, RunLTLFormula phi, boolean scopeEventually, int nbFlowFormulas) {
        return new RunLTLFormula(replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas));
    }

    /**
     * Attention: everything replaced in an LTL formula should be replaced as an
     * LTLFormula otherwise the cast would fail
     *
     * @param orig
     * @param net
     * @param phi
     * @param scopeEventually
     * @param nbFlowFormulas
     * @return
     */
    LTLFormula replaceLTLFormulaInRunFormula(PetriNet orig, PetriNet net, LTLFormula phi, boolean scopeEventually, int nbFlowFormulas) {
        IFormula f = replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas);
        return new LTLFormula((ILTLFormula) f); // cast no problem since the next is replace by an LTLFormula
    }

    ILTLFormula replaceFormulaUnaryInRunFormula(PetriNet orig, PetriNet net, FormulaUnary<ILTLFormula, LTLOperators.Unary> phi, boolean scopeEventually, int nbFlowFormulas) {
        // check if it's in the scope of an eventually
        if (!scopeEventually && phi.getOp() == LTLOperators.Unary.F) {
            scopeEventually = true;
        } else if (scopeEventually && phi.getOp() == LTLOperators.Unary.G) { // if the last operator is a globally, then the previous eventually is not helping anymore
            scopeEventually = false;
        }

        ILTLFormula substChildPhi = (ILTLFormula) replaceInRunFormula(orig, net, phi.getPhi(), scopeEventually, nbFlowFormulas); // since castPhi is of type ILTLFormula this must result an ILTLFormula
        return new LTLFormula(phi.getOp(), substChildPhi);
    }

    IFormula replaceFormulaBinaryInRunFormula(PetriNet orig, PetriNet net, FormulaBinary<IFormula, IOperatorBinary<IFormula, IFormula>, IFormula> phi, boolean scopeEventually, int nbFlowFormulas) {
        IFormula subst1 = replaceInRunFormula(orig, net, phi.getPhi1(), scopeEventually, nbFlowFormulas);
        IFormula subst2 = replaceInRunFormula(orig, net, phi.getPhi2(), scopeEventually, nbFlowFormulas);
        IOperatorBinary<? extends IFormula, ? extends IFormula> op = phi.getOp();
        if (phi instanceof ILTLFormula) {
            return new LTLFormula((ILTLFormula) subst1, (LTLOperators.Binary) op, (ILTLFormula) subst2);
        } else if (phi instanceof IRunFormula) {
            if (op instanceof RunOperators.Binary) {
                return new RunLTLFormula((IRunFormula) subst1, (RunOperators.Binary) op, (IRunFormula) subst2);
            } else {
                return new RunLTLFormula((ILTLFormula) subst1, (RunOperators.Implication) op, (IRunFormula) subst2);
            }
        }
        throw new RuntimeException("The given formula '" + phi + "' is neither an LTLFormula nor FormulaUnary nor FormulaBinary. This should not be possible.");
    }
    // EndGroup: BufferMethods

    IFormula replaceInRunFormula(PetriNet orig, PetriNet net, IFormula phi, boolean scopeEventually, int nbFlowFormulas) {
        if (phi instanceof Constants) {
            return replaceConstantsInRunFormula(orig, net, (Constants) phi, scopeEventually, nbFlowFormulas);
        } else if (phi instanceof LTLAtomicProposition) {
            return replaceLTLAtomicPropositionInRunFormula(orig, net, (LTLAtomicProposition) phi, scopeEventually, nbFlowFormulas);
        } else if (phi instanceof IFlowFormula) {
            return replaceFlowFormulaInRunFormula(orig, net, (IFlowFormula) phi, scopeEventually, nbFlowFormulas);
        } else if (phi instanceof RunLTLFormula) {
            return replaceRunFormulaInRunFormula(orig, net, (RunLTLFormula) phi, scopeEventually, nbFlowFormulas);
        } else if (phi instanceof LTLFormula) {
            return replaceLTLFormulaInRunFormula(orig, net, (LTLFormula) phi, scopeEventually, nbFlowFormulas);
        } else if (phi instanceof FormulaUnary<?, ?>) {
            FormulaUnary<ILTLFormula, LTLOperators.Unary> castPhi = (FormulaUnary<ILTLFormula, LTLOperators.Unary>) phi; // Since Unary can only be ILTLFormula since IFlowFormula was already checked
            return replaceFormulaUnaryInRunFormula(orig, net, castPhi, scopeEventually, nbFlowFormulas);
        } else if (phi instanceof FormulaBinary<?, ?, ?>) {
            return replaceFormulaBinaryInRunFormula(orig, net, (FormulaBinary<IFormula, IOperatorBinary<IFormula, IFormula>, IFormula>) phi, scopeEventually, nbFlowFormulas);
        }
        throw new RuntimeException(
                "The given formula '" + phi + "' is not an LTLFormula or FormulaUnary or FormulaBinary. This should not be possible.");
    }

    RunLTLFormula replaceInRunFormula(PetriNet orig, PetriNet net, RunLTLFormula phi, int nbFlowFormulas) {
        return new RunLTLFormula(replaceInRunFormula(orig, net, phi.getPhi(), false, nbFlowFormulas));
    }

}
