/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package evaluation;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import utils.MeasurementsWriter;
import utils.Utilities;

import static client.Dashboard.getAppSoundexCodeLen;
import static client.Dashboard.setAppSoundexCodeLen;
import static config.SoundexGrConfig.*;
import static evaluation.DictionaryBasedMeasurements.buildCodeToWordsMap;
import static evaluation.DictionaryBasedMeasurements.returnWordsHavingTheSameCode;


/**
 * @author: Antrei Kavros (additions by Yannis Tzitzikas)
 */

public class BulkCheck {
    public static String[] DatasetFiles;
    public static ArrayList<String> DocNames = Read_and_Write_to_file();
    Map<Integer, Integer> length_per_docSize = new HashMap<>();
    public static List<String> datasetFileWords = new ArrayList<>();

    public static Map<String, Integer> length_per_DocName = new HashMap<>();

    static MeasurementsWriter mw = null; // for writing evaluation measurements in a file

    /**
     * It takes as input a response (res) and the set of correct answers (exp)
     * and computes the precision
     *
     * @param exp is the set of expected answers
     * @param res is the list of returned answers
     * @return the precision
     */
    public float getPrecision(LinkedHashSet<String> exp, ArrayList<String> res) {
        int counter = 0;
        for (String word : exp) {
            if (res.contains(word.trim())) {
                counter++;
            }
        }
        return res.isEmpty() ? 0 : counter / (float) res.size();
    }

    /**
     * It takes as input a response (res) and the set of correct answers (exp)
     * and computes the recall
     *
     * @param exp is the set of expected answers
     * @param res is the list of returned answers
     * @return the recall
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
     * A class to store a query token and its misspellings
     **/
    private static class Entry {
        String queryToken;
        LinkedHashSet<String> misspellings;

        Entry(String q, LinkedHashSet<String> m) {
            queryToken = q;
            misspellings = m;
        }
    }

