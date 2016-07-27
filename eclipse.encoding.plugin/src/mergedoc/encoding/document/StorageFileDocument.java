package mergedoc.encoding.document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IStorageEditorInput;

import mergedoc.encoding.Charsets;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;

/**
 * This handler handles IStorageEditorInput for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class StorageFileDocument extends ActiveDocument {

	// The storage object associated with the editor.
	private IStorage storage;

	public StorageFileDocument(IEditorPart part, IActiveDocumentAgentCallback callback) {
		super(part, callback);
	}

	public boolean hasContent() {
		return storage != null;
	}

	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		if (!(editor.getEditorInput() instanceof IStorageEditorInput)) {
			throw new IllegalArgumentException("part must provide IStorageEditorInput.");
		}
		try {
			storage = ((IStorageEditorInput) editor.getEditorInput()).getStorage();
		} catch (CoreException e1) {
			// hasContent is false
			return;
		}
		// pom editor Effective pom tab, initial content 'Loading Effective pom...'
		// Fixed UTF-8, LF
		if (storage.getClass().getName().endsWith("MavenStorage")) {
			currentEncoding = "UTF-8";
			storage = new IStorage() {
				@Override
				public <T> T getAdapter(Class<T> adapter) {
					return null;
				}
				@Override
				public boolean isReadOnly() {
					return true;
				}
				@Override
				public String getName() {
					return null;
				}
				@Override
				public IPath getFullPath() {
					return null;
				}
				@Override
				public InputStream getContents() {
					try {
						return new ByteArrayInputStream("\n".getBytes(currentEncoding));
					} catch (UnsupportedEncodingException e) {
						return null;
					}
				}
			};
		}
		super.init(editor, callback);
	}

	@Override
	protected void updateStatus() {

		super.updateStatus();

		detectedCharset = Charsets.detect(getInputStream());
		IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(getFileName());
		if (contentType != null) {
			contentTypeEncoding = contentType.getDefaultCharset();
		}
		lineSeparator = LineSeparators.ofContent(getInputStream(), getCurrentEncoding());
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
}
