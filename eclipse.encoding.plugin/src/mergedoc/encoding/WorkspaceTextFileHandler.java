package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
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

	@Override
	public boolean isFileEncodingChangeable() {
		return true;
	}
	@Override
	public boolean isLineEndingChangeable() {
		return lineEnding != null;
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

		containerEncoding = null;
		detectedEncoding = null;
		contentTypeEncoding = null;
		lineEnding = null;

		if (text_file != null) {
			try {
				containerEncoding = text_file.getParent().getDefaultCharset();
			} catch (CoreException e) {
				// NOP
			}
			try {
				detectedEncoding = EncodingUtil.detectEncoding(getInputStream());
			} catch (Exception e) {
				// NOP
			}
			try {
				IContentDescription contentDescription = text_file.getContentDescription();
				if (contentDescription != null) {
					contentTypeEncoding = contentDescription.getCharset();
				}
			} catch (Exception e) {
				// NOP
			}
			try {
				resolveLineEnding(getInputStream());
			} catch (Exception e) {
				// NOP
			}
		}

		// Just assume that the encoding information is updated.
		return true;
	}

	@Override
	protected InputStream getInputStream() throws CoreException {
		return text_file.getContents(true);
	}

	@Override
	public void setLineEnding(String newLineEnding) {

		if (newLineEnding.equals(lineEnding)) {
			return;
		}
		// Note: Replace on String, not stream. Unsupport big file.
		try {
			InputStream is = null;
			try {
				is = getInputStream();
				InputStreamReader reader = new InputStreamReader(new BufferedInputStream(is), getEncoding());
				StringBuilder sb = new StringBuilder();
				char[] buff = new char[4096];
				int read;
				while((read = reader.read(buff)) != -1) {
				    sb.append(buff, 0, read);
				}

				String newEnding = "\r\n";
				if (newLineEnding.equals("CR")) {
					newEnding = "\r";
				} else if (newLineEnding.equals("LF")) {
					newEnding = "\n";
				}
				String content = sb.toString().replaceAll("(\\r\\n|\\r|\\n)", newEnding);
				ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(getEncoding()));
				text_file.setContents(bis, true, true, null);
			}
			finally {
				if (is != null) {
					is.close();
				}
			}
		} catch (Exception e) {
			// NOP
		}
	}
}