    /**
     * It computes precision/recall/f-measure
     *
     * @param utils
     * @param misspellings_path the file with the eval collection
     * @param type              the matching (soundex) algorith to be applied
     * @param fileToWrite       the file to write (currently it just creates the
     *                          file, it does not store anything there)
     * @param maxWordNum        max number of words to consider, if 0 no limit is
     *                          applied
     * @throws FileNotFoundException
     * @throws IOException           NOTE: the maxWordNum should be considered also
     *                               in the reading of the file (i.e. in method
     *                               check)
     */
    public void check(Utilities utils, String misspellings_path, String type, String fileToWrite, int maxWordNum)
            throws FileNotFoundException, IOException {
        // FileWriter fr = new FileWriter(fileToWrite);

        float total_pre = 0;
        float total_rec = 0;
        int counter_words = 0;

        float max_f_score = -1;
        int length_for_max_f_score = -1;
        int numOfWords = 0;
        long start = System.nanoTime();

        switch (getSelectedMethod()) {
            case "M1 - Exhaustive length calculation":
                boolean bounded = maxWordNum != 0;
                Set<String> seenWords = new HashSet<>();

                List<Entry> entries = new ArrayList<>();
                try (BufferedReader bfr = new BufferedReader(new FileReader(misspellings_path))) {
                    String line;
                    while ((line = bfr.readLine()) != null) {
                        String[] tokens = line.split(",");
                        if (tokens.length == 0)
                            continue;

                        String first = tokens[0].trim();
                        if (seenWords.contains(first))
                            continue;
                        seenWords.add(first);

                        LinkedHashSet<String> expected = new LinkedHashSet<>();
                        for (String t : tokens) {
                            expected.add(t.trim());
                        }
                        entries.add(new Entry(first, expected));
                    }
                }

                for (int length_for_testing = 3; length_for_testing <= 15; length_for_testing++) {
                    seenWords.clear();
                    setAppSoundexCodeLen(length_for_testing);

                    String newds = "All datasets".equals(getSelectedDatasetFile())
                            ? "\\Resources\\collection_words\\All_datasets_words.txt"
                            : "\\Resources\\collection_words\\" + getSelectedDatasetFile() + "_words.txt";

                    codesToWords = DictionaryBasedMeasurements.buildCodeToWordsMap(newds);


                    counter_words = 0;

                    numOfWords = 0;
                    total_pre = 0;
                    total_rec = 0;

                    for (Entry e : entries) {
                        if (bounded && numOfWords >= maxWordNum)
                            break;

                        ArrayList<String> res = utils.search(e.queryToken, type);

                        float precision_word = getPrecision(e.misspellings, res);
                        float recall_word = getRecall(e.misspellings, res);

                        total_pre += precision_word;
                        total_rec += recall_word;
                        counter_words++;

                        if (bounded) {
                            numOfWords += e.misspellings.size();
                        }
                    }

                    float avgPrecision = total_pre / counter_words;
                    float avgRecall = total_rec / counter_words;
                    float avgFmeasure = 2 * avgPrecision * avgRecall / (avgPrecision + avgRecall);

                    //System.out.println("Length: " + length_for_testing + " -> F-measure: " + avgFmeasure);

                    if (avgFmeasure > max_f_score) {
                        max_f_score = avgFmeasure;
                        length_for_max_f_score = length_for_testing;
                    }
                }

                System.out.println("\nMax F-score: " + max_f_score + " for length " + length_for_max_f_score + " with "
                        + counter_words + " words");

                setAppSoundexCodeLen(length_for_max_f_score);

                String newds = "All datasets".equals(getSelectedDatasetFile())
                        ? "\\Resources\\collection_words\\All_datasets_words.txt"
                        : "\\Resources\\collection_words\\" + getSelectedDatasetFile() + "_words.txt";
                codesToWords = DictionaryBasedMeasurements.buildCodeToWordsMap(newds);


                length_per_DocName.put(getSelectedDatasetFile(), length_for_max_f_score);

                length_per_docSize.put(length_for_max_f_score,
                        getNumberOfTotalWords_of_DatasetFile(getSelectedDatasetFile()));
                // System.out.println("Length per docSize: " + length_per_docSize);

                if (mw == null) {
                    String filename = "Resources/measurements/currentMeasurements.csv";
                    mw = new MeasurementsWriter(filename);
                    mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
                }
                /*
                 * mw.write(avgPrecision + ",");
                 * mw.write(avgRecall + ",");
                 * mw.write(avgFmeasure + "\n");
                 * mw.write(SoundexGRExtra.LengthEncoding + ","); // writing to file
                 */

                break;
            case "M2 - Predefined length":
                int newLength = DictionaryBasedMeasurements
                        .calculatePredefinedLength();
                setAppSoundexCodeLen(newLength);
                System.out.println("Predefined length: " + newLength);

                //print_precision_recall_f1(misspellings_path, utils, type);
                break;
            case "M3 - Hybrid method i-ii":
                HybridMethod_execution(misspellings_path, null, null, utils);
                break;

            case "M4 - Hybrid method ii-iii":
                setAppSoundexCodeLen(DictionaryBasedMeasurements
                        .calculatePredefinedLength()); // setting predefined length

                int pre_length = getAppSoundexCodeLen();
                assert pre_length != -1; // if length = -1 then there is no suitable code length

                int[] lengthsForTesting;
                if (pre_length > 2) {
                    lengthsForTesting = new int[]{pre_length - 2,
                            pre_length - 1, pre_length,
                            pre_length + 1, pre_length + 2};
                } else if (pre_length > 1) {
                    lengthsForTesting = new int[]{pre_length - 1, pre_length,
                            pre_length + 1, pre_length + 2};
                } else {
                    lengthsForTesting = new int[]{pre_length, pre_length + 1,
                            pre_length + 2};
                }

                HybridMethod_execution(misspellings_path, lengthsForTesting, null, utils);
                break;

            default:
                throw new RuntimeException("No method selected");
        }

        int distinctWordsForSelectedDataset = getNumberOfDistinctWords_of_DatasetFile(getSelectedDatasetFile());
        System.out.println("Distinct words with length over two characters: " + distinctWordsForSelectedDataset);

        int numOfWords_for_selectedDataset = getNumberOfTotalWords_of_DatasetFile(getSelectedDatasetFile());
        System.out.println("Total number of words: " + numOfWords_for_selectedDataset);

        long end = System.nanoTime();
        long total = end - start;

        double elapsedTime = (double) total / (1000 * 1000 * 1000);
        System.out.println("Elapsed time: " + elapsedTime);
    }


