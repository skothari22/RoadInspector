package org.roadinspector.centralstation.imageanalysis;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.util.Vector;
import org.roadinspector.centralstation.intelligence.Intelligence;

public class CarSnapshot extends Photo {
    private static int distributor_margins = 
            Intelligence.configurator.getIntProperty("carsnapshot_distributormargins");
    private static int carsnapshot_graphrankfilter =
            Intelligence.configurator.getIntProperty("carsnapshot_graphrankfilter");

    static private int numberOfCandidates = Intelligence.configurator.getIntProperty("intelligence_numberOfBands");
    private CarSnapshotGraph graphHandle = null;

    
    public static Graph.ProbabilityDistributor distributor = new Graph.ProbabilityDistributor(0,0,distributor_margins,distributor_margins);
    
    public CarSnapshot() {
    }
    
    public CarSnapshot(String filepath) throws IOException {
        super(filepath);
    }
    
    public CarSnapshot(BufferedImage bi) {
        super(bi);
    }

    public BufferedImage renderGraph() {
        this.computeGraph();
        return graphHandle.renderVertically(100, this.getHeight());
    }    
    
    private Vector<Graph.Peak> computeGraph() {
        if (graphHandle != null) return graphHandle.peaks; // the graph was used

        BufferedImage imageCopy = this.duplicateBufferedImage(this.image);
        verticalEdgeBi(imageCopy); 
        thresholding(imageCopy); 

        graphHandle = this.histogram(imageCopy);
        graphHandle.rankFilter(carsnapshot_graphrankfilter);
        graphHandle.applyProbabilityDistributor(distributor);
        
        graphHandle.findPeaks(numberOfCandidates); //sort by height
        return graphHandle.peaks;
    }
        
    public Vector<Band> getBands() {
        Vector<Band> out = new Vector<Band>();

        Vector<Graph.Peak> peaks = computeGraph();
        
        for (int i=0; i<peaks.size(); i++) {
            // cut off from the flood! image of the mark, and save it to the vector. 
            // WARNING !!!!!! We drop out of the flood, so we have to apply inverse transformation to the co-ordinates out of imageCopy
            Graph.Peak p = peaks.elementAt(i);
            out.add(new Band(
                    image.getSubimage(  0  ,
                    (int) (p.getLeft())  ,
                    image.getWidth()  ,
                    (int) (p.getDiff()  )
                    ))
                    );
        }
        return out;
        
    }
    
    public void verticalEdgeBi(BufferedImage image) {
        BufferedImage imageCopy = duplicateBufferedImage(image);
        
        float data[] = {
            -1,0,1,
            -1,0,1,
            -1,0,1,
            -1,0,1
        };
        
        new ConvolveOp(new Kernel(3, 4, data), ConvolveOp.EDGE_NO_OP, null).filter(imageCopy, image);
    }
//    public void verticalRankBi(BufferedImage image) {
//        BufferedImage imageCopy = duplicateBi(image);
//        
//        float data[] = new float[9];
//        for (int i=0; i<data.length; i++) data[i] = 1.0f/data.length;
//        
//        new ConvolveOp(new Kernel(1,data.length, data), ConvolveOp.EDGE_NO_OP, null).filter(imageCopy, image);
//    }
    
    public CarSnapshotGraph histogram(BufferedImage bi) {
        CarSnapshotGraph graph = new CarSnapshotGraph(this);
        for (int y=0; y<bi.getHeight(); y++) {
            float counter = 0;
            for (int x=0; x<bi.getWidth();x++)
                counter += getBrightness(bi,x,y);
            graph.addPeak(counter);
        }
        return graph;        
    }
}
