package org.roadinspector.centralstation.recognizer;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import org.roadinspector.centralstation.intelligence.Intelligence;
import org.roadinspector.centralstation.imageanalysis.Char;
import org.roadinspector.centralstation.neuralnetwork.NeuralNetwork;

public class NeuralPatternClassificator extends CharacterRecognizer {
    
    private static int normalize_x =
            Intelligence.configurator.getIntProperty("char_normalizeddimensions_x");
    private static int normalize_y =
            Intelligence.configurator.getIntProperty("char_normalizeddimensions_y");
    
    // dimension of input letter after transformation: 10 x 16 = 160 neurons
    
    public NeuralNetwork network;
    
    // do not learn netwotk, but load if from file (default)
    public NeuralPatternClassificator() throws Exception {
        this(false);
    }
    
    public NeuralPatternClassificator(boolean learn) throws Exception {
        // zakomentovane dna 2.1.2007
        // this.normalize_x = Intelligence.configurator.getIntProperty("char_normalizeddimensions_x");
        // this.normalize_y = Intelligence.configurator.getIntProperty("char_normalizeddimensions_y");
        
        Vector<Integer> dimensions = new Vector<Integer>();
        
        // determine size of input layer according to chosen feature extraction method.
        int inputLayerSize;
        if (Intelligence.configurator.getIntProperty("char_featuresExtractionMethod")==0)
            inputLayerSize = normalize_x * normalize_y;
        else inputLayerSize = CharacterRecognizer.features.length*4;
        
        // construct new neural network with specified dimensions.
        dimensions.add(inputLayerSize);
        dimensions.add(Intelligence.configurator.getIntProperty("neural_topology"));
        dimensions.add(CharacterRecognizer.alphabet.length);
        this.network = new NeuralNetwork(dimensions);
        
        if (learn) {
            // learn network
            learnAlphabet(Intelligence.configurator.getStrProperty("char_learnAlphabetPath"));
        } else {
            // or load network from xml
            this.network = new NeuralNetwork(Intelligence.configurator.getPathProperty("char_neuralNetworkPath"));
        }
    }
    
    // IMAGE -> CHAR
    public RecognizedChar recognize(Char imgChar) { // rozpozna UZ normalizovany char
        imgChar.normalize();
        Vector<Double> output = this.network.test(imgChar.extractFeatures());
        double max = 0.0;
        int indexMax = 0;
        
        RecognizedChar recognized = new RecognizedChar();
        
        for (int i=0; i<output.size(); i++) {
            recognized.addPattern(recognized.new RecognizedPattern(this.alphabet[i], output.elementAt(i).floatValue()));
        }
        recognized.render();
        recognized.sort(1);
        return recognized;
    }
    
//    public Vector<Double> imageToVector(Char imgChar) {
//        Vector<Double> vectorInput = new Vector<Double>();
//        for (int x = 0; x<imgChar.getWidth(); x++)
//            for (int y = 0; y<imgChar.getHeight(); y++)
//                vectorInput.add(new Double(imgChar.getBrightness(x,y)));
//        return vectorInput;
//    }
    public NeuralNetwork.SetOfIOPairs.IOPair createNewPair(char chr, Char imgChar) { // uz normalizonvany
        Vector<Double> vectorInput = imgChar.extractFeatures();
        
        
        
        Vector<Double> vectorOutput = new Vector<Double>();
        for (int i=0; i<this.alphabet.length; i++)
            if (chr == this.alphabet[i]) vectorOutput.add(1.0); else vectorOutput.add(0.0);
        
/*        System.out.println();
        for (Double d : vectorInput) System.out.print(d+" ");
        System.out.println();
        for (Double d : vectorOutput) System.out.print(d+" ");
        System.out.println();
 */
        
        return (new NeuralNetwork.SetOfIOPairs.IOPair(vectorInput, vectorOutput));
    }
    
    // NAUCI NEURON NETWORK ABECEDE WHO LOOKS AT THE ADDRESS PATH
    public void learnAlphabet(String path) throws IOException {
        String alphaString = "0123456789abcdefghijklmnopqrstuvwxyz";        
        File folder = new File(path);
        NeuralNetwork.SetOfIOPairs train = new NeuralNetwork.SetOfIOPairs();
        
        for (String fileName : folder.list()) {
            if (alphaString.indexOf(fileName.toLowerCase().charAt(0))==-1)
                continue; // je to nezname meno suboru, skip
            
            Char imgChar = new Char(path+File.separator+fileName);
            imgChar.normalize();
            train.addIOPair(this.createNewPair(fileName.toUpperCase().charAt(0), imgChar));
        }
        
        this.network.learn(train,
                Intelligence.configurator.getIntProperty("neural_maxk"),
                Intelligence.configurator.getDoubleProperty("neural_eps"),
                Intelligence.configurator.getDoubleProperty("neural_lambda"),
                Intelligence.configurator.getDoubleProperty("neural_micro")
                );
    }
}
