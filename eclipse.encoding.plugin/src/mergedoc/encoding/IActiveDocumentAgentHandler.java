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
	IEditorPart getEditor();

	// Methods corresponding to those in ActiveDocumentAgent.
	/**
	 * Get the encoding setting of the active document, if supported by the editor.
	 * @return the encoding setting or null.
	 */
	String getEncoding();

	/**
	 * Set the encoding of the active document, if supported by the editor.
	 */
	void setEncoding(String encoding);

	/**
	 * Get the name of the active document, if supported by the editor and the editor input.
	 * @return the name or null.
	 */
	String getName();

	void propertyChanged(Object source, int propId);
	void resourceChanged(IResourceChangeEvent event);
	void selectionChanged(IWorkbenchPart part, ISelection selection);

	// ADD S.Kashihara
	String getContainerEncoding();
	String getDetectedEncoding();
	String getContentTypeEncoding();
	String getLineEnding();
	boolean isFileEncodingChangeable();
	boolean isLineEndingChangeable();
	void setLineEnding(String lineEnding);
}
