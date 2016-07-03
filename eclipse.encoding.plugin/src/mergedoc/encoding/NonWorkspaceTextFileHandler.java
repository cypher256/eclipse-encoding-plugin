package mergedoc.encoding;

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

/**
 * This handler handles non-workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class NonWorkspaceTextFileHandler extends ActiveDocumentHandler {

	// The text file associated with the editor.
	private IFileStore text_file_store = null;

	public NonWorkspaceTextFileHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		super(part, callback);
		if (!(part.getEditorInput() instanceof FileStoreEditorInput)) {
			throw new IllegalArgumentException("part must provide FileStoreEditorInput.");
		}
		try {
			text_file_store = EFS.getStore(((FileStoreEditorInput) part.getEditorInput()).getURI());
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
		updateEncodingInfoPrivately();
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
		detectedEncoding = null;
		contentTypeEncoding = null;
		lineEnding = null;

		if (text_file_store != null) {
			inheritedEncoding = ResourcesPlugin.getEncoding();
			detectedEncoding = EncodingUtil.detectEncoding(getInputStream());
			IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(getFileName());
			if (contentType != null) {
				contentTypeEncoding = contentType.getDefaultCharset();
			}
			lineEnding = EncodingUtil.getLineEnding(getInputStream(), getCurrentEncoding());
		}
		// Just assume that the encoding information is updated.
		return true;
	}

	@Override
	public boolean canChangeFileEncoding() {
		return true;
	}
	@Override
	public boolean canConvertCharset() {
		return true;
	}
	@Override
	public boolean enabledContentTypeEnding() {
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
			IOUtil.closeQuietly(os);
		}
	}
}
