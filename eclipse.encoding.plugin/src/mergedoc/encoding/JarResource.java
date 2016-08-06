package mergedoc.encoding;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;

/**
 * JAR file resource in Java project.
 * @author Shinji Kashihara
 */
public class JarResource {

	/** JAR element */
	public IAdaptable element;

	/** JAR file properties encoding */
	public String encoding;

	/**
	 * Set the target resource in JAR.
	 * @param clazz getPackageFragmentRoot declared class
	 * @param targetResource resource in JAR
	 */
	public void setPackageFragmentRoot(Class<?> clazz, Object targetResource) {

		element = null;
		encoding = null;

		try {
			element = (IAdaptable) clazz.getMethod("getPackageFragmentRoot").invoke(targetResource);

			// Encoding of attached source classpath attribute in JAR file.
			// Using the workspace or jar setting, the project encoding is not used.
			IAdaptable root = element;
			Object entry = root.getClass().getMethod("getRawClasspathEntry").invoke(root);
			Object attrs = entry.getClass().getMethod("getExtraAttributes").invoke(entry);
			if (Array.getLength(attrs) > 0) {
				Object attr = Array.get(attrs, 0);
				Object name = attr.getClass().getMethod("getName").invoke(attr);

				// .classpath file
				if ("source_encoding".equals(name)) {
					encoding = (String) attr.getClass().getMethod("getValue").invoke(attr);
				}
			}

		} catch (InvocationTargetException e) {
			// Non class path entry for getRawClasspathEntry
			Activator.info(e.getCause().getMessage() + " " + getClass().getSimpleName());

		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