    /**
     * Hybrid method execution
     *
     * @param misspellings_path the file with the eval collection
     * @param lengthsForTesting an array with the lengths to be tested
     * @param K_fixed           if not null then it is the fixed K value to be used, otherwise K is calculated
     * @throws IOException
     */
    public void HybridMethod_execution(String misspellings_path, int[] lengthsForTesting, Float K_fixed, Utilities utils)
            throws IOException {
        Map<Integer, List<Integer>> listSizesPerLength = new HashMap<>();
        Map<Integer, List<Set<String>>> SameCodeWords_per_length = new HashMap<>();

        if (lengthsForTesting == null) {
            lengthsForTesting = new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        }

        List<String> misspellingLines = Files.readAllLines(Paths.get(System.getProperty("user.dir"), misspellings_path));

        for (int length_word : lengthsForTesting) {
            setAppSoundexCodeLen(length_word);

            String dsPath = "All datasets".equals(getSelectedDatasetFile())
                    ? "\\Resources\\collection_words\\All_datasets_words.txt"
                    : "\\Resources\\collection_words\\" + getSelectedDatasetFile() + "_words.txt";

            buildCodeToWordsMap(dsPath);

            List<Integer> sizes = new ArrayList<>();
            Set<String> checked_codes = new HashSet<>();

            for (String line : misspellingLines) {
                String word = line.split(",")[0];
                String wcode = SoundexGRExtra.encode(word);

                if (!checked_codes.contains(wcode)) {
                    Set<String> wordsHavingTheSameCode = returnWordsHavingTheSameCode(wcode, codesToWords);
                    if (wordsHavingTheSameCode != null) {
                        sizes.add(wordsHavingTheSameCode.size());
                        List<Set<String>> words = SameCodeWords_per_length.getOrDefault(length_word, new ArrayList<>());
                        words.add(wordsHavingTheSameCode);
                        SameCodeWords_per_length.put(length_word, words);
                    }
                    checked_codes.add(wcode);
                }
            }
            listSizesPerLength.put(length_word, sizes);
        }

        float K;
        if (K_fixed != null) {
            K = K_fixed;
            System.out.println("Fixed K value: " + K);
        } else {
            float totalSum = 0;
            int count = 0;
            for (List<Integer> sizes : listSizesPerLength.values()) {
                for (int s : sizes) {
                    totalSum += s;
                    count++;
                }
            }
            K = count > 0 ? totalSum / count : 1.5f;
            System.out.println("Calculated K: " + K);
        }

        float[] avg_list_size = new float[30];

        for (int length : listSizesPerLength.keySet()) {
            List<Set<String>> words = SameCodeWords_per_length.get(length);
            int totalSize = 0;
            for (Set<String> wordSet : words) totalSize += wordSet.size();
            avg_list_size[length] = words.isEmpty() ? 0 : (float) totalSize / words.size();
        }

        int optimal_length = -1;
        float min_difference_from_K = Float.MAX_VALUE;

        for (int length : listSizesPerLength.keySet()) {
            float difference = Math.abs(K - avg_list_size[length]);
            if (difference < min_difference_from_K) {
                min_difference_from_K = difference;
                optimal_length = length;
            }
        }

        System.out.println("Optimal length for Hybrid method: " + optimal_length);

        setAppSoundexCodeLen(optimal_length);
        String dsPath = "All datasets".equals(getSelectedDatasetFile())
                ? "\\Resources\\collection_words\\All_datasets_words.txt"
                : "\\Resources\\collection_words\\" + getSelectedDatasetFile() + "_words.txt";

        buildCodeToWordsMap(dsPath);

        //print_precision_recall_f1(Paths.get(System.getProperty("user.dir"), misspellings_path).toString(), utils, "soundex");
    }


