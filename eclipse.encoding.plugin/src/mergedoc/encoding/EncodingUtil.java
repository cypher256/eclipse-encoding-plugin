package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.eclipse.swt.graphics.Image;
import org.mozilla.universalchardet.UniversalDetector;

/**
 * Provide encoding related utility functions.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class EncodingUtil {

	private EncodingUtil() {
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
				UniversalDetector detector = new UniversalDetector(null);
				byte[] buf = new byte[4096];
				int nread;
				while ((nread = in.read(buf)) > 0 && !detector.isDone()) {
					detector.handleData(buf, 0, nread);
				}
				detector.dataEnd();
				String encoding = detector.getDetectedCharset();
				if (encoding != null) {
					encoding = Charset.forName(encoding).name();

					// to java.lang API canonical name (not java.nio API) for Japanese
					if (areCharsetsEqual(encoding, "Shift_JIS") || areCharsetsEqual(encoding, "MS932")) {
						encoding = "MS932";
					}
				}
				return encoding;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				IOUtil.closeQuietly(bin);
			}
		}
		return null;
	}

	public static Image getImage(String encoding) {
		String name = Charset.forName(encoding).name().toLowerCase();
		if (name.equals("windows-31j") || name.contains("jp") || name.contains("jis")) {
			return Activator.getImage("japan");
		}
		if (name.contains("big5") || name.startsWith("gb") || name.contains("cn") || name.contains("950")) {
			return Activator.getImage("china");
		}
		if (name.endsWith("kr") || name.contains("949")) {
			return Activator.getImage("korea");
		}
		if (name.equals("us-ascii")) {
			return Activator.getImage("us");
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
		try {
			boolean crlf = false;
			boolean cr = false;
			boolean lf = false;
			int count = 0;
			final String MIXED = "Mixed";

			Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
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
			IOUtil.closeQuietly(is);
		}
	}
}
