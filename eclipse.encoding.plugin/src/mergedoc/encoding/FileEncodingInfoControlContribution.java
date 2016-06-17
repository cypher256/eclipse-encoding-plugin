package mergedoc.encoding;

import java.util.ArrayList;
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
	private Composite status_bar;

	private Label file_encoding_label;
	private Menu file_encoding_popup_menu;
	private List<String> file_encoding_list;
	private String current_file_encoding;

	private Label line_ending_label;
	private Menu line_ending_popup_menu;
	private List<LineEndingItem> line_ending_list;
	private String current_line_ending;

	private static class LineEndingItem {
		public String value;
		public String desc;
		public LineEndingItem(String value, String desc) {
			super();
			this.value = value;
			this.desc = desc;
		}
	}

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
		status_bar = new Composite(parent, SWT.NONE);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 0;
		status_bar.setLayout(gridLayout);

		file_encoding_label = new Label(status_bar, SWT.LEFT);
		GridData encodingGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		encodingGridData.widthHint = 100;
		file_encoding_label.setLayoutData(encodingGridData);

		Label separator = new Label(status_bar, SWT.SEPARATOR | SWT.VERTICAL);
		separator.setLayoutData(new GridData(GridData.FILL_BOTH));

		line_ending_label = new Label(status_bar, SWT.LEFT);
		GridData breakGridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		breakGridData.widthHint = 40;
		line_ending_label.setLayoutData(breakGridData);

		fillComp();
		return status_bar;
	}

	private void addFileEncodingItem(String encoding) {
		if (encoding != null) {
			for (String e : file_encoding_list) {
				if (EncodingUtil.areCharsetsEqual(e, encoding)) {
					return;
				}
			}
			file_encoding_list.add(encoding);
			Collections.sort(file_encoding_list);
		}
	}

	@SuppressWarnings("unchecked")
	private void fillComp() {

		IActiveDocumentAgentHandler handler = agent.getHandler();

		current_file_encoding = handler.getEncoding();
		file_encoding_label.setText(current_file_encoding == null ? "" : current_file_encoding);

		if (handler.isFileEncodingChangeable()) {

			file_encoding_list = IDEEncoding.getIDEEncodings();
			addFileEncodingItem(current_file_encoding);
			addFileEncodingItem(handler.getContainerEncoding());
			addFileEncodingItem(handler.getDetectedEncoding());

			boolean isFileEncodingMenuAdded = true;
			if (file_encoding_popup_menu == null) {
				file_encoding_popup_menu = new Menu(file_encoding_label);
				isFileEncodingMenuAdded = false;
			}
			file_encoding_label.setMenu(file_encoding_popup_menu);
			file_encoding_label.setToolTipText(String.format("Right-click to change the encoding of '%s'", handler.getName()));

			if (!isFileEncodingMenuAdded) {

				// Add the menu items dynamically.
				file_encoding_popup_menu.addMenuListener(new MenuAdapter() {
					@Override
					public void menuShown(MenuEvent e) {

						// Remove existing menu items.
						for (MenuItem item: file_encoding_popup_menu.getItems()) item.dispose();
						// Do not allow changing encoding when the document is dirty.
						boolean is_document_dirty = agent.isDocumentDirty();
						if (is_document_dirty) {
							MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
							item.setText(String.format("Please save the document first."));
						}
						IActiveDocumentAgentHandler handler = agent.getHandler();

						// Create convert menu items.
						if (handler.isContentChangeable()) {

							MenuItem convertParentItem = new MenuItem(file_encoding_popup_menu, SWT.CASCADE);
							convertParentItem.setText("Convert Charset to");
							convertParentItem.setEnabled(!is_document_dirty);
							convertParentItem.setImage(Activator.getImage("convert"));

							Menu convertMenu = new Menu(convertParentItem);
							convertParentItem.setMenu(convertMenu);

							for (final String encoding : file_encoding_list) {
								if (EncodingUtil.areCharsetsEqual(encoding, current_file_encoding)) {
									continue;
								}
								final MenuItem item = new MenuItem(convertMenu, SWT.NONE);
								item.setText(encoding);
								item.setImage(EncodingUtil.getCountryImage(encoding));

								item.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										IActiveDocumentAgentHandler handler = agent.getHandler();
										handler.convertCharset(encoding);
									}
								});
							}
							new MenuItem(file_encoding_popup_menu, SWT.SEPARATOR);
						}

						// Create encoding menu items.
						for (final String encoding : file_encoding_list) {

							final MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.RADIO);
							final boolean isContainerEncoding = encoding.equals(handler.getContainerEncoding());
							boolean isDetectedEncoding = EncodingUtil.areCharsetsEqual(encoding, handler.getDetectedEncoding());
							final boolean isContentTypeEncoding = encoding.equals(handler.getContentTypeEncoding());

							StringBuilder sb = new StringBuilder();
							if (isDetectedEncoding) {
								sb.append(String.format("Autodetect")).append(", ");
							}
							if (isContainerEncoding) {
								sb.append(String.format("Inheritance")).append(", ");
							}
							if (isContentTypeEncoding) {
								sb.append(String.format("Content Type"));
							}
							String menuText = encoding;
							if (sb.length() > 0) {
								menuText += " (" + sb.toString().replaceFirst(", $", "") + ")";
							}
							item.setText(menuText);

							item.setEnabled(!is_document_dirty);
							if (EncodingUtil.areCharsetsEqual(encoding, current_file_encoding)) {
								item.setSelection(true);
							}
							item.setImage(EncodingUtil.getCountryImage(encoding));

							item.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									if (item.getSelection()) {
										IActiveDocumentAgentHandler handler = agent.getHandler();

										// Set the charset.
										if (isContentTypeEncoding || (isContainerEncoding && handler.getContentTypeEncoding() == null)) {
											handler.setEncoding(null); // Inheritance
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

		current_line_ending = handler.getLineEnding();
		line_ending_label.setText(current_line_ending == null ? "" : current_line_ending);

		if (handler.isContentChangeable()) {

			if (line_ending_list == null) {
				line_ending_list = new ArrayList<LineEndingItem>();
				line_ending_list.add(new LineEndingItem("CRLF", "(\\r\\n, 0D0A, Windows)"));
				line_ending_list.add(new LineEndingItem("CR", "(\\r, 0D)"));
				line_ending_list.add(new LineEndingItem("LF", "(\\n, 0A, Unix)"));
			}

			boolean isLineEndingMenuAdded = true;
			if (line_ending_popup_menu == null) {
				line_ending_popup_menu = new Menu(line_ending_label);
				isLineEndingMenuAdded = false;
			}
			line_ending_label.setMenu(line_ending_popup_menu);
			line_ending_label.setToolTipText(String.format("Right-click to convert the line ending of '%s'", handler.getName()));

			if (!isLineEndingMenuAdded) {

				// Add the menu items dynamically.
				line_ending_popup_menu.addMenuListener(new MenuAdapter() {
					@Override
					public void menuShown(MenuEvent e) {

						// Remove existing menu items.
						for (MenuItem item: line_ending_popup_menu.getItems()) item.dispose();
						// Do not allow changing encoding when the document is dirty.
						boolean is_document_dirty = agent.isDocumentDirty();
						if (is_document_dirty) {
							MenuItem item = new MenuItem(line_ending_popup_menu, SWT.NONE);
							item.setText(String.format("Please save the document first."));
						}

						// Add menu items.
						for (final LineEndingItem lineEndingItem : line_ending_list) {

							final MenuItem item = new MenuItem(line_ending_popup_menu, SWT.RADIO);
							item.setText(lineEndingItem.value + " " + lineEndingItem.desc);
							item.setEnabled(!is_document_dirty);
							if (lineEndingItem.value.equals(current_line_ending)) {
								item.setSelection(true);
							}
							item.setImage(Activator.getImage(lineEndingItem.value));

							item.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									if (item.getSelection()) {
										IActiveDocumentAgentHandler handler = agent.getHandler();
										handler.setLineEnding(lineEndingItem.value);
									}
								}
							});
						}
					}
				});
			}

		} else {
			line_ending_label.setMenu(null);
			line_ending_label.setToolTipText(null);
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
				} else if (!status_bar.isDisposed()) {
					fillComp();
				}
			}
		});
	}
}
