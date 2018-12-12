package org.roadinspector.centralstation.neuralnetwork;

import java.text.ParseException;
import java.util.Vector;
import java.util.Random;
import java.util.Date;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import java.io.*;
import org.xml.sax.SAXException;

public class NeuralNetwork {
    // holds list of layers   
    private Vector<NeuralLayer> listLayers = new Vector<NeuralLayer>();
    private Random randomGenerator;
    
    //dimensions are in the order of the lowest (input) to the topmost layer
    public NeuralNetwork(Vector<Integer> dimensions) {
        // initialization of layers
        for (int i=0; i<dimensions.size(); i++) {
            this.listLayers.add(
                               new NeuralLayer(dimensions.elementAt(i).intValue(), this)
                               );
        }
        randomGenerator = new Random();
//        System.out.println("Created neural network with "+dimensions.size()+" layers");
    }
    
    public NeuralNetwork(String fileName) throws ParserConfigurationException, SAXException, IOException, ParseException {
        loadFromXml(fileName);
        randomGenerator = new Random();
    }

    public Vector<Double> test (Vector<Double> inputs) {
        if (inputs.size() != this.getLayer(0).numberOfNeurons()) throw new ArrayIndexOutOfBoundsException("[Error] NN-Test: You are trying to pass vector with "+inputs.size()+" values into neural layer with "+this.getLayer(0).numberOfNeurons()+" neurons. Consider using another network, or another descriptors.");
        else return activities(inputs);
    }
    
    public void learn (SetOfIOPairs trainingSet, int maxK, double eps, double lambda, double micro) {
        if (trainingSet.pairs.size()==0) throw new NullPointerException("[Error] NN-Learn: You are using an empty training set, neural network couldn't be trained.");
        else if (trainingSet.pairs.elementAt(0).inputs.size() != this.getLayer(0).numberOfNeurons())
            throw new ArrayIndexOutOfBoundsException("[Error] NN-Test: You are trying to pass vector with "+trainingSet.pairs.elementAt(0).inputs.size()+" values into neural layer with "+this.getLayer(0).numberOfNeurons()+" neurons. Consider using another network, or another descriptors.");
        else if (trainingSet.pairs.elementAt(0).outputs.size() != this.getLayer(this.numberOfLayers()-1).numberOfNeurons())
            throw new ArrayIndexOutOfBoundsException("[Error] NN-Test:  You are trying to pass vector with "+trainingSet.pairs.elementAt(0).inputs.size()+" values into neural layer with "+this.getLayer(0).numberOfNeurons()+" neurons. Consider using another network, or another descriptors.");
        else adaptation(trainingSet,maxK,eps,lambda,micro);
    } 

    public int numberOfLayers() {
        return this.listLayers.size();
    }
  
