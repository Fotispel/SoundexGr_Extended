package config;

import client.Dashboard;
import evaluation.DictionaryBasedMeasurements;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static evaluation.BulkCheck.DocNames;

public class SoundexGrConfig {
    public static Map<String, Set<String>> codesToWords = new HashMap<>();

    public static int appSoundexCodeLen = 6;

    public static String selectedDatasetFile = null;

    public static String selectedMethod = null;

    public static String getSelectedDatasetFile() {
        return selectedDatasetFile;
    }

    public static void setSelectedDatasetFile(String newSelectedDatasetFile) {
        String newds = Paths.get(System.getProperty("user.dir"),
                "Resources/collection_words/" + newSelectedDatasetFile + "_words.txt").toString();

        try {
            if (!newSelectedDatasetFile.equals("All datasets")) {
                codesToWords = DictionaryBasedMeasurements.buildCodeToWordsMap(newds);
            }
            selectedDatasetFile = newSelectedDatasetFile;
        } catch (IOException e) {
            System.err.println("Error while building code-to-words map for dataset: " + newSelectedDatasetFile);
            e.printStackTrace();
            codesToWords = new HashMap<>();
        }
    }

    public static String getSelectedMethod() {
        return selectedMethod;
    }

    public static void setSelectedMethod(String newSelectedMethod) {
        selectedMethod = newSelectedMethod;
    }

    public static int getNumberOfDistinctWords_of_DatasetFile(String docName) {
        File file = "All datasets".equals(docName)
                ? new File("Resources/collection_words/All_datasets_words.txt")
                : new File(Paths.get("Resources/collection_words/" + docName + "_words.txt").toString());
        Set<String> words = new HashSet<>();

        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNext()) {
                String w = sc.next().trim();
                if (!w.isEmpty()) {
                    words.add(w);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return words.size();
    }


    public static int getNumberOfTotalWords_of_DatasetFile(String docName) {
        int count = 0;

        String safeDocName = URLDecoder.decode(docName, StandardCharsets.UTF_8);

        if (Objects.equals(safeDocName, "All datasets")) {
            for (String dn : DocNames) {
                String safeDn = URLDecoder.decode(dn, StandardCharsets.UTF_8);
                File file = Paths.get("Resources", "collection", safeDn + ".txt").toFile();
                if (!file.exists()) {
                    file = Paths.get("Resources", "collection", safeDn + ".pdf").toFile();
                }
                count += getNumberOfTotalWords_of_File(file);
            }
        } else {
            File file = Paths.get("Resources", "collection", safeDocName + ".txt").toFile();
            if (!file.exists()) {
                file = Paths.get("Resources", "collection", safeDocName + ".pdf").toFile();
            }
            count += getNumberOfTotalWords_of_File(file);
        }

        return count;
    }


    private static int getNumberOfTotalWords_of_File(File file) {
        int count = 0;
        if (!file.exists()) return 0;

        try {
            if (file.getName().endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(document);

                    String[] words = text.trim().split("\\s+");
                    count = (text.isBlank()) ? 0 : words.length;
                }
            } else {
                try (Scanner sc = new Scanner(file)) {
                    while (sc.hasNext()) {
                        sc.next();
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }
}
