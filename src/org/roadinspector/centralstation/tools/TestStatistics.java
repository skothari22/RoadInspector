package org.roadinspector.centralstation.tools;

import java.io.BufferedReader;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileReader;
//import java.io.IOException;
import java.util.Vector;

public class TestStatistics {
    
    public static String helpText = "";
    
    public TestStatistics() {
    }
    
    public static void main(String[] args) {
        if (args.length==2 &&
                args[0].equals("-i")
                ) { // proceed analysis
            try {
                File f = new File(args[1]);
                BufferedReader input = new BufferedReader(new FileReader(f));
                String line;
                int lineCount = 0;
                String[] split;
                TestReport testReport = new TestReport();
                while ((line = input.readLine())!=null) {
                    lineCount++;
                    split = line.split(",",4);
                    if (split.length!=3) {
                        System.out.println("Warning: line "+lineCount+" contains invalid CSV data (skipping)");
                        continue;
                    }
                    testReport.addRecord(testReport.new TestRecord(split[0], split[1], split[2]));
                }
                input.close();
                testReport.printStatistics();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        } else {
            // DONE display help
            System.out.println(helpText);
        }
        
    }
}

class TestReport {
    class TestRecord {
        String name, plate, recognizedPlate;
        int good;
        int length;
        
        TestRecord(String name, String plate, String recognizedPlate) {
            this.name = name.trim();
            this.plate = plate.trim();
            this.recognizedPlate = recognizedPlate.trim();
            compute();
        }
        
        private void compute() {
            this.length = Math.max(plate.length(), recognizedPlate.length());
            int g1 = 0;
            int g2 = 0;
            for (int i=0; i<this.length; i++) { // POROVNAVAT ODPREDU (napr. BA123AB vs. BA123ABX)
                if (getChar(plate,i) == getChar(recognizedPlate,i)) g1++;
            }
            for (int i=0; i<this.length; i++) { // POROVNAVAT ODZADU (napr. BA123AB vs. XBA123AB)
                if (getChar(plate,this.length-i-1) == getChar(recognizedPlate,this.length-i-1)) g2++;
            }
            this.good = Math.max(g1,g2);
        }
        
        private char getChar(String string, int position) {
            if (position >= string.length()) return ' ';
            if (position < 0) return ' ';
            return string.charAt(position);
        }
        
        public int getGoodCount() {
            return this.good;
        }
        public int getLength() {
            return this.length;
        }
        public boolean isOk() {
            if (this.length != this.good) return false; else return true;
        }
    }
    
    Vector<TestRecord> records;
    
    TestReport() {
        this.records = new Vector<TestRecord>();
    }
    
    void addRecord(TestRecord testRecord) {
        this.records.add(testRecord);
    }
    
    void printStatistics() {
        int weightedScoreCount = 0;
        int binaryScoreCount = 0;
        int characterCount = 0;
        System.out.println("----------------------------------------------");
        System.out.println("Defective plates\n");

        for(TestRecord record : this.records) {
            characterCount += record.getLength();
            weightedScoreCount += record.getGoodCount();
            binaryScoreCount += (record.isOk() ? 1 : 0);
            if (!record.isOk()) {
                System.out.println(record.plate+" ~ "+record.recognizedPlate+ " (" +(float)record.getGoodCount()/record.getLength()*100+ "% ok)");
            }
        }
        System.out.println("\n----------------------------------------------");
        System.out.println("Test report statistics\n");
        System.out.println("Total number of plates     : "+this.records.size());
        System.out.println("Total number of characters : "+characterCount);
        System.out.println("Binary score               : "+(float)binaryScoreCount / this.records.size()*100);
        System.out.println("Weighted score             : "+(float)weightedScoreCount / characterCount*100);
    }
}
