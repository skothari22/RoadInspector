package org.roadinspector.centralstation.gui.tools;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class ImageFileFilter extends FileFilter {
    public boolean accept(File f) {
        String name = f.getName();
        String type = new String(name.substring(name.lastIndexOf('.')+1,name.length()).toLowerCase());
        if (!type.equals("bmp") &&
            !type.equals("jpg") &&
            !type.equals("jpeg") &&
            !type.equals("png") && 
            !type.equals("gif") &&
            !f.isDirectory()
            ) return false;
        return true;
    }
    
    public static boolean accept(String name) {
        String type = new String(name.substring(name.lastIndexOf('.')+1,name.length()).toLowerCase());
        if (!type.equals("bmp") &&
            !type.equals("jpg") &&
            !type.equals("jpeg") &&
            !type.equals("png") && 
            !type.equals("gif")
            ) return false;
        return true;
    }

    public String getDescription() {
        return "images (*.jpg, *.bmp, *.gif, *.png)";
    }
}
