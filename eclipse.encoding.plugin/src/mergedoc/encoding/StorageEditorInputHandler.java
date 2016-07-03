package mergedoc.encoding;

import java.io.InputStream;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;

/**
 * This handler handles IStorageEditorInput for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class StorageEditorInputHandler extends ActiveDocumentHandler {

	// The storage object associated with the editor.
	private IStorage storage = null;

	public StorageEditorInputHandler(IEditorPart part, IActiveDocumentAgentCallback callback) throws CoreException {
		super(part, callback);
		if (!(part.getEditorInput() instanceof IStorageEditorInput)) {
			throw new IllegalArgumentException("part must provide IStorageEditorInput.");
		}
		storage = ((IStorageEditorInput) part.getEditorInput()).getStorage();
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

		if (storage != null) {
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
	protected InputStream getInputStream() {
		try {
			return storage.getContents();
		} catch (CoreException e) {
			// Closed stream
			return null;
		}
	}

	@Override
	public void showWarnMessage(boolean showsWarn) {
	}
}
