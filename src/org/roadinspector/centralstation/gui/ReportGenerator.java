package org.roadinspector.centralstation.gui;

//import javaanpr.intelligence.Intelligence;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import org.roadinspector.centralstation.intelligence.Intelligence;
import javax.imageio.ImageIO;

public class ReportGenerator {
    private String path;
    private String output;
    private BufferedWriter out;
    private boolean enabled;
    
    public ReportGenerator(String path) throws IOException {
        this.path = path;
        this.enabled = true;
        
        File f = new File(path);
        if (!f.exists()) throw new IOException("Report directory '"+path+"' doesn't exists");
        
        this.output = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">"+
                "<html>" +
                "<head><title>ANPR report</title>" +
                "</head>" +
                "<style type=\"text/css\">" +
		"@import \"style.css\";" +
                "</style>";
        
    }
    
    public ReportGenerator() {
        this.enabled = false;
    }
    
    public void insertText(String text) {
        if (!enabled) return;
        this.output += text;
        this.output += "\n";
    }
    
    public void insertImage(BufferedImage image, String cls, int w, int h) throws IOException {
        if (!enabled) return;
        String imageName = String.valueOf(image.hashCode())+".jpg";
        this.saveImage(image, path+File.separator+imageName);
        if (w!=0 && h!=0)
            this.output += "<img src='"+imageName+"' alt='' width='"+w+"' height='"+h+"' class='"+cls+"'>\n";
        else 
            this.output += "<img src='"+imageName+"' alt='' class='"+cls+"'>\n";
    }
    
    public void finish() throws Exception {
        if (!enabled) return;
        this.output += "</html>";
        FileOutputStream os = new FileOutputStream(this.path + File.separator + "index.html");
        Writer writer = new OutputStreamWriter(os);
        writer.write(output);
        writer.flush();
        writer.close();
        copyFile(new File(Intelligence.configurator.getPathProperty("reportgeneratorcss")),new File(this.path+File.separator+"style.css"));
    }
    
    public void copyFile(File in, File out) throws Exception {
        FileChannel sourceChannel = new
                FileInputStream(in).getChannel();
        FileChannel destinationChannel = new
                FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        // or
        //  destinationChannel.transferFrom
        //       (sourceChannel, 0, sourceChannel.size());
        sourceChannel.close();
        destinationChannel.close();
    }

    public void saveImage(BufferedImage bi, String filepath) throws IOException {
        if (!enabled) return;
        String type = new String(filepath.substring(filepath.lastIndexOf('.')+1,filepath.length()).toUpperCase());
        if (!type.equals("BMP") &&
                !type.equals("JPG") &&
                !type.equals("JPEG") &&
                !type.equals("PNG")
                ) System.out.println("unsupported format exception");//throw new IOException("Unsupported file format");
        File destination = new File(filepath);
        try {
            ImageIO.write(bi, type, destination);
        } catch (Exception e) {
            System.out.println("catched "+e.toString());
            System.exit(1);
            throw new IOException("Can't open destination report directory");
        }
    }
}
