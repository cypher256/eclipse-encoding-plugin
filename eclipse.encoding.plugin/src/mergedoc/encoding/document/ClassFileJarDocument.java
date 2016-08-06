package mergedoc.encoding.document;

import java.io.StringReader;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.JarResource;
import mergedoc.encoding.LineSeparators;

/**
 * This handler handles InternalClassFileEditorInput for ActiveDocumentAgent.
 * @author Shinji Kashihara
 */
public class ClassFileJarDocument extends ActiveDocument {

	private Object classFile;
	private JarResource jarResource;

	public ClassFileJarDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
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
	public JarResource getJarResource() {
		return jarResource;
	}

	@Override
	protected void updateStatus() {

		super.updateStatus();

		if (jarResource == null) {
			jarResource = new JarResource();
		}
		jarResource.setPackageFragmentRoot(classFile.getClass(), classFile);

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
