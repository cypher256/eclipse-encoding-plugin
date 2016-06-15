package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.mozilla.universalchardet.UniversalDetector;

/**
 * Provide encoding related utility functions.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class EncodingUtil {

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
	 * Detect the possible charsets of an input stream using ICU.
	 * @param in The input stream, should close the stream before return.
	 * @return the detected charsets or null.
	 */
	public static String detectEncoding(InputStream in) {
		if (in != null) {
			try {
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

						// to java.lang API canonical name (not java.io API) for Japanese
						if (areCharsetsEqual(encoding, "Shift_JIS") || areCharsetsEqual(encoding, "MS932")) {
							encoding = "MS932";
						}
					}
					return encoding;
				}
				finally {
					bin.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Check whether an input stream can be decoded by an encoding.
	 * @param in The input stream, should close the stream before return.
	 * @return true/false.
	 */
	public static boolean isDecodable(InputStream in, String encoding) {
		if (in != null) {
			try {
				try {
					if (encoding != null) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						byte[] buffer = new byte[4096];
						int len;
						while((len = in.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}

						// Try to decode the input stream using the encoding.
						try {
							Charset.forName(encoding).newDecoder().decode(ByteBuffer.wrap(out.toByteArray()));
							return true;
						} catch (IOException e) {
						}
					}
				}
				finally {
					in.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
}
