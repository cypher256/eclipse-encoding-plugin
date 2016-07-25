package mergedoc.encoding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Provide line separetor related utility functions.
 * @author Shinji Kashihara
 */
public class LineSeparators {

	private LineSeparators() {
	}

	/**
	 * @param is The input stream will be closed by this operation.
	 * @param encoding
	 * @return Line separator string
	 */
	public static String ofContent(InputStream is, String encoding) {
		if (is == null) {
			return null;
		}
		try {
			Reader reader = new BufferedReader(new InputStreamReader(is, encoding));
			return ofContent(reader);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param reader The reader will be closed by this operation.
	 * @return Line separator string
	 */
	public static String ofContent(Reader reader) {
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
			}
			if (cr) {
				return "CR";
			}
			if (lf) {
				return "LF";
			}
			return null;

		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	public static String resolve(IResource resource) {
		String lineSeparator = null;
		if (resource != null) {
			lineSeparator = ofProject(resource);
		}
		if (lineSeparator == null) {
			lineSeparator = ofWorkspace();
		}
		return lineSeparator;
	}

	public static String ofProject(IResource resource) {
		IPreferencesService prefs = Platform.getPreferencesService();
		IScopeContext[] scopeContext = new IScopeContext[] { new ProjectScope(resource.getProject()) };
		String lineSeparator = prefs.getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
		return toLabel(lineSeparator);
	}

	public static String ofWorkspace() {
		IPreferencesService prefs = Platform.getPreferencesService();
		IScopeContext[] scopeContext = new IScopeContext[] { InstanceScope.INSTANCE };
		String lineSeparator = prefs.getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
		if (lineSeparator == null) {
			lineSeparator = System.getProperty("line.separator");
		}
		return toLabel(lineSeparator);
	}

	private static String toLabel(String lineSeparator) {
		if ("\r\n".equals(lineSeparator)) {
			return "CRLF";
		}
		if ("\r".equals(lineSeparator)) {
			return "CR";
		}
		if ("\n".equals(lineSeparator)) {
			return "LF";
		}
		return null;
	}
}
