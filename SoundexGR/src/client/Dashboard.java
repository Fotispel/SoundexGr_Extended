package client;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import evaluation.BulkCheck;
import evaluation.DictionaryBasedMeasurements;
import evaluation.DictionaryMatcher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

import static evaluation.BulkCheck.*;

public class Dashboard extends JFrame {
    AppController apCtlr;

    static JTextArea textInputArea;
    static JTextArea textOutputArea;
    static JTextArea consoleOutputArea;

    static Dimension textAreaDimension = new Dimension(50, 10);

    static JButton soundexB;
    static JButton soundexNaiveB;
    static JButton pnoneticB;
    static JButton applyAllB;
    static JButton dictionaryLookupB;
    static JButton produceErrosB;

    static JComboBox codeLenghtsC;


    static JButton clearB;
    static JButton swapB;

    static Font appTextfont = new Font("monospaced", Font.BOLD, 18);  //  Font.PLAIN Font.BOLD
    static Font consoleTextfont = new Font("monospaced", Font.PLAIN, 12);  //  Font.PLAIN Font.BOLD
    static Font appButtonfont = new Font("serif", Font.PLAIN, 18);

    public static int appSoundexCodeLen = 6;

    private static String selectedDatasetFile = null;

    private static String selectedMethod = null;

    /**
     * @return the appSoundexCodeLen
     */
    static int getAppSoundexCodeLen() {
        return appSoundexCodeLen;
    }

    /**
     * @param newLen the appSoundexCodeLen to set
     */
    public static void setAppSoundexCodeLen(int newLen) {
        Dashboard.appSoundexCodeLen = newLen;
        SoundexGRExtra.LengthEncoding = newLen;
        SoundexGRSimple.LengthEncoding = newLen;
        loadOrRefreshDictionary();
    }

    static void loadOrRefreshDictionary() {
        for (String docName : DocNames) {
            datasetFileWords.add(String.format("Resources//collection_words//%s_words.txt", docName));  // Add each path to the list
        }
        DatasetFiles = new String[DocNames.size()];
        for (int i = 0; i < DocNames.size(); i++) {
            DatasetFiles[i] = "Resources//collection//" + DocNames.get(i) + ".txt";
            System.out.println(DatasetFiles[i]);
        }

        setSelectedDatasetFile(DocNames.get(0));

        String DefaultMethod = "";
        setSelectedMethod(DefaultMethod);

        //BulkCheck.execute_selected_method();

        /*
        //Loading the dictionary (one getMatching initiates its loading)
        new Thread(() -> {
            DictionaryMatcher.getMatchings("αυγόβββ", Dashboard.getAppSoundexCodeLen());
            System.out.println("Dictionary loaded/refreshed.");
        }).start();
         */
    }

    public static String getSelectedDatasetFile() {
        return selectedDatasetFile;
    }

    public static void setSelectedDatasetFile(String selectedDatasetFile) {
        Dashboard.selectedDatasetFile = selectedDatasetFile;
    }

    public static String getSelectedMethod() {
        return selectedMethod;
    }

    public static void setSelectedMethod(String selectedMethod) {
        Dashboard.selectedMethod = selectedMethod;
    }

