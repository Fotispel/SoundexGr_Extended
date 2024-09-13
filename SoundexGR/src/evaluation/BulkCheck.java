/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package evaluation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import utils.MeasurementsWriter;
//import java.util.stream.Collectors;
import utils.Utilities;
//import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * @author: Antrei Kavros (additions by Yannis Tzitzikas)
 */

public class BulkCheck {
    public static int length_temp = 0;
    public static float[] general_word_length_per_code_length = new float[10]; // for storing the word length per code length

    static MeasurementsWriter mw = null; // for writing evaluation measurements in a file

    /**
     * It takes as input a response (res) and the set of correct answers (exp)
     * and computes the precision
     *
     * @param exp
     * @param res
     * @return the precision
     */
    public float getPrecision(LinkedHashSet<String> exp, ArrayList<String> res) {
        int counter = 0;
        for (String word : exp) {
            if (res.contains(word.trim())) {
                counter++;
            }
        }
        return res.size() == 0 ? 0 : counter / (float) res.size();
    }

    /**
     * It takes as input a response (res) and the set of correct answers (exp)
     * and computes the recall
     *
     * @param exp
     * @param res
     * @return
     */
    public float getRecall(LinkedHashSet<String> exp, ArrayList<String> res) {
        int counter = 0;
        for (String word : exp) {
            if (res.contains(word.trim())) {
                counter++;
            }
        }
        return counter / (float) exp.size();
    }

    /**
     * It computes precision/recall/f-measure
     *
     * @param utils
     * @param path        the file with the eval collection
     * @param type        the matching (soundex) algorith to be applied
     * @param fileToWrite the file to write (currently it just creates the file, it does not store anything there)
     * @param maxWordNum  max number of words to consider, if 0 no limit is applied
     * @throws FileNotFoundException
     * @throws IOException           NOTE: the maxWordNum should be considered also in the reading of the file (i.e. in method check)
     */
    public void check(Utilities utils, String path, String type, String fileToWrite, int maxWordNum) throws FileNotFoundException, IOException {
        int[][] matching_word_and_code_length = new int[5000][2]; // for storing the matching word and code length
        int counter_for_matching_word_and_code_length = 0; // counter for the above array

        boolean bounded = maxWordNum != 0;  // true if the max num of words should be applied

        FileReader fl = new FileReader(path);
        BufferedReader bfr = new BufferedReader(fl);
        String line;

        //FileWriter fr = new FileWriter(fileToWrite); // opens the file to write  (currently does not write anything)
        float total_pre = 0; // initialization of total precision
        float total_rec = 0; // initialization of total recall
        int counter = 0; // counts the number of buckets (i.e. the number of lines in the file)

        int numOfWords = 0; // ytz: 2021 for counting the words

        // reading the eval collection
        long start = System.nanoTime();

        while ((line = bfr.readLine()) != null) {
            length_temp = 0;

            String[] tmp2 = line.split(","); // reading the tokens of a line
            int suggested_code_length = -1;
            float max_f_score = -1;
            do {
                float precision_word=-1, recall_word=-1, f_score_word;
                if (!bounded) { // no bound on number of words
                    LinkedHashSet<String> tmp = new LinkedHashSet<>(Arrays.asList(tmp2)); // adding them to a hashset
                    ArrayList<String> res = utils.search(tmp2[0].trim(), type); // ...
                    precision_word = getPrecision(tmp, res);
                    recall_word = getRecall(tmp, res);
                    total_pre += precision_word; // adding the precision
                    total_rec += recall_word; // adding the recall
                    counter++;
                    numOfWords += tmp2.length;
                } else { // bounded number of word
                    if (numOfWords < maxWordNum) { //
                        LinkedHashSet<String> tmp = new LinkedHashSet<>();
                        for (String s : tmp2) {
                            if (numOfWords < maxWordNum) {
                                tmp.add(s);
                                numOfWords++;
                            }
                        }
                        ArrayList<String> res = utils.search(tmp2[0].trim(), type); // ...

                        precision_word = getPrecision(tmp, res);
                        recall_word = getRecall(tmp, res);
                        total_pre += precision_word; // adding the precision
                        total_rec += recall_word; // adding the recall
                        counter++;
                    } else { // have read the max words

                    }
                }
                f_score_word = 2 * precision_word * recall_word / (precision_word + recall_word);
                if (f_score_word > max_f_score) {
                    max_f_score = f_score_word;
                    suggested_code_length = SoundexGRExtra.LengthEncoding;
                }
                length_temp++;
            } while (length_temp <= 9);
            //System.out.println("Suggested code length: " + suggested_code_length + " F-score: " + max_f_score + " for word: " + tmp2[0]);
            matching_word_and_code_length[counter_for_matching_word_and_code_length][0] = tmp2[0].length();
            matching_word_and_code_length[counter_for_matching_word_and_code_length][1] = suggested_code_length;
            counter_for_matching_word_and_code_length++;
        }

        int count_words_per_code_length[] = new int[10]; // for counting the number of words per code length
        int sum_word_length_per_code_length[] = new int[10]; // for summing the word length per code length
        for (int i = 0; i < counter_for_matching_word_and_code_length; i++) {
            //System.out.println("Word length: " + matching_word_and_code_length[i][0] + " Suggested code length: " + matching_word_and_code_length[i][1]);
            count_words_per_code_length[matching_word_and_code_length[i][1]]++;
            sum_word_length_per_code_length[matching_word_and_code_length[i][1]] += matching_word_and_code_length[i][0];
        }
        float[] avg_word_length_per_code_length = new float[10];
        for (int i = 0; i < 10; i++) {
            avg_word_length_per_code_length[i] = (float) sum_word_length_per_code_length[i] / count_words_per_code_length[i];
            general_word_length_per_code_length[i] += avg_word_length_per_code_length[i];
            //System.out.println("Code length: " + i + " Average word length: " + avg_word_length_per_code_length[i]);
        }

        long end = System.nanoTime();
        long total = end - start;

        double elapsedTime = (double) total / 1000 * 1000 * 1000;
        float avgPrecision = total_pre / counter;    // computing the avg precision
        float avgRecall = total_rec / counter;        // computing the avg recall
        float avgFmeasure = 2 * (total_pre / counter) * (total_rec / counter) / ((total_pre / counter) + (total_rec / counter));

        /*
        if (SoundexGRExtra.LengthEncoding != SoundexGRSimple.LengthEncoding) { // if the config is not correct for experiments
            throw new RuntimeException("SoundexGRExtra.LengthEncoding!=SoundexGRSimple.LengthEncoding");
        }
        */

        mw.write(SoundexGRExtra.LengthEncoding + ","); // writing to file
        mw.write(avgPrecision + ",");
        mw.write(avgRecall + ",");
        mw.write(avgFmeasure + "\n");
        
        /*
        System.out.println("\tElapsed time     : " + elapsedTime);
        System.out.println("\tAverage Precision: " + avgPrecision); 
        System.out.println("\tAverage Recall   : " + avgRecall); 
        System.out.println("\tF-Measure        : " + avgFmeasure);
        System.out.println("\tNum of words checked: " + numOfWords);
        */
        System.out.format("NWords:%d \t Pre:%.3f Rec:%.3f F1:%.3f ", numOfWords, avgPrecision, avgRecall, avgFmeasure);

        //fr.close();  // closes the output file
    }


