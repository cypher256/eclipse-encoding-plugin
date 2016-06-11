package mergedoc.encoding;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

/**
 * This handler handles workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class WorkspaceTextFileHandler extends EncodedDocumentHandler {

	// The text file associated with the editor.
	private IFile text_file = null;

	public WorkspaceTextFileHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		super(part, callback);

		if (!(part.getEditorInput() instanceof IFileEditorInput)) throw new IllegalArgumentException("part must provide IFileEditorInput.");

		text_file = ((IFileEditorInput) part.getEditorInput()).getFile();

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
		try {
			containerEncoding = null;
			containerEncoding = text_file.getParent().getDefaultCharset();
		} catch (CoreException e) {
			// NOP
			e.printStackTrace();
		}
		try {
			lineEnding = null;
			setLineBreak(text_file.getContents());
		} catch (Exception e) {
			// NOP
			e.printStackTrace();
		}

		// Just assume that the encoding information is updated.
		return true;
	}

}
