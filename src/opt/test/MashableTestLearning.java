package opt.test;

import dist.*;
import opt.*;
import opt.example.*;
import opt.ga.*;
import shared.*;
import func.nn.backprop.*;

import java.util.*;
import java.io.*;
import java.text.*;

/**
 * Implementation of randomized hill climbing, simulated annealing, and genetic algorithm to
 * find optimal weights to a neural network that is classifying abalone as having either fewer 
 * or more than 15 rings. 
 *
 * @author Hannah Lau
 * @version 1.0
 */
public class MashableTestLearning {
    private static Instance[] instances = initializeInstances();

    private static int inputLayer = 58, hiddenLayer = 30, outputLayer = 1, trainingIterations = 1000;
    private static BackPropagationNetworkFactory factory = new BackPropagationNetworkFactory();
    
    private static ErrorMeasure measure = new SumOfSquaresError();

    private static DataSet set = new DataSet(instances);

    private static BackPropagationNetwork networks[] = new BackPropagationNetwork[3];
    private static NeuralNetworkOptimizationProblem[] nnop = new NeuralNetworkOptimizationProblem[3];

    private static OptimizationAlgorithm[] oa = new OptimizationAlgorithm[3];
    private static String[] oaNames = {"RHC", "SA", "GA"};
    private static String results = "";

    private static DecimalFormat df = new DecimalFormat("0.000");
   

    public static void main(String[] args) {
        for(int i = 0; i < oa.length; i++) {
            networks[i] = factory.createClassificationNetwork(
                new int[] {inputLayer, hiddenLayer, outputLayer});
            nnop[i] = new NeuralNetworkOptimizationProblem(set, networks[i], measure);
        }

        oa[0] = new RandomizedHillClimbing(nnop[0]);
        oa[1] = new SimulatedAnnealing(1E11, .95, nnop[1]);
        oa[2] = new StandardGeneticAlgorithm(200, 100, 10, nnop[2]);

        for(int i = 0; i < oa.length; i++) {
            int[] trainingSize = {8000,10000,12000,14000,16000,18000,20000}; // vary the training size to get learning curve
            System.out.println("\nError results for " + oaNames[i] + "\n");
            for (int k = 0; k < trainingSize.length; k++ ) {
	            double start = System.nanoTime(), end, trainingTime, testingTime, correct = 0, incorrect = 0;
	            train(oa[i], networks[i], oaNames[i], trainingSize[k]); //trainer.train();
	            end = System.nanoTime();
	            trainingTime = end - start;
	            trainingTime /= Math.pow(10,9);
	
	            Instance optimalInstance = oa[i].getOptimal();
	            networks[i].setWeights(optimalInstance.getData());
	
	            double predicted, actual;
	            start = System.nanoTime();
	            for(int j = 20000; j < 25000; j++) {   // test with 5000 samples
	                networks[i].setInputValues(instances[j].getData());
	                networks[i].run();
	
	                predicted = Double.parseDouble(instances[j].getLabel().toString());
	                actual = Double.parseDouble(networks[i].getOutputValues().toString());
	
	                double trash = Math.abs(predicted - actual) < 0.5 ? correct++ : incorrect++;
	
	            }
	            end = System.nanoTime();
	            testingTime = end - start;
	            testingTime /= Math.pow(10,9);
	
//	            results +=  "\nResults for " + oaNames[i] + ": \nCorrectly classified " + correct + " instances." +
//	                        "\nIncorrectly classified " + incorrect + " instances.\nPercent correctly classified: "
//	                        + df.format(correct/(correct+incorrect)*100) + "%\nTraining time: " + df.format(trainingTime)
//	                        + " seconds\nTesting time: " + df.format(testingTime) + " seconds\n";
	            
	            String result =  "," + df.format(correct/(correct+incorrect)*100) ;
	            try {
	                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("learning_results.csv", true)));
	                out.println(result);
	                out.close();
	            } catch (IOException e) {
	                //exception handling left as an exercise for the reader
	            }
	            System.out.println(result);
            }
        }

        // System.out.println(results);
    }

    private static void train(OptimizationAlgorithm oa, BackPropagationNetwork network, String oaName, int trainingSize) {
            //System.out.println("\nError results for " + oaName + /*"for  iterations "+ iterations[k]  + */ "\n---------------------------");
        double correct = 0, incorrect = 0;    
    	for(int i = 0; i < trainingIterations; i++) {
	            oa.train();
	
	            double error = 0;
	            double predicted, actual;
	            correct = 0;
	            incorrect = 0;
	            for(int j = 0; j < trainingSize; j++) {
	                network.setInputValues(instances[j].getData());
	                network.run();
	
	                Instance output = instances[j].getLabel(), example = new Instance(network.getOutputValues());
	                example.setLabel(new Instance(Double.parseDouble(network.getOutputValues().toString())));
	                error += measure.value(output, example);
	                
	                predicted = Double.parseDouble(instances[j].getLabel().toString());
	                actual = Double.parseDouble(network.getOutputValues().toString());
	
	                double trash = Math.abs(predicted - actual) < 0.5 ? correct++ : incorrect++;
	            }

	            System.out.println(df.format(error));
	        }
    		// get error rate from the last iteration 
	        String result = trainingSize + "," + df.format(correct/(correct+incorrect)*100) ;
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("learning_results.csv", true)));
                out.print(result);
                out.close();
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
    }

    private static Instance[] initializeInstances() {

        double[][][] attributes = new double[25000][][];

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File("src/opt/test/mashable.csv")));
            Scanner scan = new Scanner(br.readLine());
            scan.nextLine();
            
            for(int i = 0; i < attributes.length; i++) {
                scan = new Scanner(br.readLine());
                scan.useDelimiter(",");

                attributes[i] = new double[2][];
                attributes[i][0] = new double[58]; // 58 attributes
                attributes[i][1] = new double[1];

                for(int j = 0; j < 58; j++)
                    attributes[i][0][j] = Double.parseDouble(scan.next());

                String temp = scan.next();
                if ("popular". equals(temp)) {
                	attributes[i][1][0] = 1.0;
                } else {
                	attributes[i][1][0] = 0.0;
                }
                
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        Instance[] instances = new Instance[attributes.length];

        for(int i = 0; i < instances.length; i++) {
            instances[i] = new Instance(attributes[i][0]);
            instances[i].setLabel(new Instance(attributes[i][1][0]));
        }

        return instances;
    }
}