    public static int getNumberOfWords_of_SelectedDatasetFile() {
        //calculate the number of words of the selected dataset file
        File file = new File("Resources//collection_words//" + getSelectedDatasetFile() + "_words.txt");
        int count = 0;
        try {
            java.util.Scanner sc = new java.util.Scanner(file);
            while (sc.hasNext()) {
                sc.next();
                count++;
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }


    public static int getNumberOfWords_of_DatasetFile(String docName) {
        //calculate the number of words of the selected dataset file
        File file = new File("Resources//collection_words//" + docName + "_words.txt");
        int count = 0;
        try {
            java.util.Scanner sc = new java.util.Scanner(file);
            while (sc.hasNext()) {
                sc.next();
                count++;
            }
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Constructor. Takes an input the object that will act as actionlistener
     *
     * @param ac
     */
    Dashboard(AppController ac) {
        apCtlr = ac; // controller
        setAppSoundexCodeLen(Dashboard.getAppSoundexCodeLen());   // it also loads/refreshes the dictionary
        //loadOrRefreshDictionary(); // not needed


        // ICON
        try {
            ImageIcon icon;
            String path = "/images/icon.png";  // works "/icon.png"
            //System.out.println(getClass().getResource(path)); // "/"
            URL imgURL = getClass().getResource(path);
            if (imgURL != null) {
                //System.out.println(">>>"+imgURL);
                icon = new ImageIcon(imgURL, "Eikonidio");
                this.setIconImage(icon.getImage());
            } else {
                System.err.println("Couldn't find the icon file: " + path);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        //Create and set up the Menu
        //AppMenu appMenu = new AppMenu(this);
        //this.setJMenuBar(appMenu.createMenuBar());

        // GUI PART: size, layout, title
        setBounds(80, 80, 1200, 800);  //x,  y,  width,  height)
        setLayout(new GridLayout(0, 2, 5, 5)); // rows, columns, int hgap, int vgap)
        //setLayout(new FlowLayout()); // rows, columns, int hgap, int vgap)

        //this.getContentPane().setBackground(ColorMgr.colorBackground);
        //this.getContentPane().setBackground(Color.BLUE);
        setVisible(true);
        setTitle(GUI.appName);

        // GUI PART:  calling the methods that create buttons each in one separate panel

        //PANEL FOR ALL USER INPUT (TEXT AREA AND "TOOLBARS")
        JPanel generalInputPanel = new JPanel(new GridLayout(0, 1, 5, 1)); // rows, columns, int hgap, int vgap)
        generalInputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "User Input & Tool Bars"));
        add(generalInputPanel);

        createInput(generalInputPanel);
        createDatasetFilesSelection(generalInputPanel);
        createMethodSelection(generalInputPanel);
        createPhonemicOperators(generalInputPanel);
        createMatchingOperators(generalInputPanel);
        createGeneralOperators(generalInputPanel);
        createConsoleOutput(generalInputPanel);
        redirectSystemStreams();
        createOutput(null);

        createMenu();

        setVisible(true);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }

    /*
     * Menu bar
     */

    void createMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutOption = new JMenuItem("About");
        String URLofGitHub = "https://github.com/YannisTzitzikas/SoundexGR";

        // layout
        menuBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

        // behaviour
        aboutOption.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Dashboard.textOutputArea.setText("See " + URLofGitHub);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(URLofGitHub));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

            }
        });

        //adding the menus
        helpMenu.add(aboutOption);
        menuBar.add(helpMenu);
        add(menuBar);

