package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;

/**
 * Provide encoding related utility functions.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class Encodings {

	private Encodings() {
	}

	/**
	 * Check whether two charset strings really mean the same thing.
	 * For UTF-8, acceptable variants are utf-8, utf8.
	 * For Shift_JIS, acceptable variants are shift-jis, Shift-JIS, shift_jis.
	 * @param a The first charset string.
	 * @param b The second charset string.
	 * @return true/false
	 */
	public static boolean areCharsetsEqual(String a, String b) {
		if (a == null || b == null) return false;
		try {
			return Charset.forName(a).name().equals(Charset.forName(b).name());
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Detect the possible charsets of an input stream using juniversalchardet.
	 * @param in The input stream, should close the stream before return.
	 * @return the detected charsets or null.
	 */
	public static String detectEncoding(InputStream in) {
		if (in != null) {
			InputStream bin = new BufferedInputStream(in);
			try {
				StrictUniversalDetector detector = new StrictUniversalDetector();
				byte[] buf = new byte[4096];
				int nread;
				while ((nread = in.read(buf)) > 0 && !detector.isDone()) {
					detector.handleData(buf, 0, nread);
				}
				detector.dataEnd();
				String encoding = detector.getDetectedCharset();
				if (encoding != null) {
					encoding = Charset.forName(encoding).name();
					String windowsEncoding = windowsEncodingMap.get(encoding.toLowerCase());
					if (windowsEncoding != null) {
						encoding = windowsEncoding;
					}
				}
				return encoding;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				IOs.closeQuietly(bin);
			}
		}
		return null;
	}

	/**
	 * Windows default encoding mappings.
	 * key  : java.nio canonical name (java.nio.charset.Charset#name lower case)
	 * value: java.io  canonical name (Windows Java default encoding name)
	 */
	private static final Map<String, String> windowsEncodingMap = new HashMap<String, String>() {{
		put("shift_jis", "MS932");
		put("windows-31j", "MS932");
		put("windows-1250", "Cp1250");
		put("windows-1251", "Cp1251");
		put("windows-1252", "Cp1252");
		put("windows-1253", "Cp1253");
		put("windows-1254", "Cp1254");
		put("windows-1255", "Cp1255");
		put("windows-1256", "Cp1256");
		put("windows-1257", "Cp1257");
		put("windows-1258", "Cp1258");
		put("x-windows-874", "MS874");
		put("x-windows-949", "MS949");
		put("x-windows-950", "MS950");
	}};

	public static Image getImage(String encoding) {
		String name = Charset.forName(encoding).name().toLowerCase();
		if (name.equals("windows-31j") || name.contains("jp") || name.contains("jis")) {
			return Activator.getImage("japan");
		}
		if (name.equals("us-ascii")) {
			return Activator.getImage("us");
		}
		if (name.contains("874")) {
			return Activator.getImage("thai");
		}
		if (name.contains("949") || name.endsWith("kr")) {
			return Activator.getImage("korea");
		}
		if (name.contains("950") || name.contains("big5") || name.startsWith("gb") || name.contains("cn")) {
			return Activator.getImage("china");
		}
		if (name.contains("1252") || name.contains("8859")) {
			return Activator.getImage("latin");
		}
		if (name.contains("1253")) {
			return Activator.getImage("greece");
		}
		if (name.contains("1254") || name.contains("857")) {
			return Activator.getImage("turkey");
		}
		if (name.contains("1258")) {
			return Activator.getImage("vietnam");
		}
		if (name.contains("windows")) {
			return Activator.getImage("windows");
		}
		if (name.contains("mac")) {
			return Activator.getImage("mac");
		}
		if (name.contains("ibm")) {
			return Activator.getImage("ibm");
		}
		if (name.contains("utf")) {
			return Activator.getImage("unicode");
		}
		return null;
	}

	public static String getLineEnding(InputStream is, String encoding) {
		if (is == null) {
			return null;
		}
		try {
			Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
			return getLineEnding(reader);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getLineEnding(Reader reader) {
		try {
			boolean crlf = false;
			boolean cr = false;
			boolean lf = false;
			int count = 0;
			final String MIXED = "Mixed";

			int i;
			while((i = reader.read()) != -1) {
				if (++count > 8192) {
					// Parse only starts chars for no line ending big file
					break;
				}
				char c = (char) i;
				if (c == '\r') {
					char nextChar = (char) reader.read();
					if (nextChar == '\n') {
						if (cr || lf) return MIXED;
						crlf = true;
					} else {
						if (crlf || lf) return MIXED;
						cr = true;
					}
				} else if (c == '\n') {
					if (crlf || cr) return MIXED;
					lf = true;
				}
			}
			if (crlf) {
				return "CRLF";
			} else if (cr) {
				return "CR";
			} else if (lf) {
				return "LF";
			}
			return null;

		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			IOs.closeQuietly(reader);
		}
	}
}
