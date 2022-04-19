import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * A simple JUNit test to verify that "poem.txt" was successfully created
 * 
 * @author Sharlton Shepeluk
 *
 */

class fileTest {

	@Test
	void fileTest() {
		main test = new main();
		File testFile = new File("C:\\Users\\sharl\\Desktop\\2022HW\\SoftwareDev\\Mod10Database\\poem.txt");
		assertTrue(testFile.exists());
	}

}
