package uniolunisaar.adam.modelchecker.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import org.apache.commons.io.IOUtils;
import uniol.apt.adt.pn.PetriNet;
import uniolunisaar.adam.modelchecker.circuits.AigerRenderer;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.Logger;

/**
 *
 * @author Manuel Gieseking
 */
public class ModelCheckerTools {

    public static void save2Aiger(PetriNet net, String path) throws FileNotFoundException {
        String aigerFile = AigerRenderer.render(net);
        // save aiger file
        try (PrintStream out = new PrintStream(path + ".aiger")) {
            out.println(aigerFile);
        }
    }

    public static void save2AigerAndDot(PetriNet net, String path) throws FileNotFoundException, IOException, InterruptedException {
        save2Aiger(net, path);
//        String command = AdamProperties.getInstance().getLibFolder() + "/aigtodot";
//        Logger.getInstance().addMessage(command, true);
//        ProcessBuilder procBuilder = new ProcessBuilder(command, path + ".aiger", path + "_circuit.dot");
//        Process proc = procBuilder.start();
//        proc.waitFor();
//        String error = IOUtils.toString(proc.getErrorStream());
//        Logger.getInstance().addMessage(error, true);
//        String output = IOUtils.toString(proc.getInputStream());
//        Logger.getInstance().addMessage(output, true);
    }

    public static void save2AigerAndDotAndPdf(PetriNet net, String path) throws FileNotFoundException, IOException, InterruptedException {
//        save2AigerAndDot(net, path);
//        // show circuit
//        String dotCommand = "dot -Tpdf " + path + "_circuit.dot -o " + path + "_circuit.pdf";
//        Logger.getInstance().addMessage(dotCommand, true);
//        ProcessBuilder procBuilder = new ProcessBuilder("dot", "-Tpdf", path + "_circuit.dot", "-o", path + "_circuit.pdf");
//        Process proc = procBuilder.start();
//        proc.waitFor();
//        String error = IOUtils.toString(proc.getErrorStream());
//        Logger.getInstance().addMessage(error, true);
//        String output = IOUtils.toString(proc.getInputStream());
//        Logger.getInstance().addMessage(output, true);
    }

    public static void save2AigerAndPdf(PetriNet net, String path) throws FileNotFoundException, IOException, InterruptedException {
//        String bufferpath = path + "_" + System.currentTimeMillis();
//        save2AigerAndDotAndPdf(net, bufferpath);
//        // Delete dot file
//        new File(bufferpath + "_circuit.dot").delete();
//        Logger.getInstance().addMessage("Deleted: " + bufferpath + "_circuit.dot", true);
//        // move to original name
//        Files.move(new File(bufferpath + "_circuit.pdf").toPath(), new File(path + "_circuit.pdf").toPath(), REPLACE_EXISTING);
//        Logger.getInstance().addMessage("Moved: " + bufferpath + "_circuit.pdf --> " + path + "_circuit.pdf", true);
//        Files.move(new File(bufferpath + ".aiger").toPath(), new File(path + ".aag").toPath(), REPLACE_EXISTING);
//        Logger.getInstance().addMessage("Moved: " + bufferpath + ".aiger --> " + path + ".aag", true);
    }
}
