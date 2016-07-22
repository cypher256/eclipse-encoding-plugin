package mergedoc.encoding.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;

import mergedoc.encoding.Encodings;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.IOs;
import mergedoc.encoding.LineSeparators;

/**
 * This handler handles non-workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class NonWorkspaceFileDocument extends ActiveDocument {

	// The text file associated with the editor.
	private IFileStore text_file_store = null;

	public NonWorkspaceFileDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		super(editor, callback);
	}
	
	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		if (!(editor.getEditorInput() instanceof FileStoreEditorInput)) {
			throw new IllegalArgumentException("part must provide FileStoreEditorInput.");
		}
		try {
			text_file_store = EFS.getStore(((FileStoreEditorInput) editor.getEditorInput()).getURI());
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
		super.init(editor, callback);
	}

	@Override
	protected void updateEncodingInfo() {
		
		super.updateEncodingInfo();

		if (text_file_store != null) {
			inheritedEncoding = ResourcesPlugin.getEncoding();
			detectedEncoding = Encodings.detectEncoding(getInputStream());
			IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(getFileName());
			if (contentType != null) {
				contentTypeEncoding = contentType.getDefaultCharset();
			}
			lineSeparator = LineSeparators.ofContent(getInputStream(), getCurrentEncoding());
		}
	}

	@Override
	public boolean canChangeFileEncoding() {
		return true;
	}
	@Override
	public boolean enabledContentType() {
		return true;
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return text_file_store.openInputStream(EFS.NONE, null);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void setContentString(String content, String storeEncoding) {
		OutputStream os = null;
		try {
			os = text_file_store.openOutputStream(EFS.NONE, null);
			os.write(content.getBytes(storeEncoding));
			os.flush();
			IDocumentProvider provider = ((AbstractTextEditor) getEditor()).getDocumentProvider();
			((IDocumentProviderExtension) provider).synchronize(getEditor().getEditorInput());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		} finally {
			IOs.closeQuietly(os);
		}
	}
}
