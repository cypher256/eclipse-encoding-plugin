package mergedoc.encoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

/**
 * This handler handles workspace text file for ActiveDocumentAgent.
 * Assume that the ITextEditor supports IEncodingSupport too.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
@SuppressWarnings("restriction")
class WorkspaceFileDocument extends ActiveDocument {

	private IFile file;
	private PackageRoot packageRoot = new PackageRoot();

	public WorkspaceFileDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		super(editor, callback);
		if (!(editor.getEditorInput() instanceof IFileEditorInput)) {
			throw new IllegalArgumentException("part must provide IFileEditorInput.");
		}
		file = ((IFileEditorInput) editor.getEditorInput()).getFile();
		updateEncodingInfoPrivately();
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

		inheritedEncoding = null;
		detectedEncoding = null;
		contentTypeEncoding = null;
		lineSeparator = null;

		packageRoot.element = null;
		packageRoot.encoding = null;

		if (file != null) {
			try {
				inheritedEncoding = file.getParent().getDefaultCharset();
				detectedEncoding = Encodings.detectEncoding(getInputStream());
				IContentDescription contentDescription = file.getContentDescription();
				if (contentDescription != null) {
					contentTypeEncoding = contentDescription.getCharset();
				}
				lineSeparator = Encodings.getLineEnding(getInputStream(), getCurrentEncoding());

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
				
			} catch (ResourceException e) {
				// IFile#getContentDescription - Resource is out of sync with the file system
				Activator.info(e.getMessage());
				
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

		// Just assume that the encoding information is updated.
		return true;
	}

	@Override
	public boolean canChangeFileEncoding() {
		return true;
	}
	@Override
	public boolean canConvertLineEnding() {
		return true;
	}
	@Override
	public boolean enabledContentTypeEnding() {
		return true;
	}

	@Override
	protected InputStream getInputStream() {
		try {
			return file.getContents(true);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected void setContentString(String content, String storeEncoding) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(storeEncoding));
			file.setContents(bis, true, true, null);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		} catch (CoreException e) {
			throw new IllegalStateException(e);
		}
	}
}
