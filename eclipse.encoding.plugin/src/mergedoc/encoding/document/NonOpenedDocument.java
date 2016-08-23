package mergedoc.encoding.document;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IEditorPart;

import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;

/**
 * This is not opend editor in workspace handler for ActiveDocumentAgent.
 * @author Shinji Kashihara
 */
public class NonOpenedDocument extends ActiveDocument {

	public NonOpenedDocument() {
		super(null, null);
	}

	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		updateStatus();
	}

	@Override
	public boolean canOperateBOM() {
		return false;
	}

	@Override
	protected void updateStatus() {
		// Workspace preferences, not support project prefences
		currentEncoding = ResourcesPlugin.getEncoding();
		lineSeparator = LineSeparators.ofWorkspace();
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		update();
	}
}
