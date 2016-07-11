package mergedoc.encoding;

import org.eclipse.core.runtime.CoreException;
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

/**
 * This agent tries to provide the encoding of the document of the active editor.
 * It also provides method to set the encoding of the document.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class ActiveDocumentAgent implements IPropertyListener, IPartListener, IPageChangedListener {

	// Callback for this agent.
	private IActiveDocumentAgentCallback callback;

	// The current handler for the agent.
	private ActiveDocumentHandler currentHandler;

	// Indicate whether the agent has started monitoring the encoding of the active document.
	private boolean isStarted = false;

	// The IWorkbenchWindow to work on.
	IWorkbenchWindow window;

	public ActiveDocumentAgent(IActiveDocumentAgentCallback callback) {

		if (callback == null) throw new IllegalArgumentException("Please provide a callback.");
		this.callback = callback;

		// Initialize the current handler to a dummy handler, so that we do not need to check whether it is null.
		setCurrentHandler(getHandler(null));
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
		return currentHandler != null && currentHandler.getEditor() != null && currentHandler.getEditor().isDirty();
	}

	public ActiveDocumentHandler getHandler() {
		return currentHandler;
	}

	/**
	 * Get a handler for an editor.
	 * @return a specific handler, or NullDocumentHandler if there is no specific handler for an editor.
	 */
	private ActiveDocumentHandler getHandler(IEditorPart editor) {

		if (editor != null && editor.getAdapter(IEncodingSupport.class) != null) {
			
			// MultiPartEditor active tab
			if (editor instanceof FormEditor) {
				 IEditorPart e = ((FormEditor) editor).getActiveEditor();
				 if (e instanceof ITextEditor) {
					editor = e;
				 }
			}

			IEditorInput editorInput = editor.getEditorInput();
			if (editorInput instanceof IFileEditorInput) {
				return new WorkspaceTextFileHandler(editor, callback);
			}
			else if (editorInput instanceof FileStoreEditorInput) {
				return new NonWorkspaceTextFileHandler(editor, callback);
			}
			else if (editorInput instanceof IStorageEditorInput) {
				// Non class file resources in jar
				try {
					return new StorageEditorInputHandler(editor, callback);
				} catch (CoreException e) {
					// Fallback
					return new ActiveDocumentHandler(editor, callback);
				}
			} else if (editorInput.getClass().getSimpleName().equals("InternalClassFileEditorInput")) {
				// Class file in jar
				return new ClassFileEditorInputHandler(editor, callback);
			}
		}
		return new NullDocumentHandler(editor, callback);
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
				setCurrentHandler(getHandler(getActiveEditor()));

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
			setCurrentHandler(getHandler(null));

			window = null;
			isStarted = false;
		}
	}

	/**
	 * Change the current handler.
	 * This method helps to add/remove IPropertyListener as needed.
	 * @param handler
	 */
	private void setCurrentHandler(ActiveDocumentHandler handler) {

		if (handler == null) throw new IllegalArgumentException("handler must not be null.");

		// Remove IPropertyListener from the old editor.
		if (currentHandler != null) {
			IEditorPart editor = currentHandler.getEditor();
			if (editor != null) {
				editor.removePropertyListener(this);
			}
		}
		currentHandler = handler;

		// Add IPropertyListener to the new editor.
		IEditorPart editor = currentHandler.getEditor();
		if (editor != null) {
			editor.addPropertyListener(this);
		}
	}

	/**
	 * Check whether the active editor is changed.
	 */
	private void checkActiveEditor() {

		IEditorPart active_editor = getActiveEditor();
		if (active_editor != currentHandler.getEditor()) {
			// Get a new handler for the active editor, and invoke the callback.
			setCurrentHandler(getHandler(active_editor));
			callback.encodingInfoChanged();
		}
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		if (propId == IEditorPart.PROP_INPUT) {
			// The current handler may not be able to handle the new editor input,
			// so get a new handler for the active editor, and invoke the callback.
			setCurrentHandler(getHandler(getActiveEditor()));
			callback.encodingInfoChanged();
		}
		else {
			// Pass the event to the handler.
			currentHandler.propertyChanged(source, propId);
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
		setCurrentHandler(getHandler(getActiveEditor()));
		callback.encodingInfoChanged();
	}
}
