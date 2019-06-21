package uniolunisaar.adam.logic.modelchecking.lola;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import uniol.apt.adt.pn.Transition;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.io.renderer.impl.LoLAPNRenderer;
import uniolunisaar.adam.ds.petrinetwithtransits.PetriNetWithTransits;
import uniolunisaar.adam.tools.IOUtils;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerLoLA {

    public static boolean check(PetriNetWithTransits pn, String formula, String path) throws RenderException, InterruptedException, FileNotFoundException, IOException {
        String file = new LoLAPNRenderer().render(pn);

        for (Transition t : pn.getTransitions()) {
            if (pn.isStrongFair(t)) {
                file = file.replaceAll("TRANSITION " + t.getId() + "\n", "TRANSITION " + t.getId() + " STRONG FAIR\n");
            }
        }

//        file = file.replaceAll("TRANSITION\\s(.+)", "TRANSITION $1 STRONG FAIR");
//        System.out.println(file);
        try (PrintStream out = new PrintStream(path + ".lola")) {
            out.println(file);
        }
//        Runtime rt = Runtime.getRuntime();
//        String exString = "lola '" + path + ".lola' --formula='" + formula + "' --quiet --state='" + path + "_witness_state.out' --path='" + path + "_witness_path.out'";
//        String exString = "lola '" + path + ".lola' --formula='" + formula +"' > " + path + ".out";
//        String exString = "lola '" + path + ".lola' --formula='" + formula + "'";
//        System.out.println(exString);
//        Process p = rt.exec(exString);
//        p.waitFor();
//        System.out.println(p.exitValue());

        final String json = path + ".json";
        final String state = path + "_witness_state.txt";
        final String witness_path = path + "_witness_path.txt";
        final String lola = "lola";
        final String argFile = path + ".lola";
        final String argFormula = "--formula=" + formula;
        final String argOutput = "--json=" + json;
        final String argState = "--state=" + state;
        final String argPath = "--path=" + witness_path;

        Logger.getInstance().addMessage("Checking with LoLA ...", true);
        Logger.getInstance().addMessage(lola + " " + argFile + " " + "--formula='" + formula + "' " + argOutput + " " + argState + " " + argPath, true);
//        ProcessBuilder procBuilder = new ProcessBuilder("lola", path + ".lola", "--formula=" + formula + "");
        ProcessBuilder procBuilder = new ProcessBuilder(lola, argFile, argFormula, argOutput, argState, argPath);
//        procBuilder.inheritIO();
        procBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        procBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

        Process proc = procBuilder.start();
        proc.waitFor();

        String error = IOUtils.streamToString(proc.getErrorStream());
//        Logger.getInstance().addMessage(procBuilder.command().toString().replaceAll(",|a|l", ""), true);
        Logger.getInstance().addMessage(error, true);

        String result;
        try (FileInputStream inputStream = new FileInputStream(json)) {
            String output = IOUtils.streamToString(inputStream);
            int start_idx = output.indexOf("result");
            int endA_idx = output.indexOf('}', start_idx);
            int endB_idx = output.indexOf(',', start_idx);
            int end_idx = (endA_idx < endB_idx) ? endA_idx : endB_idx;

            result = output.substring(start_idx + 9, end_idx);
            Logger.getInstance().addMessage("LoLA says: " + result);
//            System.out.println(output);
        }
        try (FileInputStream inputStream = new FileInputStream(state)) {
            Logger.getInstance().addMessage("Witness state: " + IOUtils.streamToString(inputStream), true);
        }
        try (FileInputStream inputStream = new FileInputStream(witness_path)) {
            Logger.getInstance().addMessage("Witness path: " + IOUtils.streamToString(inputStream), true);
        }

        //        System.out.println(builder.toString());
        System.out.println("return " + proc.exitValue());
        ProcessBuilder rm = new ProcessBuilder("rm", json, state, witness_path);
        rm.start();
        Logger.getInstance().addMessage(rm.command().toString(), true);
        return result.equals("true");
//        Logger.getInstance().addMessage(file);
    }
}
