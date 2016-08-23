package mergedoc.encoding.document;

import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.FileStoreEditorInput;

import mergedoc.encoding.Activator;
import mergedoc.encoding.Charsets;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;

/**
 * This handler handles decompiler class file for ActiveDocumentAgent.
 * @author Shinji Kashihara
 */
public class ClassFileSingleDocument extends ActiveDocument {

	// The text file associated with the editor.
	private IFileStore fileStore;

	public ClassFileSingleDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		super(editor, callback);
	}

	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		if (!(editor.getEditorInput() instanceof FileStoreEditorInput)) {
			throw new IllegalArgumentException("part must provide FileStoreEditorInput.");
		}
		try {
			fileStore = EFS.getStore(((FileStoreEditorInput) editor.getEditorInput()).getURI());
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
		super.init(editor, callback);
	}

	@Override
	public boolean canOperateBOM() {
		return false;
	}
	@Override
	public boolean canChangeEncoding() {
		return true;
	}

	@Override
	protected void updateStatus() {

		super.updateStatus();

		inheritedEncoding = ResourcesPlugin.getEncoding();
		detectedCharset = Charsets.detect(getInputStream());
		lineSeparator = LineSeparators.ofContent(getInputStream(), getCurrentEncoding());
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return fileStore.openInputStream(EFS.NONE, null);
		}
		// File not found, etc...
		catch (CoreException e) {
			Activator.warn(e.getMessage());
			return null;
		}
	}
}
