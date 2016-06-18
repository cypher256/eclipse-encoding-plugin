package mergedoc.encoding;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This is a dummy handler for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class DummyHandler implements IActiveDocumentAgentHandler {

	// The editor associated with this handler.
	private IEditorPart editor;

	public DummyHandler(IEditorPart part, IActiveDocumentAgentCallback callback) {
		editor = part;
	}

	@Override
	public IEditorPart getEditor() {
		return editor;
	}

	@Override
	public String getEncoding() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void propertyChanged(Object source, int propId) {
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
	}

	@Override
	public void setEncoding(String encoding) {
	}

	// ADD S.Kashihara
	@Override
	public String getContainerEncoding() {
		return null;
	}
	@Override
	public String getDetectedEncoding() {
		return null;
	}
	@Override
	public String getContentTypeEncoding() {
		return null;
	}
	@Override
	public String getLineEnding() {
		return null;
	}
	@Override
	public boolean isFileEncodingChangeable() {
		return false;
	}
	@Override
	public boolean isContentWriteable() {
		return false;
	}
	@Override
	public void setLineEnding(String lineEnding) {
	}

	@Override
	public void convertCharset(String encoding) {
	}
}