        setJMenuBar(menuBar);
        menuBar.setVisible(true);
        menuBar.setEnabled(true);
    }


    /**
     * Create gui elements for the input
     */
    void createInput(JPanel parentPanel) {
        JPanel userInputPanel = new JPanel(new GridLayout(1, 1, 5, 1)); // rows, columns, int hgap, int vgap)
        userInputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Input"));
        // A: INPUT TEXT AREA
        //JTextArea
        textInputArea = new JTextArea(
                //"Welcome! Write whatever you want here."
                GUI.exampleInputString
        );
        //textInputArea.setFont(new Font("Courier", NORMAL, 22));  // Font.ITALIC    Courier Serif
        textInputArea.setFont(appTextfont);

        textInputArea.setLineWrap(true);
        textInputArea.setWrapStyleWord(true);

        JScrollPane areaScrollPane = new JScrollPane(textInputArea);
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(textAreaDimension);

        userInputPanel.add(areaScrollPane);

        if (parentPanel == null) { // if no parent panel
            this.add(userInputPanel); // adds to Frame
        } else {
            parentPanel.add(userInputPanel);
        }
    }


    /**
     * Create gui elements for the output
     */
    void createOutput(JPanel parentPanel) {
        JPanel outputPanel = new JPanel(new GridLayout(1, 1, 5, 5)); // rows, columns, int hgap, int vgap)
        outputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Output"));

        // A: OUTPUT TEXT AREA
        //JTextArea
        textOutputArea = new JTextArea(
                "output"
        );
        //textOutputArea.setFont(new Font("Courier", NORMAL, 22));  //
        textOutputArea.setFont(appTextfont);
        textOutputArea.setLineWrap(true);
        textOutputArea.setWrapStyleWord(true);


        JScrollPane areaScrollPane = new JScrollPane(textOutputArea);
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(textAreaDimension);

        outputPanel.add(areaScrollPane);
        //add(outputPanel); // adds to Frame
        if (parentPanel == null) { // if no parent panel
            this.add(outputPanel); // adds to Frame
        } else {
            parentPanel.add(outputPanel);
        }

    } // create output


    void createConsoleOutput(JPanel parentPanel) {
        JPanel consolePanel = new JPanel(new GridLayout(1, 1, 5, 5)); // rows, columns, int hgap, int vgap)
        consolePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Console output"));

        // A: OUTPUT TEXT AREA
        //JTextArea
        consoleOutputArea = new JTextArea(
                "Console output\n"
        );
        //textOutputArea.setFont(new Font("Courier", NORMAL, 22));  //
        consoleOutputArea.setFont(consoleTextfont);
        consoleOutputArea.setLineWrap(true);
        consoleOutputArea.setWrapStyleWord(true);
        consoleOutputArea.setEditable(false);


        JScrollPane areaScrollPane = new JScrollPane(consoleOutputArea);
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setPreferredSize(textAreaDimension);

        consolePanel.add(areaScrollPane);
        //add(outputPanel); // adds to Frame
        if (parentPanel == null) { // if no parent panel
            this.add(consolePanel); // adds to Frame
        } else {
            parentPanel.add(consolePanel);
        }

    } // create output


    /**
     * Creates the buttons for the general operators
     */

    void createGeneralOperators(JPanel parentPanel) {
        //JPanel generalOperatorPanel = new JPanel(new GridLayout(1,0,5,5)); // rows, columns, int hgap, int vgap)
        JPanel generalOperatorPanel = new JPanel(new FlowLayout()); // rows, columns, int hgap, int vgap)
        generalOperatorPanel.setBackground(ColorMgr.colorBackground);

        generalOperatorPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "General Operations"));


        clearB = new JButton("Clear"); //clear
        swapB = new JButton("Set Output as Input"); //clear

        clearB.setBackground(Color.LIGHT_GRAY);
        swapB.setBackground(Color.LIGHT_GRAY);

        // Array with all buttons
        JButton[] allButtons = {
                clearB,
                swapB
        };

        // tooltips
        clearB.setToolTipText("Clears the input area");
        swapB.setToolTipText("Set Output as Input");

        // Add Buttons to Panel and sets action listeners
        for (JButton b : allButtons) {
            generalOperatorPanel.add(b); // adds button to the panel
            b.setFont(appButtonfont);
            b.addActionListener(this.apCtlr); // sets the action listener
            b.setBackground(Color.white);
        }
        // adds the panel to Frame

        //operatorPanel.setSize(200, 100); // lala
        //add(generalOperatorPanel);


        if (parentPanel == null) { // if no parent panel
            this.add(generalOperatorPanel); // adds to Frame
        } else {
            parentPanel.add(generalOperatorPanel);
        }

    }


    void createMatchingOperators(JPanel parentPanel) {
        //JPanel matchingOperatorPanel = new JPanel(new GridLayout(1,0,5,5)); // rows, columns, int hgap, int vgap)
        JPanel matchingOperatorPanel = new JPanel(new FlowLayout());
        //JPanel matchingOperatorPanel = new FlowLayout();
        matchingOperatorPanel.setBackground(ColorMgr.colorBackground);

        matchingOperatorPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Misspelling and Matching Operations"));

        dictionaryLookupB = new JButton("Dictionary lookup");
        produceErrosB = new JButton("Produce Misspellings");

        // Array with all buttons
        JButton[] allButtons = {
                produceErrosB,
                dictionaryLookupB,
        };


        //tooltips
        dictionaryLookupB.setToolTipText("Lookup the word(s) in the dictionary (it it does not exist it returns approximate matches)");
        produceErrosB.setToolTipText("Produces various misspellings of the given words");


        // Add Buttons to Panel and sets action listeners
        for (JButton b : allButtons) {
            matchingOperatorPanel.add(b); // adds button to the panel
            b.addActionListener(this.apCtlr); // sets the action listener
            b.setFont(appButtonfont);
            b.setBackground(ColorMgr.colorButtonMatch);
        }
        // adds the panel to Frame

        //operatorPanel.setSize(200, 100); // lala
        //add(matchingOperatorPanel);

        if (parentPanel == null) { // if no parent panel
            this.add(matchingOperatorPanel); // adds to Frame
        } else {
            parentPanel.add(matchingOperatorPanel);
        }
    }

    /**
     * Creates the combo box for selecting dataset files.
     */
    void createDatasetFilesSelection(JPanel parentPanel) {
        // Panel for dataset files selection
        JPanel datasetPanel = new JPanel(new FlowLayout()); // rows, columns, int hgap, int vgap)
        datasetPanel.setBackground(ColorMgr.colorBackground);

        datasetPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Select Dataset Files"));

        // Label for the combo box
        JLabel datasetLabel = new JLabel("Dataset:");

        // JComboBox for selecting dataset files
        String[] docNames = new String[DatasetFiles.length];
        for (int i = 0; i < DatasetFiles.length; i++) {
            int index = DatasetFiles[i].lastIndexOf("/");
            String name = DatasetFiles[i].substring(index + 1);

            index = name.lastIndexOf(".");
            name = name.substring(0, index);
            docNames[i] = name;
        }

        JComboBox<String> datasetComboBox = new JComboBox<>(docNames);

        // Add ActionListener to the combo box
        datasetComboBox.addActionListener(e -> {
            selectedDatasetFile = (String) datasetComboBox.getSelectedItem();

            if (!Objects.equals(getSelectedMethod(), "")) {
                SoundexGRExtra.LengthEncoding = DictionaryBasedMeasurements.calculateSuggestedCodeLen();
                appSoundexCodeLen = SoundexGRExtra.LengthEncoding;
                System.out.println("Optimal length " + appSoundexCodeLen + " for dataset " + selectedDatasetFile);
            } else {
                System.out.println("No method selected yet.");
            }
        });

        // Set font for combo box and label
        datasetLabel.setFont(appButtonfont);
        datasetComboBox.setFont(appButtonfont);

        // Add components to the panel
        datasetPanel.add(datasetLabel);
        datasetPanel.add(datasetComboBox);

        // Add the panel to the parent panel or frame
        if (parentPanel == null) {
            this.add(datasetPanel); // adds to Frame
        } else {
            parentPanel.add(datasetPanel);
        }
    }

    void createMethodSelection(JPanel parentPanel) {
        // Panel for dataset files selection
        JPanel methodPanel = new JPanel(new FlowLayout()); // rows, columns, int hgap, int vgap)
        methodPanel.setBackground(ColorMgr.colorBackground);

        methodPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Select Method"));

        // Label for the combo box
        JLabel methodLabel = new JLabel("Method:");

        // JComboBox for selecting dataset files
        String[] methods = {"","Real-time length calculation", "Predefined length", "Hybrid method"};

        JComboBox<String> methodComboBox = new JComboBox<>(methods);

        // Add ActionListener to the combo box
        methodComboBox.addActionListener(e -> {
            selectedMethod = (String) methodComboBox.getSelectedItem();

            if (Objects.equals(selectedMethod, "Real-time length calculation") && BulkCheck.length_per_DocName.isEmpty()) {  //1st method
                BulkCheck.execute_selected_method();
            } else if (Objects.equals(selectedMethod, "Predefined length") && BulkCheck.length_per_DocName.isEmpty()) {  //2nd method
                BulkCheck.execute_selected_method();
            } else if (Objects.equals(selectedMethod, "Hybrid method") && BulkCheck.length_per_DocName.isEmpty()) {  //3rd method
                BulkCheck.execute_selected_method();
            }

            assert selectedMethod != null;
            switch (selectedMethod) {
                case "Real-time length calculation":
                    SoundexGRExtra.LengthEncoding = length_per_DocName.get(selectedDatasetFile);
                    appSoundexCodeLen = SoundexGRExtra.LengthEncoding;
                    break;
                case "Predefined length":

                    break;
                case "Hybrid method":
                    SoundexGRExtra.LengthEncoding = 6;  // to be changed
                    appSoundexCodeLen = SoundexGRExtra.LengthEncoding;
                    break;
            }
        });

        // Set font for combo box and label
        methodLabel.setFont(appButtonfont);
        methodComboBox.setFont(appButtonfont);

        // Add components to the panel
        methodPanel.add(methodLabel);
        methodPanel.add(methodComboBox);

        // Add the panel to the parent panel or frame
        if (parentPanel == null) {
            this.add(methodPanel); // adds to Frame
        } else {
            parentPanel.add(methodPanel);
        }
    }


    /**
     * Creates the buttons for the phonemic operators
     */
    void createPhonemicOperators(JPanel parentPanel) {
        //JPanel operatorPanel = new JPanel(new GridLayout(1,0,5,5)); // rows, columns, int hgap, int vgap)
        JPanel operatorPanel = new JPanel(new FlowLayout()); // rows, columns, int hgap, int vgap)
        operatorPanel.setBackground(ColorMgr.colorBackground);

        operatorPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Phonemic Operations"));


        soundexB = new JButton("SoundexGR"); //soundex
        soundexNaiveB = new JButton("SoundexGRNaive"); //soundexNaive
        pnoneticB = new JButton("Phonemic"); //phonetic
        applyAllB = new JButton("APPLY ALL"); //apply all


        // Array with all buttons
        JButton[] allButtons = {
                soundexB,
                soundexNaiveB,
                pnoneticB,
                applyAllB,
        };

        // tooltips
        soundexB.setToolTipText("Applies SoundexGR to each input word");
        soundexNaiveB.setToolTipText("Applies SoundexGRNaive to each input word");
        pnoneticB.setToolTipText("Applies Phonetic Transcription to each input word");
        applyAllB.setToolTipText("Applies all to each input word");

        // Add Buttons to Panel and sets action listeners
        for (JButton b : allButtons) {
            operatorPanel.add(b); // adds button to the panel
            b.addActionListener(this.apCtlr); // sets the action listener
            b.setFont(appButtonfont);
            b.setBackground(ColorMgr.colorButtonPhone);
        }
        // adds the panel to Frame

        //operatorPanel.setSize(200, 100); // lala
        //add(operatorPanel);

        if (parentPanel == null) { // if no parent panel
            this.add(operatorPanel); // adds to Frame
        } else {
            parentPanel.add(operatorPanel);
        }
    }


    /**
     * @param text
     */
    private void updateConsoleTextArea(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                consoleOutputArea.append(text);

            }
        });
    }


    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                updateConsoleTextArea(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateConsoleTextArea(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }
}