    /**
     * It computes and prints precision/recall/f-measure
     *
     * @param misspellings_path the file with the eval collection
     * @param utils             Utilities object
     * @param type              the matching (soundex) algorith to be applied
     * @throws IOException
     **/
    void print_precision_recall_f1(String misspellings_path, Utilities utils, String type) throws IOException {
        float totalPrecision = 0;
        float totalRecall = 0;
        int counter = 0;

        try (BufferedReader bfr = new BufferedReader(new FileReader(misspellings_path))) {
            String line;
            while ((line = bfr.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 0)
                    continue;

                LinkedHashSet<String> expected = new LinkedHashSet<>();
                for (String t : tokens) {
                    expected.add(t.trim());
                }

                String query = tokens[0].trim();

                ArrayList<String> res = utils.search(query, type);

                float p = getPrecision(expected, res);
                float r = getRecall(expected, res);

                totalPrecision += p;
                totalRecall += r;
                counter++;
            }
        }

        float avgPrecision = counter > 0 ? totalPrecision / counter : 0;
        float avgRecall = counter > 0 ? totalRecall / counter : 0;
        float f1 = (avgPrecision + avgRecall > 0) ? (2 * avgPrecision * avgRecall) / (avgPrecision + avgRecall) : 0;

        // System.out.println("Precision: " + avgPrecision);
        // System.out.println("Recall: " + avgRecall);
        System.out.println("F-score: " + f1);
    }

    /**
     * Performs experiments for various dataset sizes.
     * The control parameters are in the body of the method
     */

    public static void performExperimentsForDatasetSizes() {
        mw = new MeasurementsWriter("Resources/measurements/currentMeasurements.csv");
        mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");

        // PARAMS of the experiments to run
        // Dataset sizes
        int dSizeMin = 10; // 100
        int dSizeMax = 100; // 3000
        int dSizeStep = 20; // 4000

        // Code sizes
        int codeSizeMin = 4; // 100
        int codeSizeMax = 12; // 3000

        for (int ds = dSizeMin; ds <= dSizeMax; ds += dSizeStep) { // datasetsizes
            for (int codeSize = codeSizeMin; codeSize <= codeSizeMax; codeSize++) { // code sizes
                performExperiments(ds, codeSize); // performs the experiments for size ds and code length codeSize
            }
        }
        // closing the measurements file
        mw.close(); // put in comments for dictionarybased
        System.out.println("COMPLETION.");
    }

