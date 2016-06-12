package mergedoc.encoding;

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

	private Composite comp;
	private Label file_encoding_label;
	private Menu file_encoding_popup_menu;
	private String current_file_encoding;

	// ADD S.Kashihara
	private Label line_ending_label;

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
		// Start the agent, if needed.
		agent.start(getWorkbenchWindow());

		// Use StackLayout to stack labels.
		comp = new Composite(parent, SWT.NONE);

		// ADD S.Kashihara BEGIN
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 0;
		comp.setLayout(gridLayout);

		file_encoding_label = new Label(comp, SWT.LEFT);
		GridData encodingGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		encodingGridData.widthHint = 100;
		file_encoding_label.setLayoutData(encodingGridData);

		Label fill = new Label(comp, SWT.SEPARATOR | SWT.VERTICAL);
		fill.setLayoutData(new GridData(GridData.FILL_BOTH));

		line_ending_label = new Label(comp, SWT.LEFT);
		GridData breakGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		breakGridData.widthHint = 40;
		line_ending_label.setLayoutData(breakGridData);
		line_ending_label.setText("xxxx");
		// ADD S.Kashihara END

		fillComp();

		return comp;
	}

	private void fillComp() {
		// Get the encoding information of the active document.
		current_file_encoding = agent.getEncoding();

		file_encoding_label.setText(current_file_encoding == null ? "" : current_file_encoding);
		String lineBreak = agent.getLineEnding();
		line_ending_label.setText(lineBreak == null ? "" : lineBreak);

		if (agent.enableChangeEncoding()) {

			@SuppressWarnings("unchecked")
			final List<String> encodingList = IDEEncoding.getIDEEncodings();

			boolean isListenerAdded = true;
			if (file_encoding_popup_menu == null) {
				file_encoding_popup_menu = new Menu(file_encoding_label);
				isListenerAdded = false;
			}
			file_encoding_label.setMenu(file_encoding_popup_menu);
			file_encoding_label.setToolTipText(String.format("Right-click to change the encoding of '%s'", agent.getName()));
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
							item.setText("Please save the document first.");
						}
						// Add menu items, the charset with the highest confidence is in the bottom.
						for (final String encoding : encodingList) {
							final boolean isContainerEncoding = encoding.equals(agent.getContainerEncoding());
							final MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.RADIO);
							if (isContainerEncoding) {
								item.setText(String.format("%s (inherited from container)", encoding));
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
										// Set the charset.
										ActiveDocumentAgent agent = FileEncodingInfoControlContribution.this.agent;
										if (isContainerEncoding) {
											agent.setEncoding(null);
										} else {
											agent.setEncoding(encoding);
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
				} else if (!comp.isDisposed()) {
					fillComp();
				}
			}
		});
	}
}
