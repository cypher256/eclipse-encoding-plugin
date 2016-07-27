package mergedoc.encoding.document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import mergedoc.encoding.Activator;
import mergedoc.encoding.Charsets;
import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;
import mergedoc.encoding.PackageRoot;

/**
 * This handler handles workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
@SuppressWarnings("restriction")
public class WorkspaceFileDocument extends ActiveDocument {

	private IFile file;
	private PackageRoot packageRoot;

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
	public IProject getProject() {
		return file.getProject();
	}

	@Override
	public PackageRoot getPackageRoot() {
		return packageRoot;
	}

	@Override
	public IFile getFile() {
		return file;
	}

	@Override
	public String getFilePropertiesEncoding() {
		try {
			return file.getCharset(false); // Non inheritance
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public IContentDescription getContentDescription() {
		try {
			return file.getContentDescription();
		} catch (ResourceException e) {
			return null; // Out of sync, etc...
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void updateStatus() {

		super.updateStatus();

		if (packageRoot == null) {
			packageRoot = new PackageRoot();
		}
		packageRoot.element = null;
		packageRoot.encoding = null;

		try {
			inheritedEncoding = file.getParent().getDefaultCharset();
			detectedCharset = Charsets.detect(getInputStream());

			IContentDescription contentDescription = getContentDescription();
			if (contentDescription != null) {
				contentCharset = contentDescription.getCharset();
				if (contentCharset != null && getFilePropertiesEncoding() == null) {
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

			IEditorInput editorInput = editor.getEditorInput();
			Object ele = AdapterManager.getDefault().getAdapter(editorInput, "org.eclipse.jdt.core.IJavaElement");
			if (ele != null) {
				final int PACKAGE_FRAGMENT_ROOT = 3; // IJavaElement.PACKAGE_FRAGMENT_ROOT
				packageRoot.element = (IAdaptable) ele.getClass()
						.getMethod("getAncestor", int.class).invoke(ele, PACKAGE_FRAGMENT_ROOT);
				IContainer c = (IContainer) packageRoot.element.getClass()
						.getMethod("resource").invoke(packageRoot.element);
				packageRoot.encoding = c.getDefaultCharset(false);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public boolean canChangeFileEncoding() {
		return true;
	}
	@Override
	public boolean canConvertLineSeparator() {
		return true;
	}
	@Override
	public boolean enabledContentType() {
		return true;
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return file.getContents(true);
		}
		// Out of sync, etc...
		catch (ResourceException e) {
			Activator.warn(e.getMessage());
			return null;
		}
		// File not found, etc...
		catch (CoreException e) {
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