    /**
     * Performs all the experiments
     *
     * @param maxWordNum max number of words from the dataset to be considered (use
     *                   0 for no limit in the number of words to be considered)
     * @param codeLength the length of the codes to be used
     */
    public static void performExperiments(int maxWordNum, int codeLength) {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        // MeasurementsWriter initialization and header
        if (mw == null) { // if already created
            String filename = "Resources/measurements/currentMeasurements.csv";
            System.out.println("Creating a new file: " + filename);
            mw = new MeasurementsWriter(filename);
            mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
        }

        String[] DatasetFiles = {
                "Resources/names/additions.txt", // additions
                "Resources/names/subs.txt", // subtitutions
                "Resources/names/deletions.txt", // deletions
                "Resources/names/same_sounded.txt", // same sounded
                "Resources/names/same_soundedExtended.txt" // same sounded (extended)
                // "Resources/names/dictionaryBased.txt", // dictionary Based (current)
                // "Resources/names/dictionaryBasedFull.txt",
                // "Resources/names/newcollection.txt" // test purposes
        }; // evaluation collections

        String[] OptionsToEvaluate = {
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
        }; // all supported options

        // for setting the desired code length
        SoundexGRExtra.LengthEncoding = codeLength;
        SoundexGRSimple.LengthEncoding = codeLength;
        System.out.println("Indicative enconding: " + SoundexGRExtra.encode("Αυγο")); // for testing purposes
        // System.out.println(SoundexGRSimple.encode("Αυγο"));

        // String OptionsToEvaluate[] = { "soundex"};
        String outputFilePrefix = "Resources/names/results"; // prefixes of files for writing

        try {
            for (String datasetFile : DatasetFiles) { // for each dataset file
                if (maxWordNum == 0)
                    utils.readFile(datasetFile);
                else
                    utils.readFile(datasetFile, maxWordNum); // reads the dataset file (up to maxWordNum), 0: no limit
                System.out.println("[" + datasetFile + "]: ");

                for (String optionToEvaluate : OptionsToEvaluate) { // for each code generation option
                    // System.out.print("\tTesting *" + optionToEvaluate + "* " + "\tcodeLen=" +
                    // SoundexGRExtra.LengthEncoding +" \tmaxwords="+maxWordNum +"\t:");
                    System.out.format("\tTesting *%15s* codeLen=%d maxWords=%d ",
                            optionToEvaluate,
                            SoundexGRExtra.LengthEncoding,
                            maxWordNum);

                    mw.write(datasetFile + "," + maxWordNum + "," + optionToEvaluate + ","); // writing to measurement
                    // file

                    String outputFileName = outputFilePrefix + "/output-" +
                            datasetFile.substring(datasetFile.lastIndexOf('/') + 1); // the prefix + the last part of
                    // the dataset filename

                    // System.out.println(">>>>>"+outputFileName);
                    bulkCheckRun.check(utils, datasetFile, optionToEvaluate, outputFileName, maxWordNum);
                    System.out.println(); // -------------------------------------------------");
                }
                utils.clear();
            }
        } catch (IOException ex) {
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
        // mw.close(); // put in comments if you are not evaluating datasetsizes
    }

    /**
     * Comparing the performance of Stemming
     */
    public static void performExperimentsWithStemmer() {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        System.out.println("Evaluating the peformance of *stemming*");

        // MeasurementsWriter initialization and header
        if (mw == null) { // if not already created
            String filename = "Resources/measurements/currentMeasurements.csv";
            System.out.println("Creating a new file: " + filename);
            mw = new MeasurementsWriter(filename);
            mw.write("# datasetName, datasetSize, codeMethod, CodeSize, Precision, Recall, FScore\n");
        }

        DatasetFiles = new String[]{
                "Resources/names/additions.txt", // additions
                "Resources/names/subs.txt", // subtitutions
                "Resources/names/deletions.txt", // deletions
                "Resources/names/same_sounded.txt", // same sounded
                "Resources/names/same_soundedExtended.txt" // same sounded (more)
        };

        String[] OptionsToEvaluate = {"stemcase"};
        String outputFile = "Resources/names/results/sames-stemmer.txt"; // file for writing

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

    /**
     * Writes the words of each document to a separate file name_words.txt
     *
     * @param allWordsByDoc a set of words per document
     * @param DocNames      the names of the documents
     */
    private static void write_wordsOfDoc_to_files(Map<String, Set<String>> allWordsByDoc, ArrayList<String> DocNames) {
        try {
            if (allWordsByDoc.isEmpty()) {
                throw new RuntimeException("No words in the given document");
            }

            File collectionDir = new File("Resources/collection_words");
            if (!collectionDir.exists()) {
                boolean created = collectionDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + collectionDir.getAbsolutePath());
                }
            }

            Set<String> aggregated = new LinkedHashSet<>();
            for (String docName : DocNames) {
                String fileName = "Resources//collection_words//" + docName + "_words.txt";
                try (BufferedWriter br = new BufferedWriter(new FileWriter(fileName, false))) {
                    Set<String> words = allWordsByDoc.get(docName);
                    if (words == null) continue;
                    for (String word : words) {
                        // Remove parentheses and brackets
                        if (word.startsWith("(") || word.startsWith("[")) {
                            word = word.substring(1);
                        }
                        if (word.endsWith(")") || word.endsWith("]")) {
                            word = word.substring(0, word.length() - 1);
                        }

                        if (word.startsWith("\"") || word.startsWith("“") || word.startsWith("”")) {
                            word = word.substring(1);
                        }

                        if (word.endsWith("\"") || word.endsWith("“") || word.endsWith("”")) {
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

                        // Skip non-Greek words
                        if (!word.matches("[Α-Ωα-ωίϊΐόάέύϋΰήώΆΈΊΌΎΉΏ]*")) {
                            continue;
                        }

                        // Skip words with length 2 or less
                        if (word.length() <= 2) {
                            continue;
                        }

                        br.write(word + "\n");
                        aggregated.add(word);
                    }
                }
            }

            String aggregatedFile = "Resources//collection_words//All_datasets_words.txt";
            try (BufferedWriter br_all = new BufferedWriter(new FileWriter(aggregatedFile, false))) {
                for (String w : aggregated) {
                    br_all.write(w + "\n");
                }
            }


            String inputDic = "Resources//dictionaries//EN-winedt//gr.dic";
            String outputDic = "Resources//collection_words//gr_words.txt";

            try (
                    BufferedReader br = new BufferedReader(new FileReader(inputDic));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(outputDic, false))
            ) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();

                    if (line.startsWith("(") || line.startsWith("[")) {
                        line = line.substring(1);
                    }
                    if (line.endsWith(")") || line.endsWith("]")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    if (line.endsWith(",") || line.endsWith(".")) {
                        line = line.substring(0, line.length() - 1);
                    }
                    if (line.matches("[0-9]+")) {
                        continue;
                    }
                    if (!line.matches("[Α-Ωα-ωίϊΐόάέύϋΰήώΆΈΊΌΎΉΏ]*")) {
                        continue;
                    }
                    if (line.length() <= 2) {
                        continue;
                    }

                    bw.write(line + "\n");
                }
            }
        } catch (IOException ex) {
            System.out.println(ex);
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static ArrayList<String> Read_and_Write_to_file() {
        ArrayList<String> DocNames = new ArrayList<>();
        List<String> tokensOfDoc = null;
        Map<String, List<String>> allTokensByDoc = new HashMap<>(); // To store tokens per document
        Map<String, Set<String>> allWordsByDoc = new HashMap<>();
        IRSystem irs = new IRSystem();
        DocumentCorpus corpus = new DocumentCorpus("Resources//collection");
        irs.setCorpus(corpus);

        for (Document d : corpus.getDocs()) {
            String docURI = String.valueOf(d.uri);
            String docName = docURI.substring(docURI.lastIndexOf("/") + 1);

            if (docName.contains(".")) {
                docName = docName.substring(0, docName.lastIndexOf('.'));
            }

            if (docURI.contains("/demo/")) {
                continue;
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

    /**
     * Initializes the values and creates the misspellings files for executing the selected method and
     * finding the optimal length
     */
    public static void execute_selected_method() {
        Utilities utils = new Utilities();
        BulkCheck bulkCheckRun = new BulkCheck();

        try {
            String selectedDoc = getSelectedDatasetFile();
            String selectedWordsFile =
                    "All datasets".equals(selectedDoc)
                            ? "Resources//collection_words//All_datasets_words.txt"
                            : "Resources//collection_words//" + selectedDoc + "_words.txt";

            utils.readFile(selectedWordsFile);
            String input = utils.getContents(selectedWordsFile);
            ArrayList<String> tokens = Tokenizer.getTokens(input);

            StringBuilder output = new StringBuilder();
            for (String token : tokens) {
                output.append(token);
                for (String errorStr : DictionaryBasedMeasurements.returnVariations(token)) {
                    output.append(", ").append(errorStr);
                }
                output.append("\n");
            }
            String misspellingFile =
                    "All datasets".equals(selectedDoc)
                            ? "Resources/collection_words_misspellings/misspellings_All_datasets_words.txt"
                            : "Resources/collection_words_misspellings/misspellings_" + selectedDoc + "_words.txt";

            File misspellingsDir = new File("Resources/collection_words_misspellings");
            if (!misspellingsDir.exists()) {
                boolean created = misspellingsDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create directory: " + misspellingsDir.getAbsolutePath());
                }
            }

            utils.writeToFile(output.toString(), misspellingFile);

            String SelectedDatasetFile =
                    "All datasets".equals(getSelectedDatasetFile())
                            ? "Resources//collection_words//All_datasets_words.txt"
                            : "Resources//collection_words//" + getSelectedDatasetFile() + "_words.txt";
            System.out.println("\n[" + SelectedDatasetFile + "]: ");
            utils.readFile(misspellingFile);
            bulkCheckRun.check(utils, misspellingFile, "soundex", "Resources/names/results/sames-soundex.txt", 0);
            Toolkit.getDefaultToolkit().beep();
            utils.clear();
        } catch (IOException ex) {
            System.out.println(ex);
            Logger.getLogger(BulkCheck.class.getName()).log(Level.SEVERE, null, ex);
        }

        System.out.println("\n\n");
    }

    public static void main(String[] args) {
        System.out.println("[BulkCheck]-start");

        // UNCOMMENT THE METHOD THAT YOU WANT TO RUN

        // performExperimentsWithStemmer(); // evaluation of a Greek stemmer (status:
        // ok)
        // performExperiments(0,4); // 1st arg. word limit, 2nd code length (status: ok)
        // performExperimentsForDatasetSizes(); // performs experiments for various data
        // sizes (status:ok)

        System.out.println("[BulkCheck]-complete");
    }
}