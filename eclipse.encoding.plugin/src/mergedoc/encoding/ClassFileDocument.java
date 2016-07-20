package mergedoc.encoding;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

/**
 * This handler handles InternalClassFileEditorInput for ActiveDocumentAgent.
 * @author Shinji Kashihara
 */
class ClassFileDocument extends ActiveDocument {

	private Object classFile;
	private PackageRoot packageRoot = new PackageRoot();

	public ClassFileDocument(IEditorPart part, IActiveDocumentAgentCallback callback) {
		super(part, callback);
		IEditorInput editorInput = part.getEditorInput();
		try {
			classFile = editorInput.getClass().getMethod("getClassFile").invoke(editorInput);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		updateEncodingInfoPrivately();
	}

	@Override
	public PackageRoot getPackageRoot() {
		return packageRoot;
	}

	/**
	 * Update the encoding information in member variables.
	 * This method may be overrided, but should be called by the sub-class.
	 * @return true if the encoding information is updated.
	 */
	protected boolean updateEncodingInfo() {
		return super.updateEncodingInfo() | updateEncodingInfoPrivately();
	}

	/**
	 * Update the encoding information in private member variables.
	 * @return true if the encoding information is updated.
	 */
	private boolean updateEncodingInfoPrivately() {

		inheritedEncoding = null;
		detectedEncoding = null; // Can't take source InputStream
		contentTypeEncoding = null;
		lineEnding = null;

		packageRoot.element = null;
		packageRoot.encoding = null;

		if (classFile != null) {
			try {
				packageRoot.element = (IAdaptable) classFile.getClass().getMethod("getPackageFragmentRoot").invoke(classFile);

				// Encoding of attached source classpath attribute in jar file.
				// workspace or jar setting, don't use project encoding.
				IAdaptable root = packageRoot.element;
				Object entry = root.getClass().getMethod("getRawClasspathEntry").invoke(root);
				Object attrs = entry.getClass().getMethod("getExtraAttributes").invoke(entry);
				if (Array.getLength(attrs) > 0) {
					Object attr = Array.get(attrs, 0);
					Object name = attr.getClass().getMethod("getName").invoke(attr);

					// .classpath file
					if ("source_encoding".equals(name)) {
						currentEncoding = (String) attr.getClass().getMethod("getValue").invoke(attr);
						packageRoot.encoding = currentEncoding;
					}
				}
				
			} catch (InvocationTargetException e) {
				// Non class path entry getRawClasspathEntry
				Activator.info(e.getCause().getMessage() + " " + getClass().getSimpleName());
				
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}

			IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(getFileName());
			if (contentType != null) {
				contentTypeEncoding = contentType.getDefaultCharset();
			}

			String content = getContentString();
			if (content != null) {
				lineEnding = Encodings.getLineEnding(new StringReader(content));
			} else {
				// Non source code, don't show.
				currentEncoding = null;
			}
		}
		// Just assume that the encoding information is updated.
		return true;
	}

	@Override
	protected String getContentString() {
		try {
			return (String) classFile.getClass().getMethod("getSource").invoke(classFile);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void warnSaveMessage(boolean showsWarn) {
	}
}
