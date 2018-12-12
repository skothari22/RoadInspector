package org.roadinspector.centralstation.intelligence;

import java.io.IOException;
import java.util.Vector;
//import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
//import org.xml.sax.SAXException;
import org.roadinspector.centralstation.Main;
import org.roadinspector.centralstation.recognizer.CharacterRecognizer.*;

public class Parser {
    public class PlateForm {
        public class Position {
            public char[] allowedChars;
            public Position(String data) {
                this.allowedChars = data.toCharArray();
            }
            public boolean isAllowed(char chr) {
                boolean ret = false;
                for (int i=0; i<this.allowedChars.length; i++)
                    if (this.allowedChars[i] == chr)
                        ret = true;
                return ret;
            }
        }
        Vector<Position> positions;
        String name;
        public boolean flagged = false;
        
        public PlateForm(String name) {
            this.name = name;
            this.positions = new Vector<Position>();
        }
        public void addPosition(Position p) {
            this.positions.add(p);
        }
        public Position getPosition(int index) {
            return this.positions.elementAt(index);
        }
        public int length() {
            return this.positions.size();
        }
        
    }
    public class FinalPlate {
        public String plate;
        public float requiredChanges = 0;
        FinalPlate() {
            this.plate = new String();
        }
        public void addChar(char chr) {
            this.plate = this.plate + chr;
        }
    }
    
    Vector<PlateForm> plateForms;
    
    /** Creates a new instance of Parser */
    public Parser() throws Exception {
        this.plateForms = new Vector<PlateForm>();
        this.plateForms = this.loadFromXml(Intelligence.configurator.getPathProperty("intelligence_syntaxDescriptionFile"));
    }
    
    public Vector<PlateForm> loadFromXml(String fileName) throws Exception {
        Vector<PlateForm> plateForms = new Vector<PlateForm>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.parse(fileName);
        
        Node structureNode = doc.getDocumentElement();
        NodeList structureNodeContent = structureNode.getChildNodes();
        for (int i=0; i<structureNodeContent.getLength(); i++) {
            Node typeNode = structureNodeContent.item(i);
            if (!typeNode.getNodeName().equals("type")) continue;
            PlateForm form = new PlateForm(((Element)typeNode).getAttribute("name"));
            NodeList typeNodeContent = typeNode.getChildNodes();
            for (int ii=0; ii<typeNodeContent.getLength(); ii++) {
                Node charNode = typeNodeContent.item(ii);
                if (!charNode.getNodeName().equals("char")) continue;
                String content = ((Element)charNode).getAttribute("content");
                
                form.addPosition(form.new Position(  content.toUpperCase()  ));
            }
            plateForms.add(form);
        }
        return plateForms;
    }
    ////
    public void unFlagAll() {
        for (PlateForm form : this.plateForms)
            form.flagged = false;
    }
    // For the length of the mark, try to find some plateforms of the same length
    // if there is no one, it will allow you to have some character in it
    // and flat plateform with a lesser degree of writing
    public void flagEqualOrShorterLength(int length) {
        boolean found = false;
        for (int i=length; i>=1 && !found; i--) {
            for (PlateForm form : this.plateForms) {
                if (form.length() == i) {
                    form.flagged = true;
                    found = true;
                }
            }
        }
    }
    
    public void flagEqualLength(int length) {
        for (PlateForm form : this.plateForms) {
            if (form.length() == length) {
                form.flagged = true;
            }
        }
    }

    public void invertFlags() {
        for (PlateForm form : this.plateForms)
            form.flagged = !form.flagged;
    }    
    
    // syntax analysis mode : 0 (do not parse)
    //                      : 1 (only equal length)
    //                      : 2 (equal or shorter)
    public String parse(RecognizedPlate recognizedPlate, int syntaxAnalysisMode) throws IOException {
        if (syntaxAnalysisMode==0) {
            Main.rg.insertText(" result : "+recognizedPlate.getString()+" --> <font size=15>"+recognizedPlate.getString()+"</font><hr><br>");
            return recognizedPlate.getString();
        }
        
        int length = recognizedPlate.chars.size();
        this.unFlagAll();
        if (syntaxAnalysisMode==1) {
            this.flagEqualLength(length);
        } else {
            this.flagEqualOrShorterLength(length);
        }
        
        Vector<FinalPlate> finalPlates = new Vector<FinalPlate>();
        
        for (PlateForm form : this.plateForms) {
            if (!form.flagged) continue; // skip unflagged
            for (int i=0; i<= length - form.length(); i++) { //sliding form after branding
//                System.out.println("comparing "+recognizedPlate.getString()+" with form "+form.name+" and offset "+i );
                FinalPlate finalPlate = new FinalPlate();
                for (int ii=0; ii<form.length(); ii++) { // all forms of the form have passed
                    // form.getPosition(ii).allowedChars // zoznam povolenych
                    RecognizedChar rc = recognizedPlate.getChar(ii+i); // character on the mark
                    
                    if (form.getPosition(ii).isAllowed(rc.getPattern(0).getChar())) {
                        finalPlate.addChar(rc.getPattern(0).getChar());
                    } else { // need to be replaced
                        finalPlate.requiredChanges++; // +1 for writing
                        for (int x=0; x<rc.getPatterns().size(); x++) {
                            if (form.getPosition(ii).isAllowed(rc.getPattern(x).getChar())) {
                                RecognizedChar.RecognizedPattern rp = rc.getPattern(x);
                                finalPlate.requiredChanges += (rp.getCost() / 100);  // + x at its cost
                                finalPlate.addChar(rp.getChar());
                                break;
                            }
                        }
                    }
                }
//                System.out.println("adding "+finalPlate.plate+" with required changes "+finalPlate.requiredChanges);
                finalPlates.add(finalPlate);
            }
        }
//        


        
        // you are undetected if the label is not marked
        if (finalPlates.size()==0) return recognizedPlate.getString();
        // else :
        // find yourself with the lowest honor of exchange
        float minimalChanges = Float.POSITIVE_INFINITY;
        int minimalIndex = 0;
//        System.out.println("---");
        for (int i=0; i<finalPlates.size(); i++) {
//            System.out.println("::"+finalPlates.elementAt(i).plate+" "+finalPlates.elementAt(i).requiredChanges);
            if (finalPlates.elementAt(i).requiredChanges <= minimalChanges) {
                minimalChanges = finalPlates.elementAt(i).requiredChanges;
                minimalIndex = i;
            }
        }
        
        String toReturn = recognizedPlate.getString();
        if (finalPlates.elementAt(minimalIndex).requiredChanges <= 2)
            toReturn = finalPlates.elementAt(minimalIndex).plate;
        return toReturn;
    }
    
}
