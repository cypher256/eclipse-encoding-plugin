package mergedoc.encoding;

import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import mergedoc.encoding.document.ActiveDocument;
import mergedoc.encoding.document.ClassFileDocument;
import mergedoc.encoding.document.NonWorkspaceFileDocument;
import mergedoc.encoding.document.NullDocument;
import mergedoc.encoding.document.NullDocumentWorkspace;
import mergedoc.encoding.document.StorageFileDocument;
import mergedoc.encoding.document.WorkspaceFileDocument;

/**
 * This agent tries to provide the encoding of the document of the active editor.
 * It also provides method to set the encoding of the document.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class ActiveDocumentAgent implements IPropertyListener, IPartListener, IPageChangedListener {

	// Callback for this agent.
	private IActiveDocumentAgentCallback callback;

	// The current document for the agent.
	private ActiveDocument currentDocument;

	// Indicate whether the agent has started monitoring the encoding of the active document.
	private boolean isStarted = false;

	// The IWorkbenchWindow to work on.
	IWorkbenchWindow window;

	public ActiveDocumentAgent(IActiveDocumentAgentCallback callback) {

		if (callback == null) throw new IllegalArgumentException("Please provide a callback.");
		this.callback = callback;

		// Initialize the current handler to a dummy handler, so that we do not need to check whether it is null.
		setCurrentDocument(getDocument(null));
	}

	/**
	 * Get the active editor.
	 * @return the active editor, or null if there is no active editor.
	 */
	private IEditorPart getActiveEditor() {

		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				return page.getActiveEditor();
			}
		}
		return null;
	}

	/**
	 * Check whether the active document is dirty or not.
	 * @return true/false
	 */
	public boolean isDocumentDirty() {
		return currentDocument != null && currentDocument.getEditor() != null && currentDocument.getEditor().isDirty();
	}

	public ActiveDocument getDocument() {
		return currentDocument;
	}

	/**
	 * Get a handler for an editor.
	 * @return a specific handler, or NullDocument if there is no specific handler for an editor.
	 */
	private ActiveDocument getDocument(IEditorPart editor) {

		if (editor == null) {
			// No opend editor in workspace
			return new NullDocumentWorkspace();
		}
		else if (editor.getAdapter(IEncodingSupport.class) != null) {

			// MultiPartEditor active tab
			if (editor instanceof FormEditor) {
				 IEditorPart e = ((FormEditor) editor).getActiveEditor();
				 if (e instanceof ITextEditor) {
					editor = e;
				 }
			}
			IEditorInput editorInput = editor.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				return new WorkspaceFileDocument(editor, callback);
			}
			else if (editorInput instanceof FileStoreEditorInput) {
				return new NonWorkspaceFileDocument(editor, callback);
			}
			else if (editorInput instanceof IStorageEditorInput) {
				// Non class file resources in jar
				StorageFileDocument doc = new StorageFileDocument(editor, callback);
				if (doc.hasContent()) {
					return doc;
				}
				// Fallback
				return new ActiveDocument(editor, callback);
			} else if (editorInput.getClass().getSimpleName().equals("InternalClassFileEditorInput")) {
				// Class file in jar
				return new ClassFileDocument(editor, callback);
			}
		}
		// MultiPageEditor no document tab
		return new NullDocument();
	}

	/**
	 * Start to monitor the encoding of the active document, if not started yet.
	 * @param window The IWorkbenchWindow to work on.
	 */
	public void start(IWorkbenchWindow window) {

		if (!isStarted) {
			if (window != null) {
				isStarted = true;
				this.window = window;

				// Update the current handler.
				// Not invoke the callback during start.
				setCurrentDocument(getDocument(getActiveEditor()));

				// Add listeners.
				window.getPartService().addPartListener(this);
			}
		}
	}

	/**
	 * Stop to monitor the encoding of the active document, if started.
	 */
	public void stop() {

		if (isStarted) {
			// Remove listeners.
			window.getPartService().removePartListener(this);

			// Reset the current handler to a dummy handler, which will remove IPropertyListener if added.
			setCurrentDocument(getDocument(null));

			window = null;
			isStarted = false;
		}
	}

	/**
	 * Change the current handler.
	 * This method helps to add/remove IPropertyListener as needed.
	 * @param document
	 */
	private void setCurrentDocument(ActiveDocument document) {

		if (document == null) throw new IllegalArgumentException("handler must not be null.");

		// Remove IPropertyListener from the old editor.
		if (currentDocument != null) {
			IEditorPart editor = currentDocument.getEditor();
			if (editor != null) {
				editor.removePropertyListener(this);
			}
		}
		currentDocument = document;

		// Add IPropertyListener to the new editor.
		IEditorPart editor = currentDocument.getEditor();
		if (editor != null) {
			editor.addPropertyListener(this);
		}
	}

	/**
	 * Check whether the active editor is changed.
	 */
	private void checkActiveEditor() {

		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor != currentDocument.getEditor()) {
			// Get a new handler for the active editor, and invoke the callback.
			setCurrentDocument(getDocument(activeEditor));
			callback.encodingChanged();
		}
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		if (propId == IEditorPart.PROP_INPUT) {
			// The current handler may not be able to handle the new editor input,
			// so get a new handler for the active editor, and invoke the callback.
			setCurrentDocument(getDocument(getActiveEditor()));
			callback.encodingChanged();
		}
		else {
			// Pass the event to the handler.
			currentDocument.propertyChanged(source, propId);
		}
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		if (part instanceof IPageChangeProvider) {
			((IPageChangeProvider) part).addPageChangedListener(this);
		}
		checkActiveEditor();
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		if (part instanceof IPageChangeProvider) {
			((IPageChangeProvider) part).removePageChangedListener(this);
		}
		checkActiveEditor();
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void pageChanged(PageChangedEvent event) {
		// MultiPageEditorPart tab changed
		setCurrentDocument(getDocument(getActiveEditor()));
		callback.encodingChanged();
	}

	public void fireEncodingChanged() {
		callback.encodingChanged();
	}
}
