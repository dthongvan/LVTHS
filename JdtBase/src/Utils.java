

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Bo thu vien cac tinh nang
 */
public class Utils {
	/**
	 * Doc noi dung file java hoac xhtml, xml, jsp
	 * 
	 * @param filePath
	 *            duong dan tuyet doi file
	 * @return noi dung file
	 */
	public static String readFileContent(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(3000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

}
