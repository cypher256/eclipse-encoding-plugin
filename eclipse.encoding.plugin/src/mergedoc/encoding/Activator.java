package mergedoc.encoding;

import java.util.Enumeration;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * @author Shinji Kashihara
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "mergedoc.encoding";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static boolean prefIs(String prefKey) {
		return plugin.getPreferenceStore().getBoolean(prefKey);
	}

	public static String pref(String prefKey) {
		return plugin.getPreferenceStore().getString(prefKey);
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		loadImage(reg, "/icons");
	}

	private void loadImage(ImageRegistry reg, String parentPath) {
		Enumeration<String> paths = getBundle().getEntryPaths(parentPath);
		if (paths == null) { // empty dir
			return;
		}
		while (paths.hasMoreElements()) {
			String path = "/" + paths.nextElement();
			if (path.endsWith("/")) {
				loadImage(reg, path); // recursive
			} else {
				String fileBaseName = path.replaceFirst(".*?([\\w-]+)\\.\\w+", "$1");
				reg.put(fileBaseName, imageDescriptorFromPlugin(PLUGIN_ID, path));
			}
		}
	}

	public static Image getImage(String fileBaseName) {
		return plugin.getImageRegistry().get(fileBaseName);
	}

	public static void info(String message) {
		log(new Status(Status.INFO, PLUGIN_ID, message));
	}

	public static void info(String message, Throwable t) {
		log(new Status(Status.INFO, PLUGIN_ID, message, t));
	}

	public static void warn(String message) {
		log(new Status(Status.WARNING, PLUGIN_ID, message));
	}

	public static void warn(String message, Throwable t) {
		log(new Status(Status.WARNING, PLUGIN_ID, message, t));
	}

	private static void log(IStatus status) {
		getDefault().getLog().log(status);
	}
}
