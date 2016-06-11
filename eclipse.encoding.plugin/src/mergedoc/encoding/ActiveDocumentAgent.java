package mergedoc.encoding;

import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.ui.ide.FileStoreEditorInput;


/**
 * This agent tries to provide the encoding of the document of the active editor. It also provides method to set the encoding of the document.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class ActiveDocumentAgent implements IPartListener, IPropertyListener {
	// Callback for this agent.
	private IActiveDocumentAgentCallback callback;

	// The current handler for the agent.
	private IActiveDocumentAgentHandler current_handler;

	// Indicate whether the agent has started monitoring the encoding of the active document.
	private boolean is_started = false;

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
	 * Get the encoding setting of the active document, if supported by the editor.
	 * @return the encoding setting or null.
	 */
	public String getEncoding() {
		return current_handler.getEncoding();
	}

	// ADD S.Kashihara
	public String getContainerEncoding() {
		return current_handler.getContainerEncoding();
	}
	public String getLineEnding() {
		return current_handler.getLineEnding();
	}
	public boolean enableChangeEncoding() {
		return current_handler.enableChangeEncoding();
	}

	/**
	 * Get a handler for an editor.
	 * @return a specific handler, or DummyHandler if there is no specific handler for an editor.
	 */
	private IActiveDocumentAgentHandler getHandler(IEditorPart part) {
		if (part != null) {
			if (part.getAdapter(IEncodingSupport.class) != null) {
				if (part instanceof IEditorPart) {
					IEditorPart editor = (IEditorPart) part;
					IEditorInput editor_input = editor.getEditorInput();
					if (editor_input instanceof IFileEditorInput) {
						return new WorkspaceTextFileHandler(part, callback);
					}
					else if (editor_input instanceof FileStoreEditorInput) {
						return new NonWorkspaceTextFileHandler(part, callback);
					}
					else if (editor_input instanceof IStorageEditorInput) {
						try {
							// in jar ignore class file
							return new StorageEditorInputHandler(part, callback);
						} catch (CoreException e) {
							e.printStackTrace();
							// Fallback to EncodedDocumentHandler.
							return new EncodedDocumentHandler(part, callback);
						}
					}
					else {
						// class file in jar
						return new DummyHandler(part, callback);
					}
				}
				return new EncodedDocumentHandler(part, callback);
			}
		}
		return new DummyHandler(part, callback);
	}

	/**
	 * Get the name of the active document, if supported by the editor and the editor input.
	 * @return the name or null.
	 */
	public String getName() {
		return current_handler.getName();
	}

	/**
	 * Check whether the active document is dirty or not.
	 * @return true/false
	 */
	public boolean isDocumentDirty() {
		return current_handler == null ? false : (current_handler.getEditor() == null ? false : current_handler.getEditor().isDirty());
	}

	@Override
	public void propertyChanged(Object source, int propId) {
		if (propId == IEditorPart.PROP_INPUT) {
			// The current handler may not be able to handle the new editor input, so get a new handler for the active editor, and invoke the callback.
			setCurrentHandler(getHandler(getActiveEditor()));
			callback.encodingInfoChanged();
		}
		else {
			// Pass the event to the handler.
			current_handler.propertyChanged(source, propId);
		}
	}

	@Override
	public void partActivated(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void partBroughtToTop(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void partClosed(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void partDeactivated(IWorkbenchPart part) {
		checkActiveEditor();
	}

	@Override
	public void partOpened(IWorkbenchPart part) {
		checkActiveEditor();
	}

	/**
	 * Check whether the active editor is changed.
	 */
	private void checkActiveEditor() {
		IEditorPart active_editor = getActiveEditor();
		if (active_editor != current_handler.getEditor()) {
			// Get a new handler for the active editor, and invoke the callback.
			setCurrentHandler(getHandler(active_editor));
			callback.encodingInfoChanged();
		}
	}

	/**
	 * Change the current handler.
	 * This method helps to add/remove IPropertyListener as needed.
	 * @param handler
	 */
	private void setCurrentHandler(IActiveDocumentAgentHandler handler) {
		if (handler == null) throw new IllegalArgumentException("handler must not be null.");

		// Remove IPropertyListener from the old editor.
		if (current_handler != null) {
			IEditorPart editor = current_handler.getEditor();
			if (editor != null) {
				editor.removePropertyListener(this);
			}
		}

		current_handler = handler;

		// Add IPropertyListener to the new editor.
		IEditorPart editor = current_handler.getEditor();
		if (editor != null) {
			editor.addPropertyListener(this);
		}
	}

	/**
	 * Set the encoding of the active document, if supported by the editor.
	 */
	public void setEncoding(String encoding) {
		current_handler.setEncoding(encoding);
	}

	/**
	 * Start to monitor the encoding of the active document, if not started yet.
	 * @param window The IWorkbenchWindow to work on.
	 */
	public void start(IWorkbenchWindow window) {
		if (!is_started) {
			if (window != null) {
				is_started = true;
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
		if (is_started) {
			// Remove listeners.
			window.getPartService().removePartListener(this);

			// Reset the current handler to a dummy handler, which will remove IPropertyListener if added.
			setCurrentHandler(getHandler(null));

			window = null;
			is_started = false;
		}
	}
}
