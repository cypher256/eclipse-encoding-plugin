package mergedoc.encoding.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;

import mergedoc.encoding.Charsets;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;

/**
 * This handler handles non-workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class NonWorkspaceFileDocument extends ActiveDocument {

	// The text file associated with the editor.
	private IFileStore fileStore;

	public NonWorkspaceFileDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
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
	public IContentDescription getContentDescription() {
		IContentType contentType = getContentType();
		if (contentType != null) {
			return contentType.getDefaultDescription();
		}
		return null;
	}

	public IContentType getContentType() {
		return Platform.getContentTypeManager().findContentTypeFor(getFileName());
	}

	@Override
	protected void updateEncodingInfo() {

		super.updateEncodingInfo();

		if (fileStore != null) {
			inheritedEncoding = ResourcesPlugin.getEncoding();
			detectedCharset = Charsets.detect(getInputStream());

			IContentType contentType = getContentType();
			if (contentType != null) {
				contentTypeEncoding = contentType.getDefaultCharset();

				InputStream inputStream = getInputStream();
				try {
					IContentDescription description = contentType.getDescriptionFor(
						inputStream, new QualifiedName[]{IContentDescription.CHARSET});
					if (description != null) {
						contentCharset = description.getCharset();
						if (contentCharset != null) {
							currentEncoding = contentCharset;
						}
					}
				} catch (IOException e) {
					throw new IllegalStateException(e);
				} finally {
					IOUtils.closeQuietly(inputStream);
				}
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
			return fileStore.openInputStream(EFS.NONE, null);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void setContentString(String content, String storeEncoding) {
		OutputStream os = null;
		try {
			os = fileStore.openOutputStream(EFS.NONE, null);
			os.write(content.getBytes(storeEncoding));
			os.flush();
			IDocumentProvider provider = ((AbstractTextEditor) getEditor()).getDocumentProvider();
			((IDocumentProviderExtension) provider).synchronize(getEditor().getEditorInput());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(os);
		}
	}
}
