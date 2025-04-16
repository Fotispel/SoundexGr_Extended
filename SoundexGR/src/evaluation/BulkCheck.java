/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package evaluation;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import client.Dashboard;
import utils.MeasurementsWriter;
//import java.util.stream.Collectors;
import utils.Utilities;

import javax.print.Doc;
//import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * @author: Antrei Kavros (additions by Yannis Tzitzikas)
 */

public class BulkCheck {
    public static String[] DatasetFiles;
    public static ArrayList<String> DocNames = Read_and_Write_to_file();
    Map<Integer, Integer> length_per_docSize = new HashMap<>();
    public static List<String> datasetFileWords = new ArrayList<>();

    public static ArrayList<String> DatasetFiles_Misspellings = new ArrayList<>();

    public static Map<String, Integer> length_per_DocName = new HashMap<>();


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
     * @param misspellings_path the file with the eval collection
     * @param type              the matching (soundex) algorith to be applied
     * @param fileToWrite       the file to write (currently it just creates the file, it does not store anything there)
     * @param maxWordNum        max number of words to consider, if 0 no limit is applied
     * @throws FileNotFoundException
     * @throws IOException           NOTE: the maxWordNum should be considered also in the reading of the file (i.e. in method check)
     */
    public void check(Utilities utils, String misspellings_path, String type, String fileToWrite, int maxWordNum, int file_index) throws FileNotFoundException, IOException {
        boolean bounded = maxWordNum != 0;  // true if the max num of words should be applied

        String line;

        //FileWriter fr = new FileWriter(fileToWrite); // opens the file to write  (currently does not write anything)
        float total_pre = 0; // initialization of total precision
        float total_rec = 0; // initialization of total recall
        int counter = 0; // counts the number of buckets (i.e. the number of lines in the file)
        int counter_words = 0; // counts the number of words

        float max_f_score = -1;
        int length_for_max_f_score = -1;
        int numOfWords = 0; // ytz: 2021 for counting the words
        long start = System.nanoTime();

        switch (Dashboard.getSelectedMethod()) {
            case "Real-time length calculation":
                System.out.println("Real-time length calculation");
                for (int length_for_testing = 3; length_for_testing <= 15; length_for_testing++) {
                    // reading the eval collection
                    SoundexGRExtra.LengthEncoding = length_for_testing;
                    counter_words = 0;

                    float avg_f_score = 0;
                    float total_f_score = 0;

                    FileReader fl = new FileReader(misspellings_path);
                    BufferedReader bfr = new BufferedReader(fl);

                    while ((line = bfr.readLine()) != null) {
                        counter_words++;

                        String[] tmp2 = line.split(","); // reading the tokens of a line

                        float precision_word = -1, recall_word = -1, f_score_word;
                        if (!bounded) { // no bound on number of words
                            LinkedHashSet<String> tmp = new LinkedHashSet<>(Arrays.asList(tmp2)); // adding them to a hashset
                            ArrayList<String> res = utils.search(tmp2[0].trim(), type);


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
                                ArrayList<String> res = utils.search(tmp2[0].trim(), type);

                                precision_word = getPrecision(tmp, res);
                                recall_word = getRecall(tmp, res);
                                total_pre += precision_word; // adding the precision
                                total_rec += recall_word; // adding the recall
                                counter++;
                            } else { // have read the max words

                            }
                        }
                        f_score_word = 2 * precision_word * recall_word / (precision_word + recall_word);
                        total_f_score += f_score_word;
                    }
                    bfr.close();

                    if (counter_words == 0) {
                        throw new RuntimeException("No words in the given document that can be used for evaluation");
                    } else {
                        avg_f_score = total_f_score / counter_words;
                    }

                    if (avg_f_score > max_f_score) {
                        max_f_score = avg_f_score;
                        length_for_max_f_score = length_for_testing;
                    }
                    System.out.println("\nMax F-score: " + max_f_score + " for length " + length_for_max_f_score + " with " + counter_words + " words");
                }

                System.out.println("\n\nMax F-score: " + max_f_score + " for length " + length_for_max_f_score + " with " + counter_words + " words");

                length_per_DocName.put(Dashboard.getSelectedDatasetFile(), length_for_max_f_score);

                length_per_docSize.put(length_for_max_f_score, Dashboard.getNumberOfWords_of_DatasetFile(Dashboard.getSelectedDatasetFile()));
                //System.out.println("Length per docSize: " + length_per_docSize);

                float avgPrecision = total_pre / counter;    // computing the avg precision
                float avgRecall = total_rec / counter;        // computing the avg recall
                float avgFmeasure = 2 * (total_pre / counter) * (total_rec / counter) / ((total_pre / counter) + (total_rec / counter));

                if (mw == null) {
                    String filename = "Resources/measurements/currentMeasurements.csv";
                    mw = new MeasurementsWriter(filename);
                    mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
                }


                mw.write(SoundexGRExtra.LengthEncoding + ","); // writing to file
                mw.write(avgPrecision + ",");
                mw.write(avgRecall + ",");
                mw.write(avgFmeasure + "\n");
                //System.out.format("NWords:%d \t Pre:%.3f Rec:%.3f F1:%.3f ", numOfWords, avgPrecision, avgRecall, avgFmeasure);
                break;
            case "Predefined length":
                SoundexGRExtra.LengthEncoding = DictionaryBasedMeasurements.calculateSuggestedCodeLen(Dashboard.getSelectedMethod());
                System.out.println("Predefined length and optimal length: " + SoundexGRExtra.LengthEncoding);
                break;
            case "Hybrid method i-ii":
                System.out.println("Hybrid method i-ii");
                HybridMethod_execution(file_index, misspellings_path, null);
                break;

            case "Hybrid method ii-iii":
                SoundexGRExtra.LengthEncoding = DictionaryBasedMeasurements.calculateSuggestedCodeLen("Predefined length");
                assert SoundexGRExtra.LengthEncoding != -1; //if length is -1 then there is no suitable code length

                int[] lengthsForTesting;
                if (SoundexGRExtra.LengthEncoding > 2) {
                    lengthsForTesting = new int[]{SoundexGRExtra.LengthEncoding - 2, SoundexGRExtra.LengthEncoding - 1, SoundexGRExtra.LengthEncoding, SoundexGRExtra.LengthEncoding + 1, SoundexGRExtra.LengthEncoding + 2};
                } else if (SoundexGRExtra.LengthEncoding > 1) {
                    lengthsForTesting = new int[]{SoundexGRExtra.LengthEncoding - 1, SoundexGRExtra.LengthEncoding, SoundexGRExtra.LengthEncoding + 1, SoundexGRExtra.LengthEncoding + 2};
                } else {
                    lengthsForTesting = new int[]{SoundexGRExtra.LengthEncoding, SoundexGRExtra.LengthEncoding + 1, SoundexGRExtra.LengthEncoding + 2};
                }

                HybridMethod_execution(file_index, misspellings_path, lengthsForTesting);
                break;

            default:
                throw new RuntimeException("No method selected");
        }

