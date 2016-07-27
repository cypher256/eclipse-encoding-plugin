package mergedoc.encoding.document;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import mergedoc.encoding.Activator;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;
import mergedoc.encoding.PackageRoot;

/**
 * This handler handles InternalClassFileEditorInput for ActiveDocumentAgent.
 * @author Shinji Kashihara
 */
public class ClassFileDocument extends ActiveDocument {

	private Object classFile;
	private PackageRoot packageRoot;

	public ClassFileDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		super(editor, callback);
	}
	
	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		IEditorInput editorInput = editor.getEditorInput();
		try {
			classFile = editorInput.getClass().getMethod("getClassFile").invoke(editorInput);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		super.init(editor, callback);
	}

	@Override
	public PackageRoot getPackageRoot() {
		return packageRoot;
	}

	@Override
	protected void updateEncoding() {

		super.updateEncoding();
		if (packageRoot == null) {
			packageRoot = new PackageRoot();
		}
		packageRoot.element = null;
		packageRoot.encoding = null;

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
			lineSeparator = LineSeparators.ofContent(new StringReader(content));
		} else {
			// Non source code, don't show.
			currentEncoding = null;
		}
	}

	@Override
	protected String getContentString() {
		try {
			return (String) classFile.getClass().getMethod("getSource").invoke(classFile);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
