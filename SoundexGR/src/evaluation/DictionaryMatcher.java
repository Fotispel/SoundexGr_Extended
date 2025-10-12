/**
 *
 */
package evaluation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import client.Dashboard;
import utils.*;

import SoundexGR.SoundexGRExtra;

import static config.SoundexGrConfig.codesToWords;
import static config.SoundexGrConfig.getSelectedDatasetFile;

/**
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 */
public class DictionaryMatcher {
    public static List<String> rankedWords = new ArrayList<>();
    public static String FirstMatch = "";
    private static boolean FirstMatchFound = false;

    /**
     * Lookups a work in the dictionary
     *
     * @param word
     * @return
     */
    static boolean lookup(String word) {
        return DictionaryBasedMeasurements.lookup(word);
    }

    /**
     * finds those words with edit distance less than k
     *
     * @param word
     * @param K
     * @return
     */
    public static Set<String> getDicWordByEditDist(String word, int K) {
        Set<String> res = new HashSet<>();
        Set<String> dwords = DictionaryBasedMeasurements.getWords();
        // System.out.println("******"+ dwords.size());
        for (String dword : dwords) {
            if (EditDistance.EditDistDP(word, dword) <= K) {
                res.add(dword);
                // System.out.print("+");
            }
        }
        return res;
    }

    /**
     * Ranks a set of word by their edit distance withe one word
     *
     * @param word
     * @param wordsToRank
     * @return
     */
    public static Map<String, Integer> RankByEditDistance(String word, Set<String> wordsToRank) {
        Map<String, Integer> wordsAndDists = new HashMap<>();
        for (String candidateword : wordsToRank) {
            wordsAndDists.put(candidateword, EditDistance.EditDistDP(word, candidateword));
        }

        // sort the map wrt to the value in increasing order
        Map<String, Integer> sorted = wordsAndDists
                .entrySet()
                .stream()
                // .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e2, LinkedHashMap::new));

        if (!sorted.isEmpty() && !FirstMatchFound) {
            FirstMatch = sorted.keySet().iterator().next();
            FirstMatchFound = true;
        } else if (!FirstMatchFound) // if the map is empty
            FirstMatch = "";

        return sorted;

    }

    /**
     * Returns a string with the matching of a word, by using code length as defined
     * by the parameter
     *
     * @param word
     * @param codeLength the code length to be used
     * @return a verbose string with the results of the matchings
     */


    public static String getMatchings(String word, int codeLength, boolean isRunningDemo) {
        FirstMatchFound = false;

        // Φόρτωσε dictionary μόνο αν δεν έχει φορτωθεί ήδη
        if (codesToWords == null) {
            System.out.println("Loading dictionary for dataset: " + getSelectedDatasetFile() + " ...");
            String dictResourcePlace = "All datasets".equals(getSelectedDatasetFile())
                    ? "\\Resources\\collection_words\\All_datasets_words.dic"
                    : "\\Resources\\collection_words\\" + getSelectedDatasetFile() + "_words.txt";
            DictionaryBasedMeasurements.setDictionaryLocation(dictResourcePlace);
        }

        String output = "";

        // A. Check αν η λέξη υπάρχει στο λεξικό
        if (lookup(word)) {
            FirstMatch = word;
            FirstMatchFound = true;
            return "The word \"" + word + "\" exists in the dictionary.";
        } else {
            output = "APPROXIMATE MATCHES FOR " + word + "\n";
        }

        // B1. Code length
        SoundexGRExtra.LengthEncoding = codeLength;
        String wcode = SoundexGRExtra.encode(word);

        Set<String> wordsHavingTheSameCode = DictionaryBasedMeasurements.returnWordsHavingTheSameCode(wcode, codesToWords);
        ArrayList<String> matches = new ArrayList<>();

        if (wordsHavingTheSameCode != null) {
            matches.addAll(wordsHavingTheSameCode);

            output += "* Approximate Matches (words having the same SoundexGR code with \"" + word
                    + "\" with code length = " + codeLength + "): ";
            output += matches.size() + " matches\n";
            output += matches.toString() + "\n";

            // Ranking με βάση Edit Distance
            output += "\n* Ranking of the above " + matches.size()
                    + " words wrt Edit distance:\n";
            output += RankByEditDistance(word, wordsHavingTheSameCode).toString();
            output += "\n\n";

        } else {
            output += "No word with the same SoundexGR code was found. Try reducing the code length :(\n";
        }

        // Επιπλέον approximate matches με βάση Edit Distance απευθείας από το λεξικό
        int K = 3;
        output += "* Approximate Matches directly from the Dictionary ordered by Edit distance (less than " + K + "): ";
        Set<String> matchesByED = getDicWordByEditDist(word, K);
        output += matchesByED.size() + " matches\n";
        output += RankByEditDistance(word, matchesByED).toString();

        rankedWords = RankByEditDistance(word, matchesByED)
                .keySet()
                .stream()
                .limit(10)
                .collect(Collectors.toList());

        output += "\n";

        return output;
    }


    public static void main(String[] lala) throws IOException {

        String[] exampleWords = {"Γιάννης", "μύνημα", "διάλιμα"};

        for (int i = 0; i < exampleWords.length; i++) {
            String ex = exampleWords[i];
            String m = getMatchings(ex, 12, false);
            System.out.println(ex + "\t: " + m);
        }
    }

}
