package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.swt.graphics.Image;

/**
 * Provide charset related utility functions.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class Charsets {

	private Charsets() {
	}

	/**
	 * Check whether two charset strings really mean the same thing.
	 * For UTF-8, acceptable variants are utf-8, utf8.
	 * For Shift_JIS, acceptable variants are shift-jis, Shift-JIS, shift_jis.
	 * @param a The first charset string.
	 * @param b The second charset string.
	 * @return true/false
	 */
	public static boolean equals(String a, String b) {
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
	public static String detect(InputStream in) {
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
				String charset = detector.getDetectedCharset();
				if (charset != null) {
					charset = Charset.forName(charset).name();
					String windowsEncoding = windowsCharsetMap.get(charset.toLowerCase());
					if (windowsEncoding != null) {
						charset = windowsEncoding;
					}
				}
				return charset;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				IOUtils.closeQuietly(bin);
			}
		}
		return null;
	}

	/**
	 * Windows default charset mappings.
	 * key  : java.nio canonical name (java.nio.charset.Charset#name lower case)
	 * value: java.io  canonical name (Windows Java default encoding name)
	 */
	private static final Map<String, String> windowsCharsetMap = new HashMap<String, String>() {{
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

	public static Image getImage(String charset) {
		String name = Charset.forName(charset).name().toLowerCase();
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

	//ktodo delete
	public static byte[] getBOM(InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		}
		int first = inputStream.read();
		if (first == 0xEF) {
			int second = inputStream.read();
			int third = inputStream.read();
			if (second == 0xBB && third == 0xBF)
				return IContentDescription.BOM_UTF_8;
		} else if (first == 0xFE) {
			if (inputStream.read() == 0xFF)
				return IContentDescription.BOM_UTF_16BE;
		} else if (first == 0xFF) {
			if (inputStream.read() == 0xFE)
				return IContentDescription.BOM_UTF_16LE;
		}
		return null;
	}

	public static void add(List<String> charsetList, String charset) {
		if (charset != null) {
			for (String e : charsetList) {
				if (Charsets.equals(e, charset)) {
					return;
				}
			}
			charsetList.add(charset);
			Collections.sort(charsetList);
		}
	}
}
