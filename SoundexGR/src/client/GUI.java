/**
 *
 */
package client;

/**
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 */


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import evaluation.BulkCheck;
import evaluation.DictionaryBasedMeasurements;
import evaluation.DictionaryMatcher;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import utils.Tokenizer;

import static client.Dashboard.*;
import static config.SoundexGrConfig.*;


/**
 * AppController: The controller of the graphical add
 *
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 */

class AppController implements ActionListener {

    /**
     * The method where all GUI actions are sent
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        // Clear BUTTON
        if (event.getSource() == Dashboard.clearB) {
            //System.out.println("Clear Pressed");
            if (Dashboard.textInputArea.getText().equals(""))
                Dashboard.textInputArea.setText(GUI.exampleInputString);
            else
                Dashboard.textInputArea.setText("");
        }

        // Set Output as Input Clear BUTTON
        if (event.getSource() == Dashboard.swapB) {
            Dashboard.textInputArea.setText(Dashboard.textOutputArea.getText());
        }


        // Live Demo
        if (event.getSource() == Dashboard.demoB) {
            int backup_length_exit = getAppSoundexCodeLen();
            String backup_selected_dataset_exit = getSelectedDatasetFile();
            String backup_method_exit = getSelectedMethod();

            setSelectedDatasetFile("gr");
            setSelectedMethod("Predefined length");
            int newLen = DictionaryBasedMeasurements.calculatePredefinedLength();
            setAppSoundexCodeLen(newLen);

            System.out.println("Changed settings for demo: dataset=" + getSelectedDatasetFile() +
                    ", method=" + getSelectedMethod() +
                    ", length=" + getAppSoundexCodeLen()
            );

            // Create new frame
            JFrame demoFrame = new JFrame("Demo Text Panel");
            demoFrame.setSize(800, 600);
            demoFrame.setLocationRelativeTo(null); // center on screen

            CardLayout layout = new CardLayout();
            JPanel cardPanel = new JPanel(layout);

            // Text Area with scroll
            JTextArea demoTextArea = new JTextArea();
            demoTextArea.setLineWrap(true);
            demoTextArea.setWrapStyleWord(true);
            demoTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 18));

            JScrollPane scrollPane = new JScrollPane(demoTextArea);
            scrollPane.setPreferredSize(new Dimension(780, 510));

            // Panel to hold buttons (words)
            JPanel wordButtonsPanel = new JPanel(new FlowLayout());

            JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

            JPanel searchAndTextPanel = new JPanel(new BorderLayout());
            searchAndTextPanel.add(searchPanel, BorderLayout.NORTH); // search panel
            searchAndTextPanel.add(scrollPane, BorderLayout.CENTER); // text area

            cardPanel.add(searchAndTextPanel, "TEXT_AREA");
            cardPanel.add(wordButtonsPanel, "BUTTONS");

            JButton textCorrectionButton = new JButton("Text Correction");

            JCheckBox editTextCheckBox = new JCheckBox("Edit Text", true);
            editTextCheckBox.addActionListener(e -> demoTextArea.setEditable(editTextCheckBox.isSelected()));

            textCorrectionButton.addActionListener(e -> {
                System.out.println("Text Correction with length " + getAppSoundexCodeLen() +
                        " and dataset " + getSelectedDatasetFile());
                String inputText = demoTextArea.getText();
                StringBuilder outputText = new StringBuilder();

                ArrayList<String> tokens = new ArrayList<>();

                Matcher matcher = Pattern.compile("[\\p{L}]+|[.,!?;:{}()/<>&]").matcher(inputText);
                while (matcher.find()) {
                    tokens.add(matcher.group());
                }

                for (String token : tokens) {
                    if (token.matches("[.,!?;:{}()/<>&]")) {
                        outputText.append(token);
                    } else {
                        if (outputText.length() > 0 && outputText.charAt(outputText.length() - 1) != ' ') {
                            outputText.append(" ");
                        }

                        if (token.length() < 3) {
                            outputText.append(token);
                            continue;
                        }

                        String output = DictionaryMatcher.getMatchings(token, getAppSoundexCodeLen());
                        String firstMatch = (DictionaryMatcher.FirstMatch != null && !DictionaryMatcher.FirstMatch.isEmpty())
                                ? DictionaryMatcher.FirstMatch
                                : token;

                        if (Character.isUpperCase(token.charAt(0))) {
                            firstMatch = firstMatch.substring(0, 1).toUpperCase() + firstMatch.substring(1);
                        }

                        outputText.append(firstMatch);
                    }
                }

                System.out.println("Demo output: " + outputText);
                demoTextArea.setText(outputText.toString());
            });


            JTextField searchField = new JTextField(20);
            JButton searchButton = new JButton("Search");


            searchButton.addActionListener(e -> {
                int backup_length_correction = getAppSoundexCodeLen();
                String backup_selected_dataset_correction = getSelectedDatasetFile();
                String backup_method_correction = getSelectedMethod();
                System.out.println("Search pressed in Demo with length " + getAppSoundexCodeLen() +
                        " and dataset " + getSelectedDatasetFile()
                );

                String query = searchField.getText().trim();
                if (query.isEmpty()) return;

                String inputText = demoTextArea.getText();
                Highlighter highlighter = demoTextArea.getHighlighter();
                highlighter.removeAllHighlights();

                File collectionDir = new File("Resources/collection/demo");
                if (!collectionDir.exists()) {
                    boolean created = collectionDir.mkdirs();
                    if (!created) {
                        System.err.println("Failed to create directory: " + collectionDir.getAbsolutePath());
                        return;
                    }
                }


                try (FileWriter writer = new FileWriter("Resources\\collection\\demo\\search_ds.txt")) {
                    writer.write(inputText);
                    System.out.println("Saved text area content to search_ds.txt");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }


                // --- Process demo search file ---
                File demoFile = new File("Resources/collection/demo/search_ds.txt");
                if (demoFile.exists()) {
                    String demoOutput = "Resources/collection_words/search_ds_words.txt";
                    try (BufferedReader reader = new BufferedReader(new FileReader(demoFile));
                         BufferedWriter writer = new BufferedWriter(new FileWriter(demoOutput, false))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Διαχωρισμός λέξεων (όπως στα υπόλοιπα docs)
                            String[] tokens = line.split("\\s+");
                            for (String word : tokens) {
                                // Remove parentheses and brackets
                                if (word.startsWith("(") || word.startsWith("[")) {
                                    word = word.substring(1);
                                }
                                if (word.endsWith(")") || word.endsWith("]")) {
                                    word = word.substring(0, word.length() - 1);
                                }

                                // Remove quotes
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
                                if (word.matches("[0-9]+")) continue;

                                // Skip non-Greek words
                                if (!word.matches("[Α-Ωα-ωίϊΐόάέύϋΰήώΆΈΊΌΎΉΏ]*")) continue;

                                // Skip very short words
                                if (word.length() <= 2) continue;

                                writer.write(word + "\n");
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println("Demo search file not found: " + demoFile.getAbsolutePath());
                }

                setSelectedDatasetFile("search_ds");
                DictionaryBasedMeasurements.calculatePredefinedLength();

                DictionaryMatcher.getMatchings(query, getAppSoundexCodeLen());
                ArrayList<String> similarWords = new ArrayList<>(DictionaryMatcher.rankedWords);

                int limit = Math.min(similarWords.size(), 5);
                similarWords = new ArrayList<>(similarWords.subList(0, limit));

                Matcher matcher = Pattern.compile("[\\p{L}]+|[.,!?;:{}()/<>&]").matcher(inputText);
                while (matcher.find()) {
                    String word = matcher.group();

                    for (String candidate : similarWords) {
                        if (word.equalsIgnoreCase(candidate)) {
                            try {
                                highlighter.addHighlight(
                                        matcher.start(),
                                        matcher.end(),
                                        new DefaultHighlighter.DefaultHighlightPainter(ColorMgr.colorButtonMatch)
                                );
                            } catch (BadLocationException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }

                System.out.println("Search query: " + query + " | Found matches: " + similarWords);

                RestoreSettings(
                        backup_selected_dataset_correction,
                        backup_method_correction,
                        backup_length_correction
                );
            });


            searchPanel.add(textCorrectionButton);
            searchPanel.add(editTextCheckBox);
            searchPanel.add(new JLabel("Search:"));
            searchPanel.add(searchField);
            searchPanel.add(searchButton);


            editTextCheckBox.addActionListener(e -> {
                boolean selected = editTextCheckBox.isSelected();
                if (selected) {
                    //System.out.println("Edit Text selected");
                    layout.show(cardPanel, "TEXT_AREA");
                } else {
                    //System.out.println("Edit Text not selected");
                    wordButtonsPanel.removeAll();
                    String inputText = demoTextArea.getText();
                    ArrayList<String> tokens = Tokenizer.getTokens(inputText);
                    for (String token : tokens) {
                        JButton wordButton = new JButton(token);
                        wordButtonsPanel.add(wordButton);

                        wordButton.addActionListener(er -> {
                                    JFrame wordFrame = new JFrame("Word Info: " + token);
                                    wordFrame.setSize(400, 200);
                                    wordFrame.setLocationRelativeTo(null);

                                    String res = DictionaryMatcher.getMatchings(token, getAppSoundexCodeLen());
                                    JPanel buttonsMatchingPanel = new JPanel(new FlowLayout());
                                    for (String matching : DictionaryMatcher.rankedWords) {
                                        JButton matchingButton = new JButton(matching);
                                        matchingButton.setBackground(ColorMgr.colorButtonMatch);
                                        matchingButton.setForeground(Color.black);

                                        matchingButton.addActionListener(err -> {
                                            String newWord = matchingButton.getText();
                                            String oldWord = wordButton.getText();


                                            if (oldWord.equals(oldWord.toUpperCase())) {
                                                newWord = newWord.toUpperCase();
                                            } else if (!oldWord.isEmpty() && Character.isUpperCase(oldWord.charAt(0))) {
                                                newWord = newWord.substring(0, 1).toUpperCase() + newWord.substring(1);
                                            }

                                            String outputText = demoTextArea.getText();
                                            outputText = outputText.replace(oldWord, newWord);
                                            demoTextArea.setText(outputText);

                                            wordButton.setText(newWord);
                                        });


                                        buttonsMatchingPanel.add(matchingButton);
                                    }

                                    wordFrame.add(buttonsMatchingPanel);
                                    wordFrame.setVisible(true);
                                }

                        );
                    }
                    wordButtonsPanel.revalidate();
                    wordButtonsPanel.repaint();
                    layout.show(cardPanel, "BUTTONS");
                }
            });


            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> {
                RestoreSettings(
                        backup_selected_dataset_exit,
                        backup_method_exit,
                        backup_length_exit
                );
                demoFrame.dispose();
            });

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.add(closeButton);
            controlPanel.add(textCorrectionButton);
            controlPanel.add(editTextCheckBox);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(cardPanel, BorderLayout.CENTER);
            panel.add(controlPanel, BorderLayout.SOUTH);

            demoFrame.add(panel);
            demoFrame.setVisible(true);

            demoFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    RestoreSettings(
                            backup_selected_dataset_exit,
                            backup_method_exit,
                            backup_length_exit
                    );
                }
            });

        }

        // Code Length
        if (event.getSource() == Dashboard.codeLenghtsC) {
            int lenBefore = getAppSoundexCodeLen();
            String selected = (String) Dashboard.codeLenghtsC.getSelectedItem();
            assert selected != null;
            int selectedInt = Integer.parseInt(selected);
            DictionaryBasedMeasurements.invalidateMap();
            setAppSoundexCodeLenAndRefresh(selectedInt);
            System.out.printf("Code length changed from %d to %d.", lenBefore, getAppSoundexCodeLen());
        }

        // SOUNDEX EXTRA
        if (event.getSource() == Dashboard.soundexB) {
            //System.out.println("SoundexGR Pressed");
            ArrayList<String> tokens = Tokenizer.getTokens(
                    Dashboard.textInputArea.getText()
            );
            String outputStr = "";
            for (String token : tokens) {

                outputStr = outputStr.concat(" " +
                        SoundexGRExtra.encode(token)
                );
            }
            Dashboard.textOutputArea.setText(outputStr);
            Dashboard.textOutputArea.setCaretPosition(0);
        }


        // SOUNDEX Naive
        if (event.getSource() == Dashboard.soundexNaiveB) {
            //System.out.println("SoundexGR Naive Pressed");
            ArrayList<String> tokens = Tokenizer.getTokens(
                    Dashboard.textInputArea.getText()
            );
            String outputStr = "";
            for (String token : tokens) {

                outputStr = outputStr.concat(" " +
                        SoundexGRSimple.encode(token)
                );
            }
            Dashboard.textOutputArea.setText(outputStr);
            Dashboard.textOutputArea.setCaretPosition(0);
        }

        // Phonetic
        if (event.getSource() == Dashboard.pnoneticB) {
            //System.out.println("Phonetic Transcription  Pressed");
            ArrayList<String> tokens = Tokenizer.getTokens(
                    Dashboard.textInputArea.getText()
            );
            String outputStr = "";
            for (String token : tokens) {

                outputStr = outputStr.concat(" " +
                        SoundexGRExtra.phoneticTrascription(token)
                );
            }
            Dashboard.textOutputArea.setText(outputStr);
            Dashboard.textOutputArea.setCaretPosition(0);
        }

        //applyAllB
        if (event.getSource() == Dashboard.applyAllB) {
            //System.out.println("Apply all pressed");
            ArrayList<String> tokens = Tokenizer.getTokens(
                    Dashboard.textInputArea.getText()
            );
            String strFormat = "%14s ->  %s   %s  %s";

            String outputStr = String.format(strFormat, "word", "SGR", "SGRNv", "Phonetic");
            for (String token : tokens) {
                String output = String.format(strFormat,
                        token,
                        SoundexGRExtra.encode(token),
                        SoundexGRSimple.encode(token),
                        SoundexGRExtra.phoneticTrascription(token)
                );
                outputStr = outputStr.concat("\n" + output);
            }
            Dashboard.textOutputArea.setText(outputStr);
            Dashboard.textOutputArea.setCaretPosition(0);
        }

        //produceErrosB
        if (event.getSource() == Dashboard.produceErrosB) {
            //System.out.println("Misspellings");
            ArrayList<String> tokens = Tokenizer.getTokens(
                    Dashboard.textInputArea.getText()
            );

            String output = "";
            for (String token : tokens) {
                output += token + ":";
                for (String errorStr : DictionaryBasedMeasurements.returnVariations(token)) {
                    output += " " + errorStr;
                    //System.out.println(output);
                }
                output += "\n";
            }
            Dashboard.textOutputArea.setText(output);
            Dashboard.textOutputArea.setCaretPosition(0);
        }

        // dictionarylookup
        if (event.getSource() == Dashboard.dictionaryLookupB) {
            //System.out.println("Disctionary Lookup");
            ArrayList<String> tokens = Tokenizer.getTokens(
                    Dashboard.textInputArea.getText()
            );

            String output = "";
            for (String token : tokens) {
                //output += token + ":";
                output += DictionaryMatcher.getMatchings(token, getAppSoundexCodeLen()) + "\n";
            }
            Dashboard.textOutputArea.setText(output);
            Dashboard.textOutputArea.setCaretPosition(0);
        }

    } // actionPerformed


    void dicToTxt() {
        File dicFile = new File("Resources/dictionaries/EN-winedt/gr.dic");
        File txtFile = new File("Resources/collection/gr.txt");

        try (BufferedReader br = new BufferedReader(new FileReader(dicFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(txtFile))) {

            String line;
            while ((line = br.readLine()) != null) {
                bw.write(line);
                bw.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class GUI {
    static String appName = "SoundexGR Editor v0.2";
    static String exampleInputString =
            "μήνυμα μύνοιμα διάλειμμα διάλιμα αύξων άφξον";
    /*
            "αυγό  αβγό "
            + "θαύμα θάβμα θαυμαστικό "
            + "ξέρω  κσαίρο "
            + "αύξων άφξον "
            +  "εύδοξος εβδοξος "
            + "έτοιμος έτιμος έτημος έτυμος έτιμως αίτημος "
            + "μήνυμα μύνοιμα"
             ;
    */
    static String aboutMsg = "About ... ";
    //FileReadingUtils.readFileAsString("README.txt");

    public static void main(String[] args) {
        System.out.println(appName);
        AppController ac = new AppController(); // controller
        Dashboard d = new Dashboard(ac);  // gui taking the controller
    }
}


class ColorMgr {
    static Color colorBackground = Color.white;
    static Color colorButtonPhone = new Color(204, 255, 255);
    static Color colorButtonMatch = new Color(255, 255, 204);

}
