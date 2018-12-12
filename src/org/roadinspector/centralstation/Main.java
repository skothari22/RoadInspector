package org.roadinspector.centralstation;

import org.roadinspector.centralstation.gui.ReportGenerator;
import javax.swing.UIManager;
import org.roadinspector.centralstation.gui.windows.FrameComponentInit;
import org.roadinspector.centralstation.gui.windows.FrameMain;
import org.roadinspector.centralstation.intelligence.Intelligence;

public class Main {
    public static ReportGenerator rg = new ReportGenerator();
    public static Intelligence systemLogic;
    
    public static void main(String[] args) throws Exception {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            FrameComponentInit frameComponentInit = new FrameComponentInit(); // show wait
            Main.systemLogic = new Intelligence(false);
            frameComponentInit.dispose(); // hide wait
            FrameMain mainFrame = new FrameMain();
    }
}