    private void loadFromXml(String fileName) throws ParserConfigurationException, SAXException, IOException, ParseException {
        System.out.println("NeuralNetwork : loading network topology from file "+fileName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.parse(fileName);

        Node nodeNeuralNetwork = doc.getDocumentElement();
        if (!nodeNeuralNetwork.getNodeName().equals("neuralNetwork")) throw new ParseException("[Error] NN-Load: Parse error in XML file, neural network couldn't be loaded.",0);
        // nodeNeuralNetwork ok
        // indexNeuralNetworkContent -> indexStructureContent -> indexLayerContent -> indexNeuronContent -> indexNeuralInputContent
        NodeList nodeNeuralNetworkContent = nodeNeuralNetwork.getChildNodes();
        for (int innc=0; innc<nodeNeuralNetworkContent.getLength(); innc++) {
            Node nodeStructure = nodeNeuralNetworkContent.item(innc);
            if (nodeStructure.getNodeName().equals("structure")) { // for structure element
                NodeList nodeStructureContent = nodeStructure.getChildNodes();
                for (int isc=0; isc<nodeStructureContent.getLength();isc++) {
                    Node nodeLayer = nodeStructureContent.item(isc);
                    if (nodeLayer.getNodeName().equals("layer")) { // for layer element
                        NeuralLayer neuralLayer = new NeuralLayer(this);
                        this.listLayers.add(neuralLayer);
                        NodeList nodeLayerContent = nodeLayer.getChildNodes();
                        for (int ilc=0; ilc<nodeLayerContent.getLength();ilc++) {
                            Node nodeNeuron = nodeLayerContent.item(ilc);
                            if (nodeNeuron.getNodeName().equals("neuron")) { // for neuron in layer
                                Neuron neuron = new Neuron(Double.parseDouble(
                                                                             ((Element)nodeNeuron).getAttribute("threshold")
                                                                             ),neuralLayer);
                                neuralLayer.listNeurons.add(neuron);
                                NodeList nodeNeuronContent = nodeNeuron.getChildNodes();
                                for (int inc=0; inc < nodeNeuronContent.getLength();inc++) {
                                    Node nodeNeuralInput = nodeNeuronContent.item(inc);
                                    //if (nodeNeuralInput==null) System.out.print("-"); else System.out.print("*");
                                    
                                    if (nodeNeuralInput.getNodeName().equals("input")) {
//                                        System.out.println("neuron at STR:"+innc+" LAY:"+isc+" NEU:"+ilc+" INP:"+inc);
                                        NeuralInput neuralInput = new NeuralInput(Double.parseDouble(
                                                                             ((Element)nodeNeuralInput).getAttribute("weight")   
                                                                             ),neuron);
                                        neuron.listInputs.add(neuralInput);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        

    }

    public void saveToXml(String fileName) throws ParserConfigurationException, FileNotFoundException, TransformerException, TransformerConfigurationException {
        System.out.println("Saving network topology to file "+fileName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document doc = parser.newDocument();
        
        Element root = doc.createElement("neuralNetwork");
            root.setAttribute("dateOfExport",new Date().toString());
        Element layers = doc.createElement("structure");
            layers.setAttribute("numberOfLayers",Integer.toString(this.numberOfLayers()));
        
        for (int il=0; il<this.numberOfLayers(); il++) {
            Element layer = doc.createElement("layer");
            layer.setAttribute("index",Integer.toString(il));
            layer.setAttribute("numberOfNeurons",Integer.toString(this.getLayer(il).numberOfNeurons()));
            
            for (int in=0; in<this.getLayer(il).numberOfNeurons();in++) {
                Element neuron = doc.createElement("neuron");
                neuron.setAttribute("index",Integer.toString(in));
                neuron.setAttribute("NumberOfInputs",Integer.toString(this.getLayer(il).getNeuron(in).numberOfInputs()));
                neuron.setAttribute("threshold",Double.toString(this.getLayer(il).getNeuron(in).threshold));
                
                for (int ii=0; ii<this.getLayer(il).getNeuron(in).numberOfInputs();ii++) {
                    Element input = doc.createElement("input");
                    input.setAttribute("index",Integer.toString(ii));
                    input.setAttribute("weight",Double.toString(this.getLayer(il).getNeuron(in).getInput(ii).weight));
                    
                    neuron.appendChild(input);
                }
                
                layer.appendChild(neuron);
            }
            
            layers.appendChild(layer);
        }
        
        root.appendChild(layers);
        doc.appendChild(root);
        
        // save
        File xmlOutputFile = new File(fileName);
        FileOutputStream fos;
        Transformer transformer;

        fos = new FileOutputStream(xmlOutputFile);
        // Use a Transformer for output
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(fos);
        // transform source into result will do save
        transformer.setOutputProperty("encoding","iso-8859-2");
        transformer.setOutputProperty("indent","yes");
        transformer.transform(source, result);
    }    
    
    


    public static class SetOfIOPairs {
        Vector<IOPair> pairs;
        public static class IOPair { // YOU HAVE A PRIDAL STATIC, but there was not there
            Vector<Double> inputs;
            Vector<Double> outputs;
            public IOPair(Vector<Double> inputs, Vector<Double> outputs) {
                // corrected warning
                //this.inputs = (Vector<Double>)inputs.clone();
                //this.outputs = (Vector<Double>)outputs.clone();
                this.inputs = new Vector<Double>(inputs);
                this.outputs = new Vector<Double>(outputs);
            }
        }
        public SetOfIOPairs() {
            this.pairs = new Vector<IOPair>();
        }
        public void addIOPair(Vector<Double> inputs, Vector<Double> outputs) {
            this.addIOPair(new IOPair(inputs,outputs));
        }
        public void addIOPair(IOPair pair) {
            this.pairs.add(pair);
        }
        int size() {
            return pairs.size();
        }
    }
    private class NeuralInput {
        double weight;
        int index;
        Neuron neuron;
        
        NeuralInput(double weight, Neuron neuron) {
            this.neuron = neuron;
            this.weight = weight;
            this.index = this.neuron.numberOfInputs();
            //System.out.println("Created neural input "+this.index+" with weight "+this.weight);
        }
    } // end class NeuralInput
    private class Neuron {
        private Vector<NeuralInput> listInputs = new Vector<NeuralInput>();//holds list of inputs
        int index;
        public double threshold;
        public double output;
        NeuralLayer neuralLayer;
        
        // initializes all neuron weights to 1 parameter specifies number of weights
        
        Neuron(double threshold, NeuralLayer neuralLayer) {
            this.threshold = threshold;
            this.neuralLayer = neuralLayer;
            this.index = this.neuralLayer.numberOfNeurons();
        }
        
        Neuron(int numberOfInputs, double threshold, NeuralLayer neuralLayer) {
            this.threshold = threshold;
            this.neuralLayer = neuralLayer;
            this.index = this.neuralLayer.numberOfNeurons();
            for (int i=0; i<numberOfInputs; i++) {
                this.listInputs.add(new NeuralInput(1.0,this));
            }
        }

        public int numberOfInputs() {
            return this.listInputs.size();
        }
        
        public NeuralInput getInput (int index) {
            return this.listInputs.elementAt(index);
        }
        
    } // end class Neuron

    private class NeuralLayer {
        //holds list od neurons
        private Vector<Neuron> listNeurons = new Vector<Neuron>();
        int index;
        NeuralNetwork neuralNetwork;
        
        NeuralLayer(NeuralNetwork neuralNetwork) {
            this.neuralNetwork = neuralNetwork;
            this.index = this.neuralNetwork.numberOfLayers();
        }
        
        // initializes all neurons in layer
        NeuralLayer(int numberOfNeurons, NeuralNetwork neuralNetwork) {
            this.neuralNetwork = neuralNetwork;
            this.index = this.neuralNetwork.numberOfLayers();
            // if one is the lowest layer (0), each neuron will have only 1 input
            for (int i=0; i<numberOfNeurons;i++) {
                if (this.index == 0) {
                    this.listNeurons.add(new Neuron(1,0.0,this)); 
                    /* thresholds of the neurons of the lowest layer are always 0.0, the layer only distributes the inputs, at least it results
                      * from the algorithm on page 111 */
                } else { //in the opposite case, the neuron will have all the inputs as the neurons of the lower layer
                    this.listNeurons.add(
                                        /* the neuron thresholds on the upper layers will be 0.0, but they may not be */
                                        new Neuron(this.neuralNetwork.getLayer(this.index-1).numberOfNeurons(), 0.0, this)
                                        );
                }
            }
            //System.out.println("Created neural layer "+this.index+" with "+numberOfNeurons+" neurons");
        } // end constructor
        
        public int numberOfNeurons() {
            return this.listNeurons.size();
        }
        
        public boolean isLayerTop() {
            return (this.index == this.neuralNetwork.numberOfLayers()-1);
        }
        
        public boolean isLayerBottom() {
            return (this.index == 0);
        }
        
        public NeuralLayer upperLayer() { 
            if (this.isLayerTop()) return null;
            return this.neuralNetwork.getLayer(index+1);
        }

        public NeuralLayer lowerLayer() { 
            if (this.isLayerBottom()) return null;
            return this.neuralNetwork.getLayer(index-1);
        }
        
        public Neuron getNeuron(int index) {
            return this.listNeurons.elementAt(index);
        }
        
    } // end class NeuralLayer
    
    private class Gradients {
        Vector<Vector<Double>> thresholds;
        Vector<Vector<Vector<Double>>> weights;
        NeuralNetwork neuralNetwork;
        
        Gradients(NeuralNetwork network) {
            this.neuralNetwork = network;
            this.initGradients();
        }
        
        public void initGradients() {
            this.thresholds = new Vector<Vector<Double>>();
            this.weights = new Vector<Vector<Vector<Double>>>();
            //System.out.println("init for threshold gradient "+this.toString());
            for (int il = 0; il < this.neuralNetwork.numberOfLayers(); il++) {
                this.thresholds.add(new Vector<Double>());
                this.weights.add(new Vector<Vector<Double>>());
                for (int in = 0; in < this.neuralNetwork.getLayer(il).numberOfNeurons(); in++) {
                    this.thresholds.elementAt(il).add(0.0);
                    this.weights.elementAt(il).add(new Vector<Double>());
                    for (int ii = 0; ii < this.neuralNetwork.getLayer(il).getNeuron(in).numberOfInputs(); ii++) {
                        this.weights.elementAt(il).elementAt(in).add(0.0);
                    } // for each input
                } // for each neuron
            } // for each layer
        }
        
        public void resetGradients() { //resets to 0
           for (int il = 0; il < this.neuralNetwork.numberOfLayers(); il++) {
               for (int in = 0; in < this.neuralNetwork.getLayer(il).numberOfNeurons(); in++) { 
                   this.setThreshold(il,in,0.0); 
                   for (int ii = 0; ii < this.neuralNetwork.getLayer(il).getNeuron(in).numberOfInputs(); ii++) {
                       this.setWeight(il,in,ii,0.0);
                   }
               }
           }
        }
        
        public double getThreshold(int il, int in) {
            return thresholds.elementAt(il).elementAt(in).doubleValue();
        }

        public void setThreshold(int il, int in, double value) {
            thresholds.elementAt(il).setElementAt(value, in);
        }
        
        public void incrementThreshold(int il, int in, double value) {
            this.setThreshold(il,in,this.getThreshold(il,in) + value);
        }
        
        public double getWeight (int il, int in, int ii) {
            return weights.elementAt(il).elementAt(in).elementAt(ii).doubleValue();
        }
        
        public void setWeight (int il, int in, int ii, double value) {
            weights.elementAt(il).elementAt(in).setElementAt(value,ii);
        }
         
        public void incrementWeight(int il, int in, int ii, double value) {
            this.setWeight(il,in,ii,this.getWeight(il,in,ii) + value);
        }

        public double getGradientAbs() {
            double currE = 0;
            
            for (int il=1;il<neuralNetwork.numberOfLayers();il++) {
                currE += this.vectorAbs(thresholds.elementAt(il)); 
                currE += this.doubleVectorAbs(weights.elementAt(il));
            }
            return currE;            
            
            //for (Vector<Double> vector : this.thresholds) currE += this.vectorAbs(vector); 
            //for (Vector<Vector<Double>> doubleVector : this.weights) currE += this.doubleVectorAbs(doubleVector);    
            //return currE;
        }
        
        private double doubleVectorAbs(Vector<Vector<Double>> doubleVector) {
            double totalX = 0;
            for (Vector<Double> vector : doubleVector) {
                totalX += Math.pow(vectorAbs(vector),2);
            }
            return Math.sqrt(totalX);
        }
        
        private double vectorAbs(Vector<Double> vector) {
            double totalX = 0;
            for (Double x : vector) totalX += Math.pow(x,2);
            return Math.sqrt(totalX);
        }
        
    }
    
    private double random() {
        return randomGenerator.nextDouble();
    }
    
    private void computeGradient(Gradients gradients, Vector<Double> inputs, Vector<Double> requiredOutputs) {
       //Gradients gradients = new Gradients(this);
       activities(inputs);
       for (int il=this.numberOfLayers()-1; il>=1; il--) { //backpropagation over all layers except the last one
            NeuralLayer currentLayer = this.getLayer(il);
           
            if (currentLayer.isLayerTop()) { // if one is the highest layer
                // add a gradient of thresholds for the dan layer to the corresponding vector, and this gradient is felt through the neurons:
                //gradients.thresholds.add(il, new Vector<Double>()); 
                for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // pre vsetky neurony na vrstve
                    Neuron currentNeuron = currentLayer.getNeuron(in);
                    gradients.setThreshold(il, in, 
                            currentNeuron.output * (1 - currentNeuron.output) * (currentNeuron.output - requiredOutputs.elementAt(in))
                            );
                } // end for each neuron

                for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // for each neuron
                    Neuron currentNeuron = currentLayer.getNeuron(in);
                    for (int ii=0; ii<currentNeuron.numberOfInputs(); ii++) { // for each neuron's input
                        NeuralInput currentInput = currentNeuron.getInput(ii);
                        gradients.setWeight(il,in,ii,
                                gradients.getThreshold(il,in) * currentLayer.lowerLayer().getNeuron(ii).output    
                                );
                    } // end for each input
                } // end for each neuron
                
            } else { //if one of the lower layers (the lowest layer does not look, we only go after 1)
                // pocitame gradient thresholds:
                //gradients.thresholds.add(il, new Vector<Double>()); 
                for (int in=0; in<currentLayer.numberOfNeurons();in++) { // for each neuron
                    double aux = 0;
                    // iterujeme cez vsetky axony neuronu (resp. synapsie neuronov na vyssej vrstve)
                    for (int ia=0; ia<currentLayer.upperLayer().numberOfNeurons(); ia++) { 
                        aux += gradients.getThreshold(il+1,ia) * 
                               currentLayer.upperLayer().getNeuron(ia).getInput(in).weight;
                    }
                    gradients.setThreshold(il,in,
                            currentLayer.getNeuron(in).output * (1 - currentLayer.getNeuron(in).output) * aux
                            );
                } //end for each neuron
                
                // pocitame gradienty vah : 
                for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // for each neuron
                    Neuron currentNeuron = currentLayer.getNeuron(in);
                    for (int ii=0; ii<currentNeuron.numberOfInputs(); ii++) { // for each neuron's input
                        NeuralInput currentInput = currentNeuron.getInput(ii);
                        gradients.setWeight(il, in, ii,
                                gradients.getThreshold(il,in) * currentLayer.lowerLayer().getNeuron(ii).output
                        );    
                    } // end for each input
                } // end for each neuron
               
            } // end layer IF
            
        } // end backgropagation for each layer
        //return gradients;
    }
   
    private void computeTotalGradient(Gradients totalGradients, Gradients partialGradients, SetOfIOPairs trainingSet) {
        // na zaciatku sa inicializuju gradienty (total)
        totalGradients.resetGradients();
        //partialGradients.resetGradients();
        //Gradients totalGradients = new Gradients(this);
        //Gradients partialGradients = new Gradients(this); /***/
        
        for (SetOfIOPairs.IOPair pair : trainingSet.pairs) { // pre kazdy par trenovacej mnoziny
            //partialGradients = computeGradient(pair.inputs, pair.outputs);
            computeGradient (partialGradients, pair.inputs, pair.outputs);
            for (int il = this.numberOfLayers()-1; il >= 1; il--) { // pre vsetky vrstvy okrem poslednej
                NeuralLayer currentLayer = this.getLayer(il);
                for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // pre vsetky neurony na currentLayer
                    // upravime gradient prahov : 
                    totalGradients.incrementThreshold(il,in,partialGradients.getThreshold(il,in));
                    for (int ii=0; ii<currentLayer.lowerLayer().numberOfNeurons(); ii++) { // pre vsetky vstupy
                        totalGradients.incrementWeight(il,in,ii,partialGradients.getWeight(il,in,ii));
                    }
                }
            
            } // end for layer
        } // end foreach
        //return totalGradients;
    } // end method

    private void adaptation(SetOfIOPairs trainingSet, int maxK, double eps , double lambda , double micro) {
//         trainingSet  : trenovacia mnozina
//         maxK         : maximalny pocet iteracii
//         eps          : pozadovana presnost normovanej dlzky gradientu
//         lambda       : rychlost ucenia (0.1)
//         micro        : momentovy clen
        double delta;
        Gradients deltaGradients = new Gradients(this);
        Gradients totalGradients = new Gradients(this);
        Gradients partialGradients = new Gradients(this);
        
        System.out.println("setting up random weights and thresholds ...");
        
        // we set the thresholds and weights of the neural network to random values, reset the delta-gradients (they are reset at init)
        for (int il = this.numberOfLayers()-1; il >= 1; il--) { // iteration over all layers except the last one
            NeuralLayer currentLayer = this.getLayer(il);
            for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // for each neuron on the layer
                Neuron currentNeuron = currentLayer.getNeuron(in);
                currentNeuron.threshold = 2*this.random()-1;
                //deltaGradients.setThreshold(il,in,0.0);
                for (int ii = 0; ii < currentNeuron.numberOfInputs(); ii++) {
                    currentNeuron.getInput(ii).weight = 2*this.random()-1;
                    //deltaGradients.setWeight(il,in,ii,0.0);
                } // end ii
            } // end in
        } // end il
         
        int currK = 0; // citac iteracii
        double currE = Double.POSITIVE_INFINITY; // the beginning of the current accuracy will be infinite (the tendency of decay)
        
        System.out.println("entering adaptation loop ... (maxK = "+maxK+")");
        
        while ( currK < maxK && currE > eps ) {
            computeTotalGradient(totalGradients,partialGradients,trainingSet);
            for (int il = this.numberOfLayers()-1; il >= 1; il--) { // iteration over all layers except the last one
                NeuralLayer currentLayer = this.getLayer(il);            
                
                for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // for each neuron on the layer
                    Neuron currentNeuron = currentLayer.getNeuron(in);
                    delta = -lambda * totalGradients.getThreshold(il,in) + micro * deltaGradients.getThreshold(il,in);
                    currentNeuron.threshold += delta;
                    deltaGradients.setThreshold(il,in,delta);
                } // end for ii 1
                
                for (int in=0; in<currentLayer.numberOfNeurons(); in++) { // for each neuron on the layer
                    Neuron currentNeuron = currentLayer.getNeuron(in);
                    for (int ii = 0; ii < currentNeuron.numberOfInputs(); ii++) { // and for each neuron input
                        delta = -lambda * totalGradients.getWeight(il,in,ii) + micro * deltaGradients.getWeight(il,in,ii);
                        currentNeuron.getInput(ii).weight += delta;
                        deltaGradients.setWeight(il,in,ii,delta);
                    } // end for ii
                } // end for in 2
            } // end for il
            
            currE = totalGradients.getGradientAbs(); 
            currK++;
            if (currK%25==0) System.out.println("currK="+currK+"   currE="+currE);
        } // end while
    }
        
    private Vector<Double> activities (Vector<Double> inputs) {
        for (int il=0; il<this.numberOfLayers();il++) { // for each neuron layers
            for (int in=0; in<this.getLayer(il).numberOfNeurons();in++) { // for each neuron 
                double sum = this.getLayer(il).getNeuron(in).threshold; // sum <- threshold
                for (int ii=0; ii<this.getLayer(il).getNeuron(in).numberOfInputs(); ii++) { // input
                    // take the weight with the input
                    if (il==0) { // if we are on the lowest layer, weigh up the balances with the inputs
                        sum+=
                        this.getLayer(il).getNeuron(in).getInput(ii).weight *
                        inputs.elementAt(in).doubleValue();
                    } else { // on the upper layers, weigh the scales with the lower layer outputs
                        sum+=
                        this.getLayer(il).getNeuron(in).getInput(ii).weight *
                        this.getLayer(il-1).getNeuron(ii).output;
                    }
                }
                
                // !!!YOU HAVE DISCUSED IT ONLY ON THE FIRST COURSE:
    //            if (il == 0)
    //                  this.getLayer(il).getNeuron(in).output = sum; // vystup neuronu
    //            else 
                      this.getLayer(il).getNeuron(in).output = this.gainFunction(sum);
                
                //this.getLayer(il).getNeuron(in).output = this.gainFunction(sum); // vystup neuronu
            }
        }
        // the neuron outputs of the highest layer are written into the vector:
        Vector<Double> output = new Vector<Double>();
        
        for (int i=0; i<this.getLayer(this.numberOfLayers()-1).numberOfNeurons();i++) 
            output.add(this.getLayer(this.numberOfLayers()-1).getNeuron(i).output);
        
        return output;
    }
        
    private double gainFunction (double x) {
        return 1/(1+Math.exp(-x)); 
    }
    
    private NeuralLayer getLayer(int index) {
        return this.listLayers.elementAt(index);
    }
    
    

/*    public void printNeuralNetwork() {
        for (int il=0; il<this.numberOfLayers();il++) {
            System.out.println("Layer "+il);
            for (int in=0; in<this.getLayer(il).numberOfNeurons();in++) {
                System.out.print("      Neuron "+in+ " (threshold="+this.getLayer(il).getNeuron(in).threshold+") : ");
                for (int ii=0; ii<this.getLayer(il).getNeuron(in).numberOfInputs(); ii++) {
                    System.out.print(this.getLayer(il).getNeuron(in).getInput(ii).weight+" ");
                }
                System.out.println();
            }
        }
      }
 */
    
} // end class NeuralNetwork
