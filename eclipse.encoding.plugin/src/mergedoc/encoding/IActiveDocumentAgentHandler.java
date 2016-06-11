package mergedoc.encoding;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The interface for the handlers used by ActiveDocumentAgent.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
interface IActiveDocumentAgentHandler {
	/**
	 * Get the editor associated with this handler.
	 * If the associated editor is different from the active editor, ActiveDocumentAgent will change handler.
	 */
	public IEditorPart getEditor();

	// Methods corresponding to those in ActiveDocumentAgent.
	public String getEncoding();
	public String getName();
	public void propertyChanged(Object source, int propId);
	public void resourceChanged(IResourceChangeEvent event);
	public void selectionChanged(IWorkbenchPart part, ISelection selection);
	public void setEncoding(String encoding);

	// ADD S.Kashihara
	public String getContainerEncoding();
	public String getLineEnding();
	public boolean enableChangeEncoding();
}
