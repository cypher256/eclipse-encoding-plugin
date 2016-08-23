package mergedoc.encoding.document;

import org.eclipse.ui.IEditorPart;

import mergedoc.encoding.IActiveDocumentAgentCallback;

/**
 * This is a dummy handler for ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class NullDocument extends ActiveDocument {

	public NullDocument(IEditorPart editor) {
		super(editor, null);
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
}
