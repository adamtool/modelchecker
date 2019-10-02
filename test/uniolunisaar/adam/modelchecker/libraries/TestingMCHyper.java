package uniolunisaar.adam.modelchecker.libraries;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import uniol.apt.adt.pn.Place;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.renderer.RenderException;
import uniolunisaar.adam.ds.modelchecking.output.AdamCircuitLTLMCOutputData;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.logic.transformers.pn2aiger.AigerRenderer;
import uniolunisaar.adam.logic.externaltools.modelchecking.Abc.VerificationAlgo;
import uniolunisaar.adam.util.PNWTTools;
import uniolunisaar.adam.logic.transformers.pn2aiger.Circuit;
import uniolunisaar.adam.logic.transformers.modelchecking.circuit.pnandformula2aiger.CircuitAndLTLtoCircuit;
import uniolunisaar.adam.logic.modelchecking.circuits.PetriNetModelChecker;
import uniolunisaar.adam.exceptions.ExternalToolException;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
@Test
public class TestingMCHyper {

    @BeforeClass
    public void silence() {
        Logger.getInstance().setVerbose(false);
        Logger.getInstance().setShortMessageStream(null);
        Logger.getInstance().setVerboseMessageStream(null);
        Logger.getInstance().setWarningStream(null);
    }

    @Test
    void testFormulaParser() throws RenderException, InterruptedException, IOException, ProcessNotStartedException, ExternalToolException {
        PetriNetWithTransits net = new PetriNetWithTransits("testing");
        Place init = net.createPlace("inittfl");
        init.setInitialToken(1);
//        Transition t = game.createTransition("tA");
//        game.createFlow(init, t);
//        game.createFlow(t, init);
        Transition tstolen = net.createTransition("tB");
        net.createFlow(init, tstolen);
        Place out = net.createPlace("out");
        net.createFlow(tstolen, out);
//
//        Place init2 = game.createPlace("inittflA");
//        init2.setInitialToken(1);
//        
//        
        Place init3 = net.createPlace("inittflB");
        init3.setInitialToken(1);
//        
        Transition t2 = net.createTransition("tC");
        net.createFlow(init3, t2);
        net.createFlow(t2, init3);

        PNWTTools.savePnwt2PDF(net.getName(), net, true);
//        check(game, "A((G(inittfl > 0)) OR (F(out > 0)))", "./testing");
//        String formula = "Forall (F (AP \"#out#_out\" 0))";
//        String formula = "Forall (Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
//        String formula = "Forall (Implies (X (AP \"#out#_tB\" 0)) (F (AP \"#out#_out\" 0)))";
//        String formula = "Forall (G (Implies (And (Neg (AP \"#out#_tB\", 0)) (Neg (AP \"#out#_tC\", 0))) (And (Neg (AP \"#out#_inittfl\", 0)) (Neg ((AP \"#out#_inittflB\", 0))))))";
//        String formula = "Forall (G (Implies (And (Neg (AP \"#out#_tB\", 0)) (Neg (AP \"#out#_tC\", 0))) (And (Neg (AP \"#out#_inittfl\", 0)) (Neg ((AP \"#out#_inittflB\", 0))))))";
//        String formula = "Forall ("+"Implies (AP \"#out#_tB\" 0) (F (AP \"#out#_out\" 0)))";
//        String formula = "Forall ((AP \"#out#_out\" 0))"; // correct
//        String formula = "Forall (AP \"#out#_out\" 0)"; // also correct
//        String formula = "Forall (And ((AP \"#out#_out\" 0) (AP \"#out#_out\" 0)))"; // incorrect
//        String formula = "Forall (And (AP \"#out#_out\" 0) (AP \"#out#_out\" 0))"; // correct
//        String formula = "Forall (F (Eq (AP \"#out#_out\" 0) (AP \"#out#_out\" 0)))"; // correkt

        String formula = "Forall (Until (Or (AP \"#out#_out\" 0) (AP \"#out#_out\" 0)) (Or (AP \"#out#_out\" 0) (AP \"#out#_out\" 0)))";
        AigerRenderer renderer = Circuit.getRenderer(Circuit.Renderer.OUTGOING_REGISTER, net);
        AdamCircuitLTLMCOutputData data = new AdamCircuitLTLMCOutputData("./" + net.getName(), false, false);

        CircuitAndLTLtoCircuit.createCircuit(net, renderer, formula, data, null, false);

        String inputFile = "./" + net.getName() + ".aig";
        PetriNetModelChecker.check(inputFile, VerificationAlgo.IC3, net, renderer, "./" + net.getName(), "", data);
    }

    @Test
    public void regEx() {
//           StringBuilder pat = new StringBuilder("(?:^|" + linSep + "| )(?:");
////            System.out.println("%%%%%%%%%%%%%%%%%%%%%%% ASDF");
//            for (String search : replacement.keySet()) {
////                System.out.println(search);
//                pat.append(search).append("|");
//            }
////            System.out.println("%%%%%%%%%%%%%%%%%%%%%%% ASDF");
//            pat.replace(pat.length() - 1, pat.length(), ")");
////            pat.append("(?: |$)");
////            pat.append("(?: |" + linSep + ")");
//            pat.append("(?:$| |").append(linSep).append(")");
////            System.out.println(pat.toString());

        String linSep = System.lineSeparator();
        if (linSep.equals("\n")) {
//            System.out.println("asdfasfd");
        }
//        System.out.println("\\" + linSep);
        String test = "\\" + linSep;
//        System.out.println(test);
        String test1 = new String("\\" + linSep);
//        System.out.println(test1);

//        Pattern pattern = Pattern.compile("(?:^|\\s|" + System.lineSeparator() + ")(12|13|14)(?:$|\\" + System.lineSeparator() + ")");
//        Pattern pattern = Pattern.compile("(?:^|\\s)(12|13|14)(?:$|\\s)");
        Pattern pattern = Pattern.compile("(^|\\s|\\G)(12|13|14)($|\\s)");
//        System.out.println(pattern);
//        String check = "13" + linSep;
//        check += "12" + linSep;
        String check = "13\n";
        check += "12\n";
        StringBuffer sub = new StringBuffer();
        Matcher matcher = pattern.matcher(check);
//        System.out.println("check: '"+check+"'");
        while (matcher.find()) {
//            System.out.println("found");
//            System.out.println("'"+matcher.group()+"'");
//            System.out.println("finished");
//                String match = matcher.group();
//                String matchTrimmed = match.trim();
//                String pre = match.substring(0, match.indexOf(matchTrimmed));
//                String post = match.substring(pre.length() + matchTrimmed.length());
//                matcher.appendReplacement(sub, pre + replacement.get(matchTrimmed) + ((post == null) ? "" : post));
////                System.out.println(replacement.get(matchTrimmed));
////                System.out.println("for " + matchTrimmed);
//                matcher.appendReplacement(newLatches, matcher.group("A" + match + "Start") + replacement.get(match.trim()) + matcher.group("A" + match + "End"));
        }
//            matcher.appendTail(sub);
//            latches = sub.toString();
    }

}
