package uniolunisaar.adam.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import uniolunisaar.adam.exceptions.ProcessNotStartedException;
import uniolunisaar.adam.tools.AdamProperties;
import uniolunisaar.adam.tools.Logger;
import uniolunisaar.adam.tools.processHandling.ExternalProcessHandler;
import uniolunisaar.adam.tools.processHandling.ProcessPool;

/**
 *
 * @author Manuel Gieseking
 */
public class MCTools {

    public static void save2Dot(String path, DotSaveable object) throws FileNotFoundException {
        try (PrintStream out = new PrintStream(path + ".dot")) {
            out.println(object.toDot());
        }
        Logger.getInstance().addMessage("Saved to: " + path + ".dot", true);
    }

    public static Thread save2DotAndPDF(String path, DotSaveable object) throws IOException, InterruptedException {
        save2Dot(path, object);
        String dot = AdamProperties.getInstance().getProperty(AdamProperties.DOT);
        String[] command = {dot, "-Tpdf", path + ".dot", "-o", path + ".pdf"};
        ExternalProcessHandler procH = new ExternalProcessHandler(true, command);
        ProcessPool.getInstance().putProcess(object.hashCode() + "#dot", procH);
        // start it in an extra thread
        Thread thread = new Thread(() -> {
            try {
                procH.startAndWaitFor();
                Logger.getInstance().addMessage("Saved to: " + path + ".pdf", true);
//                    if (deleteDot) {
//                        // Delete dot file
//                        new File(path + ".dot").delete();
//                        Logger.getInstance().addMessage("Deleted: " + path + ".dot", true);
//                    }
            } catch (IOException | InterruptedException ex) {
                String errors = "";
                try {
                    errors = procH.getErrors();
                } catch (ProcessNotStartedException e) {
                }
                Logger.getInstance().addError("Saving pdf from dot failed.\n" + errors, ex);
            }
        });
        thread.start();
        return thread;
    }

    public static Thread save2PDF(String path, DotSaveable object) throws IOException, InterruptedException {
        String bufferpath = path + "_" + System.currentTimeMillis();
        Thread dot;
        dot = save2DotAndPDF(bufferpath, object);
        Thread mvPdf = new Thread(() -> {
            try {
                dot.join();
                // Delete dot file
                new File(bufferpath + ".dot").delete();
                Logger.getInstance().addMessage("Deleted: " + bufferpath + ".dot", true);
                // move to original name 
                Files.move(new File(bufferpath + ".pdf").toPath(), new File(path + ".pdf").toPath(), REPLACE_EXISTING);
                Logger.getInstance().addMessage("Moved: " + bufferpath + ".pdf --> " + path + ".pdf", true);
            } catch (IOException | InterruptedException ex) {
                Logger.getInstance().addError("Deleting the buffer files and moving the pdf failed", ex);
            }
        });
        mvPdf.start();
        return mvPdf;
    }

}
