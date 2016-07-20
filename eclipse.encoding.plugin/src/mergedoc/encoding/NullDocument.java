package mergedoc.encoding;

import org.eclipse.ui.IEditorPart;

/**
 * This is a dummy handler for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
class NullDocument extends ActiveDocument {

	public NullDocument(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		super(editor, callback);
	}
	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		this.editor = editor;
	}
	@Override
	public String getFileName() {
		return null;
	}
	@Override
	public void propertyChanged(Object source, int propId) {
	}
	@Override
	public void setEncoding(String encoding) {
	}
	@Override
	public String getInheritedEncoding() {
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
	public String getLineSeparator() {
		return null;
	}
	@Override
	public boolean canChangeFileEncoding() {
		return false;
	}
}
