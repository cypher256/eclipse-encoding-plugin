package mergedoc.encoding.document;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

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

import mergedoc.encoding.Activator;
import mergedoc.encoding.Charsets;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;

/**
 * This handler handles non-workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class ExternalFileDocument extends ActiveDocument {

	// The text file associated with the editor.
	private IFileStore fileStore;

	public ExternalFileDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
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
	public IContentDescription getContentDescription() {
		IContentType contentType = getContentType();
		if (contentType != null) {
			return contentType.getDefaultDescription();
		}
		return null;
	}

	@Override
	protected byte[] resolveBOM() {
		InputStream inputStream = getInputStream();
		if (inputStream == null) {
			return null;
		}
		try {
			int first = inputStream.read();
			if (first == 0xEF) {
				int second = inputStream.read();
				int third = inputStream.read();
				if (second == 0xBB && third == 0xBF)
					return IContentDescription.BOM_UTF_8;
			} else if (first == 0xFE) {
				if (inputStream.read() == 0xFF)
					return IContentDescription.BOM_UTF_16BE;
			} else if (first == 0xFF) {
				if (inputStream.read() == 0xFE)
					return IContentDescription.BOM_UTF_16LE;
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
		return null;
	}

	public IContentType getContentType() {
		return Platform.getContentTypeManager().findContentTypeFor(getFileName());
	}

	@Override
	protected void updateStatus() {

		super.updateStatus();

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

		// Sync file and editor using refrection. The synchronize method not working UTF-8.
		/*
		IDocumentProvider provider = ((ITextEditor) editor).getDocumentProvider();
		((IDocumentProviderExtension) provider).synchronize(editor.getEditorInput());
		*/
		if (editor instanceof AbstractTextEditor) {
			try {
				AbstractTextEditor textEditor = (AbstractTextEditor) editor;
				IDocumentProvider provider = textEditor.getDocumentProvider();
				long providerStamp = provider.getModificationStamp(textEditor.getEditorInput());

				Field editorStampField = AbstractTextEditor.class.getDeclaredField("fModificationStamp");
				editorStampField.setAccessible(true);
				editorStampField.set(textEditor, providerStamp);

			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return fileStore.openInputStream(EFS.NONE, null);
		}
		// File not found, etc...
		catch (CoreException e) {
			Activator.warn(e.getMessage());
			return null;
		}
	}

	@Override
	protected void setContents(byte[] bytes) {
		OutputStream os = null;
		try {
			os = fileStore.openOutputStream(EFS.NONE, null);
			os.write(bytes);
			os.flush(); // Sync file and editor in updateEncoding
		} catch (Exception e) {
			throw new IllegalStateException(e);
		} finally {
			IOUtils.closeQuietly(os);
		}
	}
}
