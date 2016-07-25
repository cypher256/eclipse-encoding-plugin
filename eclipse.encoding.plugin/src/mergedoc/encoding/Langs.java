package mergedoc.encoding;

import static java.lang.String.*;

import org.apache.commons.lang.StringUtils;

/**
 * @author Shinji Kashihara
 */
public class Langs {

	private Langs() {
	}

	public static String formatLabel(String text, String note) {
		if (StringUtils.isEmpty(note)) {
			return format(text);
		}
		return format(text) + " (" + format(note) + ")";
	}
}
