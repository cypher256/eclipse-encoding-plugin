package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
	private IFile text_file;

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
	public boolean isContentWriteable() {
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
				detectedEncoding = EncodingUtil.detectEncoding(getInputStream());
				IContentDescription contentDescription = text_file.getContentDescription();
				if (contentDescription != null) {
					contentTypeEncoding = contentDescription.getCharset();
				}
				lineEnding = EncodingUtil.getLineEnding(getInputStream(), getEncoding());

			} catch (CoreException e) {
				throw new IllegalStateException(e);
			}
		}

		// Just assume that the encoding information is updated.
		return true;
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return text_file.getContents(true);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	// Note: Process on String, not stream. Unsupport big file.
	private String getContentString() {
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
			return sb.toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtil.closeQuietly(is);
		}
	}

	private void setContentString(String content, String storeEncoding) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(storeEncoding));
			text_file.setContents(bis, true, true, null);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void setLineEnding(String newLineEnding) {
		if (newLineEnding.equals(lineEnding)) {
			return;
		}
		String newEnding = "\r\n";
		if (newLineEnding.equals("CR")) {
			newEnding = "\r";
		} else if (newLineEnding.equals("LF")) {
			newEnding = "\n";
		}
		String content = getContentString().replaceAll("(\\r\\n|\\r|\\n)", newEnding);
		setContentString(content, getEncoding());
	}

	@Override
	public void convertCharset(String newEncoding) {
		String content = getContentString();
		setContentString(content, newEncoding);
		setEncoding(newEncoding);
	}
}
