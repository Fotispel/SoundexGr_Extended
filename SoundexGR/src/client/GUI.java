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
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.*;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import evaluation.BulkCheck;
import evaluation.DictionaryBasedMeasurements;
import evaluation.DictionaryMatcher;
import utils.Tokenizer;


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
            JScrollPane scrollPane = new JScrollPane(demoTextArea);
            scrollPane.setPreferredSize(new Dimension(780, 510));

            // Panel to hold buttons (words)
            JPanel wordButtonsPanel = new JPanel(new FlowLayout());

            // Add both to card panel
            cardPanel.add(scrollPane, "TEXT_AREA");
            cardPanel.add(wordButtonsPanel, "BUTTONS");


            JButton runDemoButton = new JButton("Run Demo");

            JCheckBox editTextCheckBox = new JCheckBox("Edit Text", true);
            editTextCheckBox.addActionListener(e -> demoTextArea.setEditable(editTextCheckBox.isSelected()));

            runDemoButton.addActionListener(e -> {
                String inputText = demoTextArea.getText();
                StringBuilder outputText = new StringBuilder();

                ArrayList<String> tokens = Tokenizer.getTokens(inputText);

                for (String token : tokens) {
                    if (token.length() < 3) {
                        outputText.append(token).append(" ");
                        continue;
                    }

                    String output = DictionaryMatcher.getMatchings(token, Dashboard.getAppSoundexCodeLen(), true) + "\n";
                    System.out.println("FirstMatch: " + DictionaryMatcher.FirstMatch);
                    outputText.append(DictionaryMatcher.FirstMatch).append(" ");
                }

                System.out.println("Demo output: " + outputText);
                demoTextArea.setText(outputText.toString());
            });

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

                                    String res = DictionaryMatcher.getMatchings(token, Dashboard.getAppSoundexCodeLen(), true);
                                    JPanel buttonsMatchingPanel = new JPanel(new FlowLayout());
                                    for (String matching : DictionaryMatcher.rankedWords) {
                                        JButton matchingButton = new JButton(matching);
                                        matchingButton.setBackground(ColorMgr.colorButtonMatch);
                                        matchingButton.setForeground(Color.black);

                                        matchingButton.addActionListener(err -> {
                                            String newWord = matchingButton.getText();
                                            String oldWord = wordButton.getText();

                                            // replaces oldWord in output text with newWord
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
            closeButton.addActionListener(e -> demoFrame.dispose());

            JPanel controlPanel = new JPanel(new FlowLayout());
            controlPanel.add(closeButton);
            controlPanel.add(runDemoButton);
            controlPanel.add(editTextCheckBox);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(cardPanel, BorderLayout.CENTER);
            panel.add(controlPanel, BorderLayout.SOUTH);

            demoFrame.add(panel);
            demoFrame.setVisible(true);
        }

        // Code Length
        if (event.getSource() == Dashboard.codeLenghtsC) {
            int lenBefore = Dashboard.getAppSoundexCodeLen();
            String selected = (String) Dashboard.codeLenghtsC.getSelectedItem();
            int selectedInt = new Integer(selected);
            DictionaryBasedMeasurements.invalidateMap();
            Dashboard.setAppSoundexCodeLen(selectedInt); // TODO here it should make refresh (recompute the codes)
            System.out.printf("Code length changed from %d to %d.", lenBefore, Dashboard.getAppSoundexCodeLen());
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
                output += DictionaryMatcher.getMatchings(token, Dashboard.getAppSoundexCodeLen(), false) + "\n";
            }
            Dashboard.textOutputArea.setText(output);
            Dashboard.textOutputArea.setCaretPosition(0);
        }

    } // actionPerformed
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
