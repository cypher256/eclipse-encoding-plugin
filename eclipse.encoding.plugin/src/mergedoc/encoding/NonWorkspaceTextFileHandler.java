package mergedoc.encoding;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.FileStoreEditorInput;

/**
 * This handler handles non-workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class NonWorkspaceTextFileHandler extends EncodedDocumentHandler {

	// The text file associated with the editor.
	private IFileStore text_file_store = null;

	public NonWorkspaceTextFileHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		super(part, callback);

		if (!(part.getEditorInput() instanceof FileStoreEditorInput)) throw new IllegalArgumentException("part must provide FileStoreEditorInput.");

		try {
			text_file_store = EFS.getStore(((FileStoreEditorInput) part.getEditorInput()).getURI());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		updateEncodingInfoPrivately();
	}

	// ADD S.Kashihara
	@Override
	public boolean enableChangeEncoding() {
		return true;
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

		// ADD S.Kashihara
		containerEncoding = null;
		lineEnding = null;
		if (text_file_store != null) {
			try {
				setLineBreak(text_file_store.openInputStream(EFS.NONE, null));
			} catch (Exception e) {
				// NOP
			}
		}

		// Just assume that the encoding information is updated.
		return true;
	}

}
