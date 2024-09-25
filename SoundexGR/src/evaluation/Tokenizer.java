package evaluation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A few simple methods for tokenization (words and phrases)
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 *
 */

public class Tokenizer {
	static String delimiter = "\t\n\r\f ";
	static String lineDelimiters = ".:!?;";


	/**
	 * Removes punctuation (. and ,)
	 *
	 * @param s
	 * @return
	 */
	static String removePunctuation(String s) {
		s = s.replace(".", "");
		s = s.replace(",", "");
		//s.replaceAll("\\p{Punct}",""); // removes punctuation
		//String sOut = Normalizer.normalize(s, Normalizer.Form.NFD); // this will separate all of the accent marks from the characters.
		//sOut = sOut.replaceAll("\\p{M}", ""); // exlcudes accents for all unicode
		return s;
	}


	/**
	 * It returns an ArrayList with the tokens (words) of a string
	 * First removes punctuation,
	 * then it splits the string using delimiters
	 *
	 **/
	static public  ArrayList<String> getTokens(String s) {
		ArrayList<String>  tokens = new ArrayList<>();
		s = removePunctuation(s);
		//System.out.println(s);
		StringTokenizer tokenizer = new StringTokenizer(s, delimiter+lineDelimiters);
		while(tokenizer.hasMoreTokens() ) {
			String currentToken = tokenizer.nextToken();
			tokens.add(currentToken.toLowerCase());
		}
		return tokens;
	}

	/**
	 * It prints in the console the tokens and returns
	 * the input string annotated.
	 * @param s
	 * @return
	 */
	static public  String tokenize(String s) {


		s = removePunctuation(s);
		String output ="";
		StringTokenizer tokenizer = new StringTokenizer(s, delimiter+lineDelimiters);
		while(tokenizer.hasMoreTokens() ) {
			String currentToken = tokenizer.nextToken();
			//System.out.println(currentToken);
			output= output + "<word>"+currentToken+ "</word> ";
		}
		return output;
	}



	/**
	 * It returns an ArrayList with the lines  of a string (textual paragraph
	 *
	 **/
	static public  ArrayList<String> getLines(String s) {

		ArrayList<String>  lines = new ArrayList<>();
		StringTokenizer tokenizer = new StringTokenizer(s, lineDelimiters);
		while(tokenizer.hasMoreTokens() ) {
			String currentLine = tokenizer.nextToken();
			lines.add(currentLine);
		}
		return lines;
	}

	public static void main(String[] lala) {

		System.out.println(Tokenizer.getLines("\ngrami 1 \n gram\nmi 2 ;lk;kl:\n"));

	}
}



/**
 * A class for demonstrating the previous methods
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 *
 */
class TokenizerDemo {

	public static void main(String[] lala) {
		System.out.println("**Tokenizer**");

		 String s = "Who are you? Hello world. I am a simple tokenizer. "
		 		+ "My name is Tokenizer22 and I am 22 minutes old. Have fun!!"
		 		+ "My friends are: Maria and Nikos. Μιλάω και Ελληνικά επίσης!";

		 System.out.println("\nOriginal Text:\n" + s );


		 System.out.println("\nWith Separations:\n" + Tokenizer.tokenize(s) );


		 System.out.println("\nLines:\n"+ Tokenizer.getLines(s));

	}

}