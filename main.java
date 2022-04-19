import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Word Occurrences program
 * <p>
 * This program reads the poem The Raven from a webpage and removes symbols/numbers.
 * The program totals the occurrences of each word in the poem then saves the results to a HashMap.
 * The program then inserts the HashMap data into a local database.
 * A simple UI is provided to search the database for words.
 * 
 * @author Sharlton Shepeluk
 *
 */



public class main {

	/**
	 * The main method
	 * @param args
	 * @throws Exception
	 */
	
	public static void main(String[] args) throws Exception {

		// Reading URL and writing to file ==================================================================================

		URL url;
		try {
			url = new URL("https://www.gutenberg.org/files/1065/1065-h/1065-h.htm"); // Create object for URL.
			BufferedReader readr = new BufferedReader(new InputStreamReader(url.openStream())); // Open a reader to
																								// stream the file
			BufferedWriter writer = new BufferedWriter(new FileWriter("poem.txt")); // Create file to store the
																					// downloaded info.
			String line;

			int start = 77; // line right before poem starts. Seen by viewing webpage source code
			int end = 244; // line right after poem starts. Seen by viewing webpage source code

			// Writes poem to file starting and ending at the predetermined lines above
			for (int ln = 0; (line = readr.readLine()) != null && ln <= end; ln++) {
				if (ln >= start) {
					writer.write(line);
				}
			}

			readr.close();
			writer.close();
		} catch (IOException e) {
			System.out.println("Error");
			e.printStackTrace();
		}

		File f = new File("poem.txt");

		try {

			Document doc = Jsoup.parse(f, "UTF-8"); // Eliminates html tags and symbols

			String body = doc.body().text(); // Save the parsed words to a string.
			BufferedWriter writer = new BufferedWriter(new FileWriter("poem.txt")); // Rewrite the file without the
																					// tags/symbols.
			writer.write(body);
			writer.close();
		} catch (IOException e) {
			System.out.println("Error");
			e.printStackTrace();
		}

		// HashMap and Word Count ===========================================================================================================

		Path path = Paths.get("C:\\Users\\sharl\\Desktop\\2022HW\\SoftwareDev\\Mod10Database\\poem.txt");

		try {

			// Converting file from ANSI to UTF-8
			ByteBuffer bb = ByteBuffer.wrap(Files.readAllBytes(path));
			CharBuffer cb = Charset.forName("windows-1252").decode(bb);
			bb = Charset.forName("UTF-8").encode(cb);
			Files.write(path, bb.array());

		} catch (IOException e1) {
			System.out.println("Error");
			e1.printStackTrace();
		}

		// HashMap
		LinkedHashMap<String, Integer> sortedFreq = new LinkedHashMap<>();

		try {

			String poem = Files.readString(path);

			poem = poem.toLowerCase();
			Pattern p = Pattern.compile("[a-z]+"); // creates pattern that ignores symbols
			Matcher m = p.matcher(poem);

			TreeMap<String, Integer> freq = new TreeMap<>();

			// Count occurrences
			while (m.find()) {
				String word = m.group();

				if (freq.containsKey(word)) {
					freq.computeIfPresent(word, (w, c) -> Integer.valueOf(c.intValue() + 1));
				} else {
					freq.computeIfAbsent(word, (w) -> Integer.valueOf(1));
				}
			}

			// Sorts treemap into descending order with use of a linked hashmap

			freq.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
					.forEachOrdered(x -> sortedFreq.put(x.getKey(), x.getValue()));

			// Populates database with HashMap data
			popDatabase(sortedFreq);

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println();

		// GUI ============================================================================================================

		JFrame frame = new JFrame();
		JPanel panel = new JPanel();
		frame.setSize(350, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(panel);

		panel.setLayout(null);
		JLabel label1 = new JLabel("Search for word:");
		label1.setFont(new Font("Serif", Font.PLAIN, 12));
		label1.setBounds(10, 20, 80, 25);
		panel.add(label1);

		JLabel label2 = new JLabel();
		label2.setBounds(10, 100, 180, 25);
		panel.add(label2);

		JTextField searchWord = new JTextField("Enter word", 20);

		searchWord.setBounds(100, 20, 165, 25);
		panel.add(searchWord);

		JButton button = new JButton("Search");
		button.setBounds(10, 80, 80, 25);
		button.addActionListener(new ActionListener() {

			// Button searches for inputed word and outputs # of occurrences
			public void actionPerformed(java.awt.event.ActionEvent e) {

				String userEntry = searchWord.getText().toLowerCase();

				try {
					Connection conn = getConnection();
					PreparedStatement stmtSearch = conn.prepareStatement("SELECT * FROM word WHERE Word= ?");
					stmtSearch.setString(1, userEntry);
					ResultSet result = stmtSearch.executeQuery();

					if (result.next()) {
						do {
							label2.setText(result.getString(1) + ", " + result.getString(2));
						} while (result.next());
					} else
						label2.setText("Word not found in database.");

				} catch (Exception d) {
					System.out.println("Searching error.");
				}

			}

		});

		panel.add(button);

		frame.setVisible(true);
		
		
	}// end main

	 
	
	/**
	 * Method to establish connection to database
	 * @return conn
	 * @throws Exception
	 */
	public static Connection getConnection() throws Exception {
		try {
			
			String driver = "com.mysql.cj.jdbc.Driver";
			String url = "jdbc:mysql://localhost:3306/wordoccurrences";
			String username = "root";
			String password = "dbtotaltime";
			Class.forName(driver);

			Connection conn = DriverManager.getConnection(url, username, password);

			return conn;
		} catch (Exception e) {
			System.out.println("Error");

		}

		return null;
	}

	
	/**
	 * Method to populate database with HashMap sortedFreq data
	 * Outputs data by # of Occurrences in descending order
	 * @param sortedFreq
	 * @throws Exception
	 */
	public static void popDatabase(LinkedHashMap<String, Integer> sortedFreq) throws Exception {
		Connection conn = getConnection();

		String sql = "INSERT INTO word (Word, Occurrences) VALUES (?, ?)";
		PreparedStatement stmtInsert = conn.prepareStatement(sql);

		for (String word : sortedFreq.keySet()) {
			String key = word.toString();
			int value = sortedFreq.get(word);
			stmtInsert.setString(1, key);
			stmtInsert.setInt(2, value);
			stmtInsert.executeUpdate();
		}

		PreparedStatement stmtSelect = conn.prepareStatement("SELECT * FROM word ORDER BY Occurrences DESC");
		ResultSet result = stmtSelect.executeQuery();

		while (result.next()) {

			System.out.println(result.getString(1) + ", " + result.getString(2));

		}

	}
}
