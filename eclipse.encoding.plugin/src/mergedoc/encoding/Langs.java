package mergedoc.encoding;

import static java.lang.String.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @author Shinji Kashihara
 */
public class Langs {

	private Langs() {
	}

	public static String formatLabel(String text, List<String> noteList) {
		return formatLabel(text, noteList.toArray(new String[noteList.size()]));
	}

	public static String formatLabel(String text, String... noteArray) {
		ArrayList<String> noteList = new ArrayList<String>();
		for (String n : noteArray) {
			if (n != null) {
				noteList.add(format(n));
				if (noteList.size() == 3) { // Size limit for display width
					break;
				}
			}
		}
		String note = StringUtils.join(noteList, ", ");
		if (StringUtils.isEmpty(note)) {
			return format(text);
		}
		return format(text) + " (" + note + ")";
	}
}
