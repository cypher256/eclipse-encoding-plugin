package mergedoc.encoding.document;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

import mergedoc.encoding.IActiveDocumentAgentCallback;
import mergedoc.encoding.LineSeparators;

/**
 * This is no opend editor in workspace handler for ActiveDocumentAgent.
 * @author Shinji Kashihara
 */
@SuppressWarnings("restriction")
public class NullDocumentWorkspace extends NullDocument {

	public NullDocumentWorkspace() {
	}
	
	@Override
	protected void init(IEditorPart editor, IActiveDocumentAgentCallback callback) {
		updateEncodingInfo();
	}
	
	@Override
	protected void updateEncodingInfo() {
		// Workspace preferences, not support project prefences
		currentEncoding = ResourcesPlugin.getEncoding();
		lineSeparator = LineSeparators.ofWorkspace();
	}
	
	@Override
	public void propertyChanged(Object source, int propId) {
		if (updateEncoding()) {
			callback.encodingChanged();
		}
	}
	
	@Override
	protected IStatusLineManager getStatusLineManager() {
		WorkbenchWindow window = (WorkbenchWindow) PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return window.getActionBars().getStatusLineManager();
	}
}
