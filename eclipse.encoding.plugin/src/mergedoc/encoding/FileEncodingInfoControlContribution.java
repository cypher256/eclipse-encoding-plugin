package mergedoc.encoding;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ide.IDEEncoding;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

/**
 * Show the file encoding information for the active document.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class FileEncodingInfoControlContribution extends
		WorkbenchWindowControlContribution implements IActiveDocumentAgentCallback {

	// The agent is responsible for monitoring the encoding information of the active document.
	private ActiveDocumentAgent agent = new ActiveDocumentAgent(this);

	private Composite statusBar;
	private Label file_encoding_label;
	private Menu file_encoding_popup_menu;
	private String current_file_encoding;

	private Label line_ending_label;
	private List<String> encodingList;

	public FileEncodingInfoControlContribution() {
	}

	public FileEncodingInfoControlContribution(String id) {
		super(id);
	}

	/**
	 * This method will be called each time to update the label, as resize cannot be made to work.
	 */
	@Override
	protected Control createControl(Composite parent) {

		agent.start(getWorkbenchWindow());
		statusBar = new Composite(parent, SWT.NONE);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 0;
		statusBar.setLayout(gridLayout);

		file_encoding_label = new Label(statusBar, SWT.LEFT);
		GridData encodingGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		encodingGridData.widthHint = 100;
		file_encoding_label.setLayoutData(encodingGridData);

		Label separator = new Label(statusBar, SWT.SEPARATOR | SWT.VERTICAL);
		separator.setLayoutData(new GridData(GridData.FILL_BOTH));

		line_ending_label = new Label(statusBar, SWT.LEFT);
		GridData breakGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		breakGridData.widthHint = 40;
		line_ending_label.setLayoutData(breakGridData);

		fillComp();
		return statusBar;
	}

	private void addEncodingList(String encoding) {
		if (encoding != null) {
			for (String e : encodingList) {
				if (EncodingUtil.areCharsetsEqual(e, encoding)) {
					return;
				}
			}
			encodingList.add(encoding);
			Collections.sort(encodingList);
		}
	}

	@SuppressWarnings("unchecked")
	private void fillComp() {

		// Get the encoding information of the active document.
		IActiveDocumentAgentHandler handler = agent.getHandler();
		current_file_encoding = handler.getEncoding();

		file_encoding_label.setText(current_file_encoding == null ? "" : current_file_encoding);
		String lineBreak = handler.getLineEnding();
		line_ending_label.setText(lineBreak == null ? "" : lineBreak);

		if (handler.enableChangeEncoding()) {

			encodingList = IDEEncoding.getIDEEncodings();
			addEncodingList(current_file_encoding);
			addEncodingList(handler.getContainerEncoding());
			addEncodingList(handler.getDetectedEncoding());

			boolean isListenerAdded = true;
			if (file_encoding_popup_menu == null) {
				file_encoding_popup_menu = new Menu(file_encoding_label);
				isListenerAdded = false;
			}
			file_encoding_label.setMenu(file_encoding_popup_menu);
			file_encoding_label.setToolTipText(String.format("Right-click to change the encoding of '%s'", handler.getName()));
			if (!isListenerAdded) {

				// Add the menu items dynamically.
				file_encoding_popup_menu.addMenuListener(new MenuAdapter() {
					@Override
					public void menuShown(MenuEvent e) {

						// Remove existing menu items.
						for (MenuItem item: file_encoding_popup_menu.getItems()) item.dispose();
						// Do not allow changing encoding when the document is dirty.
						boolean is_document_dirty = FileEncodingInfoControlContribution.this.agent.isDocumentDirty();
						if (is_document_dirty) {
							MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
							item.setText(String.format("Please save the document first."));
						}
						IActiveDocumentAgentHandler handler = agent.getHandler();

						// Add menu items.
						for (final String encoding : encodingList) {

							final MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.RADIO);
							final boolean isContainerEncoding = encoding.equals(handler.getContainerEncoding());
							boolean isDetected = EncodingUtil.areCharsetsEqual(encoding, handler.getDetectedEncoding());

							if (isDetected && isContainerEncoding) {
								item.setText(String.format("%s (Autodetect, Inheritance)", encoding));
							} else if (isDetected) {
								item.setText(String.format("%s (Autodetect)", encoding));
							} else if (isContainerEncoding) {
								item.setText(String.format("%s (Inheritance)", encoding));
							} else {
								item.setText(encoding);
							}

							item.setEnabled(!is_document_dirty);
							if (EncodingUtil.areCharsetsEqual(encoding, current_file_encoding)) {
								item.setSelection(true);
							}
							item.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									if (item.getSelection()) {
										IActiveDocumentAgentHandler handler = agent.getHandler();
										// Set the charset.
										if (isContainerEncoding) {
											handler.setEncoding(null);
										} else {
											handler.setEncoding(encoding);
										}
									}
								}
							});
						}
					}
				});
			}
		} else {
			file_encoding_label.setMenu(null);
			file_encoding_label.setToolTipText(null);
		}
	}

	@Override
	public void dispose() {
		// Stop the agent.
		agent.stop();

		super.dispose();
	}

	@Override
	public boolean isDynamic() {
		// Call createControl() on update.
		return true;
	}

	/**
	 * Update the encoding information in the label.
	 * Like after the user switches to another editor.
	 */
	public void encodingInfoChanged() {
		// Cannot make resize work, need to call createControl() again.
		// Do update in the UI thread.
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				IContributionManager manager = getParent();
				if (manager != null) {
					manager.update(true);
				} else if (!statusBar.isDisposed()) {
					fillComp();
				}
			}
		});
	}
}
