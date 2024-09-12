/**
 * 
 */
package client;

/**
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 *
 */


import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import SoundexGR.SoundexGRExtra;
import SoundexGR.SoundexGRSimple;
import evaluation.DictionaryBasedMeasurements;
import evaluation.DictionaryMatcher;
import utils.Tokenizer;



/**
 * AppController: The controller of the graphical add
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 *
 */

class AppController implements ActionListener {

	/**
	 * The method where all GUI actions are sent
	 */
	@Override
	public void actionPerformed(ActionEvent event) {
		// Clear BUTTON
		if (event.getSource()==Dashboard.clearB ) {
			//System.out.println("Clear Pressed");
			if (Dashboard.textInputArea.getText().equals(""))
			   Dashboard.textInputArea.setText(GUI.exampleInputString);
			else 
				 Dashboard.textInputArea.setText("");
		}
		
		// Set Output as Input Clear BUTTON
		if (event.getSource()==Dashboard.swapB ) {
			Dashboard.textInputArea.setText(Dashboard.textOutputArea.getText());
		}
				
		// Code Length
		if (event.getSource()==Dashboard.codeLenghtsC ) {
			int lenBefore =  Dashboard.getAppSoundexCodeLen();
			String selected = (String)Dashboard.codeLenghtsC.getSelectedItem();
			int selectedInt= new Integer(selected);
			DictionaryBasedMeasurements.invalidateMap();
			Dashboard.setAppSoundexCodeLen(selectedInt); // TODO here it should make refresh (recompute the codes)
			System.out.printf("Code length changed from %d to %d.", lenBefore,Dashboard.getAppSoundexCodeLen());
		}
		
		// SOUNDEX EXTRA
		if (event.getSource()==Dashboard.soundexB ) {
			//System.out.println("SoundexGR Pressed");
			ArrayList<String> tokens = 	Tokenizer.getTokens(
							Dashboard.textInputArea.getText()
			);
			String outputStr="";
			for (String token: tokens) {	
				
				outputStr = outputStr.concat(" " + 
						SoundexGRExtra.encode(token)
						)  ;
			}
			Dashboard.textOutputArea.setText(outputStr);
			Dashboard.textOutputArea.setCaretPosition(0);
		}
		
		
		// SOUNDEX Naive
		if (event.getSource()==Dashboard.soundexNaiveB) {
			//System.out.println("SoundexGR Naive Pressed");
			ArrayList<String> tokens = 	Tokenizer.getTokens(
							Dashboard.textInputArea.getText()
			);
			String outputStr="";
			for (String token: tokens) {	
				
				outputStr = outputStr.concat(" " + 
						SoundexGRSimple.encode(token)
						)  ;
			}
			Dashboard.textOutputArea.setText(outputStr);
			Dashboard.textOutputArea.setCaretPosition(0);
		}
		
		// Phonetic
		if (event.getSource()==Dashboard.pnoneticB) {
			//System.out.println("Phonetic Transcription  Pressed");
			ArrayList<String> tokens = 	Tokenizer.getTokens(
							Dashboard.textInputArea.getText()
			);
			String outputStr="";
			for (String token: tokens) {	
				
				outputStr = outputStr.concat(" " + 
						SoundexGRExtra.phoneticTrascription(token)
						)  ;
			}
			Dashboard.textOutputArea.setText(outputStr);
			Dashboard.textOutputArea.setCaretPosition(0);
		}
		
		//applyAllB
		if (event.getSource()==Dashboard.applyAllB) {
			//System.out.println("Apply all pressed");
			ArrayList<String> tokens = 	Tokenizer.getTokens(
							Dashboard.textInputArea.getText()
			);
			String strFormat = "%14s ->  %s   %s  %s";
			
			String outputStr= String.format(strFormat, "word", "SGR", "SGRNv", "Phonetic");				
			for (String token: tokens) {	
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
		if (event.getSource()==Dashboard.produceErrosB) {
			//System.out.println("Misspellings");
			ArrayList<String> tokens = 	Tokenizer.getTokens(
							Dashboard.textInputArea.getText()
			);
			
			String output="";
			for (String token: tokens) {	
				output += token + ":";
				for (String errorStr: DictionaryBasedMeasurements.returnVariations(token)) {
					output+= " " + errorStr;
					//System.out.println(output);
				}
				output+="\n";
			}
			Dashboard.textOutputArea.setText(output);
			Dashboard.textOutputArea.setCaretPosition(0);
		}
			
		// dictionarylookup
		if (event.getSource()==Dashboard.dictionaryLookupB) {
			//System.out.println("Disctionary Lookup");
			ArrayList<String> tokens = 	Tokenizer.getTokens(
							Dashboard.textInputArea.getText()
			);
			String output="";
			for (String token: tokens) {	
				//output += token + ":";
				output+=DictionaryMatcher.getMatchings(token, Dashboard.getAppSoundexCodeLen()) + "\n";
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
	static Color  colorBackground = Color.white;
	static Color  colorButtonPhone = new Color(204,255,255);
	static Color  colorButtonMatch = new Color(255,255,204);
	
}