        long end = System.nanoTime();
        long total = end - start;

        double elapsedTime = (double) total / 1000 * 1000 * 1000;
        System.out.println("Elapsed time: " + elapsedTime);


        /*
        if (SoundexGRExtra.LengthEncoding != SoundexGRSimple.LengthEncoding) { // if the config is not correct for experiments
            throw new RuntimeException("SoundexGRExtra.LengthEncoding!=SoundexGRSimple.LengthEncoding");
        }
        */

        /*
        System.out.println("\tElapsed time     : " + elapsedTime);
        System.out.println("\tAverage Precision: " + avgPrecision); 
        System.out.println("\tAverage Recall   : " + avgRecall); 
        System.out.println("\tF-Measure        : " + avgFmeasure);
        System.out.println("\tNum of words checked: " + numOfWords);
        */

        //fr.close();  // closes the output file
    }

    public void HybridMethod_execution(int file_index, String misspellings_path, int[] lengthsForTesting) throws IOException {
        String docName = Dashboard.getSelectedDatasetFile();
        String line;
        String path_toWordsFile = "\\Resources\\collection_words\\" + docName + "_words.txt";
        Map<Integer, List<Set<String>>> SameCodeWords_per_length = new HashMap<>();

        if (lengthsForTesting == null) {
            lengthsForTesting = new int[]{2, 3, 4, 5, 6, 7, 8, 9};
        }

        for (int length_word = lengthsForTesting[0]; length_word <= lengthsForTesting[lengthsForTesting.length - 1]; length_word++) {
            SoundexGRExtra.LengthEncoding = length_word; // setting the length for encoding

            FileReader fl = new FileReader(misspellings_path);
            BufferedReader bfr = new BufferedReader(fl);
            ArrayList<String> checked_codes = new ArrayList<>();
            while ((line = bfr.readLine()) != null) {
                String word = line.split(",")[0];
                String wcode = SoundexGRExtra.encode(word);
                if (!checked_codes.contains(wcode)) {
                    Set<String> wordsHavingTheSameCode = DictionaryBasedMeasurements.returnWordsHavingTheSameCode(wcode, path_toWordsFile);

                    if (wordsHavingTheSameCode != null) {
                        // Add the words to the map using wcode as key
                        List<Set<String>> words;
                        if (SameCodeWords_per_length.containsKey(length_word)) {
                            words = SameCodeWords_per_length.get(length_word);
                        } else {
                            words = new ArrayList<>();
                        }
                        words.add(wordsHavingTheSameCode);
                        SameCodeWords_per_length.put(length_word, words);
                        //System.out.println("Length: " + length_word + " SameCodeWords_per_length: " + SameCodeWords_per_length.get(length_word));
                    }

                    checked_codes.add(wcode);
                }
            }
            bfr.close();
        }
        // Print words grouped by their wcode
        for (int length = lengthsForTesting[0]; length <= lengthsForTesting[lengthsForTesting.length - 1]; length++) {
            if (SameCodeWords_per_length.containsKey(length)) {
                List<Set<String>> words = SameCodeWords_per_length.get(length);
                //System.out.println("Words with length " + length + ": " + words);

            } else {
                //System.out.println("No words with length " + length);
            }
        }

        final float K = 1.5f; // Predefined optimal size of the list

        float[] avg_list_size = new float[10];

        // Calculate average list size for each length
        for (int length = lengthsForTesting[0]; length <= lengthsForTesting[lengthsForTesting.length - 1]; length++) {
            if (SameCodeWords_per_length.containsKey(length)) {
                List<Set<String>> words = SameCodeWords_per_length.get(length);

                // Sum the sizes of all sets
                int totalSize = 0;
                for (Set<String> wordSet : words) {
                    totalSize += wordSet.size();
                }

                // Calculate the average list size
                float avgSize = words.isEmpty() ? 0 : (float) totalSize / words.size();
                avg_list_size[length] = avgSize;

                // Print the average size for this encoding length
                //System.out.println("Average list size for length " + length + ": " + avg_list_size[length]);
            } else {
                //System.out.println("No words with length " + length);
            }
        }

        int optimal_length = -1;
        float min_difference_from_K = Float.MAX_VALUE;

        for (int length = lengthsForTesting[0]; length <= lengthsForTesting[lengthsForTesting.length - 1]; length++) {
            if (SameCodeWords_per_length.containsKey(length)) {
                float difference = Math.abs(K - avg_list_size[length]);
                //System.out.println("For length " + length + " the difference from K (=" + K + ") is " + difference);
                if (difference < min_difference_from_K) {
                    min_difference_from_K = difference;
                    optimal_length = length;
                }
            }
        }
        System.out.println("Optimal length for Hybrid method: " + optimal_length);
        Dashboard.appSoundexCodeLen = optimal_length;
        SoundexGRExtra.LengthEncoding = optimal_length;
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
                    bulkCheckRun.check(utils, datasetFile, optionToEvaluate, outputFileName, maxWordNum, 0);
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

        DatasetFiles = new String[]{
                "Resources/names/additions.txt",        // additions
                "Resources/names/subs.txt",            // subtitutions
                "Resources/names/deletions.txt",        // deletions
                "Resources/names/same_sounded.txt",        // same sounded
                "Resources/names/same_soundedExtended.txt"        // same sounded (more)
        };

        String OptionsToEvaluate[] = {"stemcase"};
        String outputFile = "Resources/names/results/sames-stemmer.txt";   // file for writing

        try {
            for (String datasetFile : DatasetFiles) { // for each dataset file
                utils.readFile(datasetFile); // reads the dataset file
                System.out.println("[" + datasetFile + "]");
                for (String optionToEvaluate : OptionsToEvaluate) { // for each code generation option
                    System.out.println("Results  over " + datasetFile + " with the option *" + optionToEvaluate + "*:");
                    bulkCheckRun.check(utils, datasetFile, optionToEvaluate, outputFile, 0, 0);
                    System.out.println("-------------------------------------------------");
                }
                utils.clear();
            }
        } catch (IOException ex) {
            System.out.println(ex);
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void write_wordsOfDoc_to_files
            (Map<String, Set<String>> allWordsByDoc, ArrayList<String> DocNames) {
        try {
            if (allWordsByDoc.isEmpty()) {
                throw new RuntimeException("No words in the given document");
            }

            for (String docName : DocNames) {
                String fileName = "Resources//collection_words//" + docName + "_words.txt";
                String fileName_all_words = "Resources//collection_words//collection_all_words.dic";
                FileWriter fr = new FileWriter(fileName);
                BufferedWriter br = new BufferedWriter(fr);

                FileWriter fr_all = new FileWriter(fileName_all_words, true);
                BufferedWriter br_all = new BufferedWriter(fr_all);


                Set<String> words = allWordsByDoc.get(docName);
                for (String word : words) {
                    // Remove parentheses and brackets
                    if (word.startsWith("(") || word.startsWith("[")) {
                        word = word.substring(1);
                    }
                    if (word.endsWith(")") || word.endsWith("]")) {
                        word = word.substring(0, word.length() - 1);
                    }

                    // Remove commas and periods
                    if (word.endsWith(",") || word.endsWith(".")) {
                        word = word.substring(0, word.length() - 1);
                    }

                    // Skip numbers
                    if (word.matches("[0-9]+")) {
                        continue;
                    }

                    //Skip non-Greek words
                    if (!word.matches("[Α-Ωα-ωίϊΐόάέύϋΰήώΆΈΊΌΎΉΏ]*")) {
                        continue;
                    }

                    if (word.length() < 3) {
                        continue;
                    }
                    br.write(word + "\n");
                    br_all.write(word + "\n");
                }

                br_all.close();
                fr_all.close();
                br.close();
                fr.close();
            }
        } catch (IOException ex) {
            System.out.println(ex);
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static ArrayList<String> Read_and_Write_to_file() {
        ArrayList<String> DocNames = new ArrayList<>();
        List<String> tokensOfDoc = null;
        Map<String, List<String>> allTokensByDoc = new HashMap<>();  // To store tokens per document
        Map<String, Set<String>> allWordsByDoc = new HashMap<>();
        IRSystem irs = new IRSystem();
        DocumentCorpus corpus = new DocumentCorpus("Resources//collection");
        irs.setCorpus(corpus);

        for (Document d : corpus.getDocs()) {
            String docURI = String.valueOf(d.uri);
            String docName = docURI.substring(docURI.lastIndexOf("/") + 1);

            if (docName.endsWith(".txt")) {
                docName = docName.replace(".txt", "");
            } else if (docName.endsWith(".pdf")) {
                docName = docName.replace(".pdf", "");

            }
            DocNames.add(docName);

            // Tokenize the document content
            tokensOfDoc = Tokenizer.getTokens(d.contents);

            // Save tokens in the map (List<String> for tokens)
            allTokensByDoc.put(docName, new ArrayList<>(tokensOfDoc));

            // Save unique words in the map (Set<String> for unique words)
            allWordsByDoc.put(docName, new HashSet<>(tokensOfDoc));
        }

        write_wordsOfDoc_to_files(allWordsByDoc, DocNames);
        return DocNames;
    }

    public static void execute_selected_method() {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        Set<String> commonwords = null;
        Map<Document, Double> scores = new HashMap<Document, Double>();

        int number_of_datasets = DatasetFiles.length;
        int file_index = 0;

        try {
            for (String FileWords : datasetFileWords) {
                utils.readFile(FileWords);
                String input = utils.getContents(FileWords);
                ArrayList<String> tokens = Tokenizer.getTokens(input);

                StringBuilder output = new StringBuilder();
                for (String token : tokens) {

                    output.append(token);
                    for (String errorStr : DictionaryBasedMeasurements.returnVariations(token)) {
                        output.append(", ").append(errorStr);
                        //System.out.println(output);
                    }
                    output.append("\n");
                }
                utils.writeToFile(output.toString(), "Resources//collection_words_misspellings//misspellings_" + FileWords.substring(FileWords.lastIndexOf("/") + 1));
            }


            //for (int j = 0; j < number_of_datasets; j++) {
            String misspellingFile = "Resources//collection_words_misspellings//misspellings_" + Dashboard.getSelectedDatasetFile() + "_words.txt";

            String SelectedDatasetFile = "Resources//collection_words//" + Dashboard.getSelectedDatasetFile() + "_words.txt";
            System.out.println("\n[" + SelectedDatasetFile + "]: ");
            utils.readFile(misspellingFile);  // Retrieve the file at index j
            bulkCheckRun.check(utils, misspellingFile, "soundex", "Resources/names/results/sames-soundex.txt", 0, file_index);
            file_index++;
            Toolkit.getDefaultToolkit().beep();
            utils.clear();
            //}

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