    /**
     * Performs experiments for various dataset sizes.
     * The control parameters are in the body of the method
     */

    public static void performExperimentsForDatasetSizes() {
        //MeasurementsWriter initialization and header
        mw = new MeasurementsWriter("Resources/measurements/currentMeasurements.csv");
        mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");

        // PARAMS of the experiments to run
        // Dataset sizes
        int dSizeMin = 10;   //100
        int dSizeMax = 100;  //3000
        int dSizeStep = 20;  //4000

        // Code sizes
        int codeSizeMin = 4; //100
        int codeSizeMax = 12; //3000

        for (int ds = dSizeMin; ds <= dSizeMax; ds += dSizeStep) { // datasetsizes
            for (int codeSize = codeSizeMin; codeSize <= codeSizeMax; codeSize++) {  // code sizes
                performExperiments(ds, codeSize);  // performs the experiments for size ds and code length codeSize
            }
        }
        // closing the measurements file
        mw.close(); // put in comments for dictionarybased
        System.out.println("COMPLETION.");
    }


    /**
     * Performs all the experiments
     *
     * @param maxWordNum max number of words from the dataset to be considered (use 0 for no limit in the number of words to be considered)
     * @param codeLength the length of the codes to be used
     */
    public static void performExperiments(int maxWordNum, int codeLength) {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        //MeasurementsWriter initialization and header
        if (mw == null) { // if already created
            String filename = "Resources/measurements/currentMeasurements.csv";
            System.out.println("Creating a new file: " + filename);
            mw = new MeasurementsWriter(filename);
            mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
        }

        String[] DatasetFiles = {
                "Resources/names/additions.txt",            // additions
                "Resources/names/subs.txt",                // subtitutions
                "Resources/names/deletions.txt",            // deletions
                "Resources/names/same_sounded.txt",        // same sounded
                "Resources/names/same_soundedExtended.txt"    // same sounded (extended)
                //"Resources/names/dictionaryBased.txt",       // dictionary Based (current)
                //"Resources/names/dictionaryBasedFull.txt",
                //"Resources/names/newcollection.txt"  // test purposes
        };  // evaluation collections

        String OptionsToEvaluate[] = {
                "exactMatch",
                "soundex",
                "original",
                "combine",
                "stemcase",
                "stemAndsoundex",
                "fullPhonetic",
                "editDistance1",
                "editDistance2",
                "editDistance3",
                "editDistance4"
        };  // all supported options


        //for setting the desired code length
        SoundexGRExtra.LengthEncoding = codeLength;
        SoundexGRSimple.LengthEncoding = codeLength;
        System.out.println("Indicative enconding: " + SoundexGRExtra.encode("Αυγο")); // for testing purposes
        //System.out.println(SoundexGRSimple.encode("Αυγο"));


        //String OptionsToEvaluate[] 	= { "soundex"};
        String outputFilePrefix = "Resources/names/results";   // prefixes of files for writing

        try {
            for (String datasetFile : DatasetFiles) { // for each dataset file
                if (maxWordNum == 0)
                    utils.readFile(datasetFile);
                else
                    utils.readFile(datasetFile, maxWordNum); // reads the dataset file (up to maxWordNum), 0: no limit
                System.out.println("[" + datasetFile + "]: ");

                for (String optionToEvaluate : OptionsToEvaluate) { // for each code generation option
                    // System.out.print("\tTesting *" + optionToEvaluate + "* " + "\tcodeLen=" + SoundexGRExtra.LengthEncoding +" \tmaxwords="+maxWordNum +"\t:");
                    System.out.format("\tTesting *%15s* codeLen=%d maxWords=%d ",
                            optionToEvaluate,
                            SoundexGRExtra.LengthEncoding,
                            maxWordNum);

                    mw.write(datasetFile + "," + maxWordNum + "," + optionToEvaluate + ","); // writing to measurement file

                    String outputFileName =
                            outputFilePrefix + "/output-" +
                                    datasetFile.substring(datasetFile.lastIndexOf('/') + 1); // the prefix + the last part of the dataset filename

                    //System.out.println(">>>>>"+outputFileName);
                    bulkCheckRun.check(utils, datasetFile, optionToEvaluate, outputFileName, maxWordNum);
                    System.out.println(""); //-------------------------------------------------");
                }
                utils.clear();
            }
        } catch (IOException ex) {
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        //mw.close(); // put in comments if you are not evaluating datasetsizes
    }


    /**
     * Comparing the performance of Stemming
     */
    public static void performExperimentsWithStemmer() {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        System.out.println("Evaluating the peformance of *stemming*");

        //MeasurementsWriter initialization and header
        if (mw == null) { // if not already created
            String filename = "Resources/measurements/currentMeasurements.csv";
            System.out.println("Creating a new file: " + filename);
            mw = new MeasurementsWriter(filename);
            mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
        }

        String DatasetFiles[] = {
                "Resources/names/additions.txt",        // additions
                "Resources/names/subs.txt",            // subtitutions
                "Resources/names/deletions.txt",        // deletions
                "Resources/names/same_sounded.txt",        // same sounded
                "Resources/names/same_soundedExtended.txt"        // same sounded (more)
        };  // evaluation collections

        String OptionsToEvaluate[] = {"stemcase"};
        String outputFile = "Resources/names/results/sames-stemmer.txt";   // file for writing

        try {
            for (String datasetFile : DatasetFiles) { // for each dataset file
                utils.readFile(datasetFile); // reads the dataset file
                System.out.println("[" + datasetFile + "]");
                for (String optionToEvaluate : OptionsToEvaluate) { // for each code generation option
                    System.out.println("Results  over " + datasetFile + " with the option *" + optionToEvaluate + "*:");
                    bulkCheckRun.check(utils, datasetFile, optionToEvaluate, outputFile, 0);
                    System.out.println("-------------------------------------------------");
                }
                utils.clear();
            }
        } catch (IOException ex) {
            System.out.println(ex);
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public static void print_fscores() {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        if (mw == null) { // if already created
            String filename = "Resources/measurements/currentMeasurements.csv";
            System.out.println("Creating a new file: " + filename);
            mw = new MeasurementsWriter(filename);
            mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
        }

        String DatasetFiles[] = {
                "Resources/names/additions.txt",        // additions
                "Resources/names/subs.txt",            // subtitutions
                "Resources/names/deletions.txt",        // deletions
                "Resources/names/same_sounded.txt",        // same sounded
                "Resources/names/same_soundedExtended.txt"        // same sounded (more)
        };

        try {
            for (String datasetFile : DatasetFiles) {
                utils.readFile(datasetFile);
                System.out.print("\n[" + datasetFile + "]\n");
                bulkCheckRun.check(utils, datasetFile, "soundex", "Resources/names/results/sames-soundex.txt", 0);
                utils.clear();
            }
            for (int i = 0; i < 10; i++) {
                System.out.println("Code length: " + i + " Average word length: " + general_word_length_per_code_length[i] / 5);
            }
        } catch (IOException ex) {
            System.out.println(ex);
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("\n\n");
    }


    public static void main(String[] args) {
        System.out.println("[BulkCheck]-start");

        // UNCOMMENT THE METHOD THAT YOU WANT TO RUN

        //performExperimentsWithStemmer();  // evaluation of a Greek stemmer   (status: ok)
        //performExperiments(0,4); // 1st arg. word limit, 2nd code length  (status: ok)
        //performExperimentsForDatasetSizes(); // performs experiments for various data sizes (status:ok)


        System.out.println("[BulkCheck]-complete");
    }
}
