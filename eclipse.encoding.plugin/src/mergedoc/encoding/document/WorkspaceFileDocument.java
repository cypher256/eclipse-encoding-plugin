package mergedoc.encoding.document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import mergedoc.encoding.Activator;
import mergedoc.encoding.Charsets;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;
import mergedoc.encoding.Resources;

/**
 * This handler handles workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
@SuppressWarnings("restriction")
public class WorkspaceFileDocument extends ActiveDocument {

	private IFile file;

	public WorkspaceFileDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		super(editor, callback);
	}

	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		if (!(editor.getEditorInput() instanceof IFileEditorInput)) {
			throw new IllegalArgumentException("part must provide IFileEditorInput.");
		}
		file = ((IFileEditorInput) editor.getEditorInput()).getFile();
		super.init(editor, callback);
	}

	@Override
	public boolean canChangeEncoding() {
		return true;
	}
	@Override
	public boolean canConvertContent() {
		return true;
	}
	@Override
	public boolean enabledContentType() {
		return true;
	}

	@Override
	public IProject getProject() {
		return file.getProject();
	}

	@Override
	public IFile getFile() {
		return file;
	}

	@Override
	public IContentDescription getContentDescription() {
		try {
			return file.getContentDescription();
		} catch (ResourceException e) {
			// Out of sync, etc... => No rethrow
			return null;
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void updateStatus() {

		super.updateStatus();

		try {
			inheritedEncoding = file.getParent().getDefaultCharset();
			detectedCharset = Charsets.detect(getInputStream());

			IContentDescription contentDescription = getContentDescription();
			if (contentDescription != null) {
				contentCharset = contentDescription.getCharset();
				if (contentCharset != null && Resources.getEncoding(file) == null) {
					currentEncoding = contentCharset;
				}
				IContentType contentType = contentDescription.getContentType();
				if (contentType != null) {
					contentTypeEncoding = contentType.getDefaultCharset();
					if (Charsets.equals(contentCharset, contentTypeEncoding)) {
						contentCharset = null;
					}
				}
			}

			lineSeparator = LineSeparators.ofContent(getInputStream(), getCurrentEncoding());
			if (lineSeparator == null) {
				lineSeparator = LineSeparators.resolve(file);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return file.getContents(true);
		} catch (CoreException e) {
			// Out of sync, File not found, etc... => No rethrow
			Activator.warn(e.getMessage());
			return null;
		}
	}

	@Override
	protected void setContents(byte[] bytes) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			file.setContents(bis, true, true, null);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}
}
