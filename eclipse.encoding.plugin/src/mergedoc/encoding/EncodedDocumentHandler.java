package mergedoc.encoding;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.editors.text.IEncodingSupport;

/**
 * This handler handles editors which support IEncodingSupport for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class EncodedDocumentHandler implements IActiveDocumentAgentHandler {

	// Invoke the callback on behalf of the agent.
	private IActiveDocumentAgentCallback callback;

	// The editor associated with this handler.
	private IEditorPart editor;

	// Obtained from the editor.
	private IEncodingSupport encoding_support;

	// The encoding setting of the text file.
	private String encoding;

	public EncodedDocumentHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		if (callback == null) throw new IllegalArgumentException("callback must not be null.");
		if (part == null) throw new IllegalArgumentException("part must not be null.");

		this.callback = callback;
		editor = part;
		encoding_support = (IEncodingSupport) editor.getAdapter(IEncodingSupport.class);

		if (encoding_support == null) throw new IllegalArgumentException("part must provide IEncodingSupport.");

		updateEncodingInfoPrivately();
	}

	@Override
	public IEditorPart getEditor() {
		return editor;
	}

	@Override
	public String getEncoding() {
		return encoding;
	}

	@Override
	public String getName() {
		return editor.getEditorInput().getName();
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		// It seems that the editor's encoding will not change when it is dirty.
		if (!editor.isDirty()) {
			// The document may be just saved.
			if (updateEncodingInfo()) {
				// Invoke the callback if the encoding information is changed.
				callback.encodingInfoChanged();
			}
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		// It seems that propertyChanged() can detect changes well already.
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// It seems that propertyChanged() can detect encoding setting changes well already.
	}

	@Override
	public void setEncoding(String encoding) {
		encoding_support.setEncoding(encoding);

		if (updateEncodingInfo()) {
			// Invoke the callback if the encoding information is changed.
			callback.encodingInfoChanged();
		}
	}

	/**
	 * Update the encoding information in member variables.
	 * This method may be overrided, but should be called by the sub-class.
	 * @return true if the encoding information is updated.
	 */
	protected boolean updateEncodingInfo() {
		return updateEncodingInfoPrivately();
	}

	/**
	 * Update the encoding information in private member variables.
	 * @return true if the encoding information is updated.
	 */
	private boolean updateEncodingInfoPrivately() {
		String encoding = null;

		encoding = encoding_support.getEncoding();
		if (encoding == null) {
			// workspace encoding
			encoding = encoding_support.getDefaultEncoding();
		}

		boolean is_not_updated =
			(encoding == null ? this.encoding == null : encoding.equals(this.encoding));

		this.encoding = encoding;

		return !is_not_updated;
	}

	// ADD S.Kashihara
	protected String containerEncoding;
	protected String detectedEncoding;
	protected String contentTypeEncoding;
	protected String lineEnding;

	@Override
	public String getContainerEncoding() {
		return containerEncoding;
	}
	@Override
	public String getDetectedEncoding() {
		return detectedEncoding;
	}
	@Override
	public String getContentTypeEncoding() {
		return contentTypeEncoding;
	}
	@Override
	public String getLineEnding() {
		return lineEnding;
	}
	@Override
	public boolean isFileEncodingChangeable() {
		return false;
	}
	@Override
	public boolean isLineEndingChangeable() {
		return false;
	}

	protected void resolveLineEnding(InputStream is) throws IOException {

		try {
			InputStreamReader reader = new InputStreamReader(new BufferedInputStream(is), encoding);
			StringBuilder sb = new StringBuilder();
			char[] buff = new char[4096];
			int read;
			while((read = reader.read(buff)) != -1) {
			    sb.append(buff, 0, read);
			    if (sb.length() >= buff.length) {
			    	break;
			    }
			}
			String s = sb.toString();
			boolean crlf = s.contains("\r\n");
			if (crlf) {
				s = s.replaceAll("\\r\\n", "");
			}
			boolean cr = s.contains("\r");
			boolean lf = s.contains("\n");

			if (crlf && !cr && !lf) {
				lineEnding = "CRLF";
			} else if (!crlf && cr && !lf) {
				lineEnding = "CR";
			} else if (!crlf && !cr && lf) {
				lineEnding = "LF";
			} else if (!crlf && !cr && !lf) {
				lineEnding = null;
			} else {
				lineEnding = "Mixed";
			}
		} finally {
			is.close();
		}
	}

	protected InputStream getInputStream() throws CoreException {
		throw new UnsupportedOperationException("Not implements getInputStream method.");
	}

	@Override
	public void setLineEnding(String newLineEnding) {
		throw new UnsupportedOperationException("Not implements setLineEnding method.");
	}
}
