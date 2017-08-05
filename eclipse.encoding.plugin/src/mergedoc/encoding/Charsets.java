package mergedoc.encoding;

import static mergedoc.encoding.Activator.*;
import static mergedoc.encoding.EncodingPreferenceInitializer.DetectorValue.*;
import static mergedoc.encoding.EncodingPreferenceInitializer.PreferenceKey.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.eclipse.swt.graphics.Image;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

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
		if (a == null || b == null) {
			return false;
		}
		if (a.equalsIgnoreCase(b)) {
			return true;
		}
		return canonicalName(a).equalsIgnoreCase(canonicalName(b));
	}

	private static String canonicalName(String charset) {
		try {
			return Charset.forName(charset).name();
		} catch (Exception e) {
			Activator.info(e.toString(), e);
			return charset;
		}
	}

	/**
	 * Detect the possible charsets of an input stream using juniversalchardet.
	 * @param in The input stream, should close the stream before return.
	 * @return the detected charsets or null.
	 */
	public static String detect(InputStream in) {
		String charset = null;
		if (in != null) {
			InputStream bin = new BufferedInputStream(in);
			try {
				// ICU4J
				if (ICU4J.equals(pref(PREF_DETECTOR))) {
					CharsetDetector detector = new CharsetDetector();
					detector.setText(bin);
					CharsetMatch[] matches = detector.detectAll();
					if (matches != null && matches.length > 0) {
						charset = matches[0].getName();
					}
				}
				// juniversalchardet
				else {
					StrictUniversalDetector detector = new StrictUniversalDetector();
					byte[] buf = new byte[8192];
					int nread;
					while ((nread = in.read(buf)) > 0 && !detector.isDone()) {
						detector.handleData(buf, 0, nread);
					}
					detector.dataEnd();
					charset = detector.getDetectedCharset();
				}
				charset = toMicrosoftName(charset);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				IOUtils.closeQuietly(bin);
			}
		}
		return charset;
	}

	/**
	 * Microsoft default charset mappings.
	 * key  : java.nio canonical name (java.nio.charset.Charset#name lower case)
	 * value: java.io  canonical name (Microsoft Java default encoding name)
	 */
	private static final Map<String, String> msCharsetMap = new HashMap<String, String>() {{
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

	/**
	 * Convert charset to microsoft name.
	 * e.g. Windows-31J => MS932
	 * @return java.io canonical name
	 */
	private static String toMicrosoftName(String charset) {
		if (charset == null) {
			return null;
		}
		String canonicalName = canonicalName(charset);
		String windowsCharset = msCharsetMap.get(canonicalName.toLowerCase());
		if (windowsCharset != null) {
			canonicalName = windowsCharset;
		}
		return canonicalName;
	}

	/**
	 * Convert charset to IANA preferred name.
	 * e.g. BIG5 => Big5
	 * e.g. windows-31j => Windows-31J
	 * @return IANA  preferred name
	 */
	public static String toIANAName(String charset) {
		if (charset == null) {
			return null;
		}
		// convert to java.nio canonical name
		String ianaName = canonicalName(charset);
		if (ianaName.equalsIgnoreCase("windows-31j")) {
			ianaName = "Windows-31J";
		}
		return ianaName;
	}

	public static Image getImage(String charset) {
		if (charset == null) {
			return null;
		}
		// java.nio canonical name lowercase
		String name = canonicalName(charset).toLowerCase();
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

	public static void add(List<String> charsetList, String addition) {
		if (addition != null) {
			String charset = toMicrosoftName(addition);
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
