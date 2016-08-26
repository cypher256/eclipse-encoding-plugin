package mergedoc.encoding;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Provide the Eclipse resources related utility functions.
 * @author Shinji Kashihara
 */
public class Resources {

	private Resources() {
	}

	public static String getEncoding(IFile file) {
		try {
			return file.getCharset(false); // Non inheritance
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getEncoding(IContainer container) {
		try {
			return container.getDefaultCharset(false); // Non inheritance
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String getEncoding(IContainer container, String defaultValue) {
		String encoding = getEncoding(container);
		return encoding == null ? defaultValue : encoding;
	}
}
