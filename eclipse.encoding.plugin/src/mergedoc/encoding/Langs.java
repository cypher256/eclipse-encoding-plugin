package mergedoc.encoding;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.lang.StringUtils;

/**
 * @author Shinji Kashihara
 */
public class Langs {

	private Langs() {
	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public static String parentheses(String encoding) {
		return StringUtils.isEmpty(encoding) ? "" : " (" + encoding + ")";
	}
}
