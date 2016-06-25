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
import org.eclipse.ui.dialogs.PreferencesUtil;
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

	private static class FileEncodingItem {
		public String encoding;
		public boolean isInheritance;
		public boolean isAutodetect;
		public boolean isContentType;
		public String menuText;
	}

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
							item.setText("Please save the document first.");
						}

						MenuItem prefParentItem = new MenuItem(file_encoding_popup_menu, SWT.CASCADE);
						prefParentItem.setText("Preferences");
						prefParentItem.setImage(Activator.getImage("preference"));
						Menu prefMenu = new Menu(prefParentItem);
						prefParentItem.setMenu(prefMenu);

						// Menu for Open Woekspace Preferences
						{
							MenuItem item = new MenuItem(prefMenu, SWT.NONE);
							item.setText("Workspace Preferences...");
							item.setImage(Activator.getImage("workspace"));
							item.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
										"org.eclipse.ui.preferencePages.Workspace", null, null).open();
								}
							});
						}

						if (agent.getHandler() instanceof WorkspaceTextFileHandler) {
							// Menu for Open Project Properties
							{
								MenuItem item = new MenuItem(prefMenu, SWT.NONE);
								item.setText("Project Properties...");
								item.setImage(Activator.getImage("project"));
								item.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(),
											((WorkspaceTextFileHandler) agent.getHandler()).getFile().getProject(),
											"org.eclipse.ui.propertypages.info.file", null, null).open();
									}
								});
							}
							// Menu for Open File Properties
							{
								MenuItem item = new MenuItem(prefMenu, SWT.NONE);
								item.setText("File Properties...");
								item.setImage(Activator.getImage("file"));
								item.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(),
											((WorkspaceTextFileHandler) agent.getHandler()).getFile(),
											"org.eclipse.ui.propertypages.info.file", null, null).open();
									}
								});
							}
						}

						// Menu for Open Content Type Preferences
						{
							MenuItem item = new MenuItem(prefMenu, SWT.NONE);
							item.setText("Content Types Preferences...");
							item.setImage(Activator.getImage("content"));
							item.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
										"org.eclipse.ui.preferencePages.ContentTypes", null, null).open();
								}
							});
						}

						// Create encoding menu meta data
						IActiveDocumentAgentHandler handler = agent.getHandler();
						List<FileEncodingItem> encodingList = new ArrayList<FileEncodingItem>();
						for (final String encoding : file_encoding_list) {

							FileEncodingItem i = new FileEncodingItem();
							encodingList.add(i);
							i.encoding = encoding;
							i.isInheritance = EncodingUtil.areCharsetsEqual(encoding, handler.getContainerEncoding());
							i.isAutodetect  = EncodingUtil.areCharsetsEqual(encoding, handler.getDetectedEncoding());
							i.isContentType = EncodingUtil.areCharsetsEqual(encoding, handler.getContentTypeEncoding());

							StringBuilder sb = new StringBuilder();
							if (i.isAutodetect) {
								sb.append(String.format("Autodetect")).append(", ");
							}
							if (i.isInheritance) {
								sb.append(String.format("Inheritance")).append(", ");
							}
							if (i.isContentType) {
								sb.append(String.format("Content Type"));
							}
							i.menuText = i.encoding;
							if (sb.length() > 0) {
								i.menuText += " (" + sb.toString().replaceFirst(", $", "") + ")";
							}
						}

						// Create convert charset menu items.
						if (handler.isContentWriteable()) {

							MenuItem charsetParentItem = new MenuItem(file_encoding_popup_menu, SWT.CASCADE);
							charsetParentItem.setText(String.format("Convert Charset %s to", current_file_encoding));
							charsetParentItem.setEnabled(!is_document_dirty);
							charsetParentItem.setImage(Activator.getImage("convert_charset"));
							Menu convertMenu = new Menu(charsetParentItem);
							charsetParentItem.setMenu(convertMenu);

							for (final FileEncodingItem i : encodingList) {

								if (EncodingUtil.areCharsetsEqual(i.encoding, current_file_encoding)) {
									continue;
								}
								MenuItem item = new MenuItem(convertMenu, SWT.NONE);
								item.setText(i.menuText);
								item.setImage(EncodingUtil.getImage(i.encoding));

								item.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent e) {
										IActiveDocumentAgentHandler handler = agent.getHandler();
										handler.convertCharset(i.encoding);
									}
								});
							}
						}

						// Create change encoding property menu items.
						MenuItem encodingParentItem = new MenuItem(file_encoding_popup_menu, SWT.CASCADE);
						encodingParentItem.setText("Change Encoding Setting");
						encodingParentItem.setEnabled(!is_document_dirty);
						encodingParentItem.setImage(Activator.getImage("change_encoding"));
						Menu encodingMenu = new Menu(encodingParentItem);
						encodingParentItem.setMenu(encodingMenu);

						for (final FileEncodingItem i : encodingList) {

							final MenuItem item = new MenuItem(encodingMenu, SWT.RADIO);
							item.setText(i.menuText);
							item.setImage(EncodingUtil.getImage(i.encoding));
							if (EncodingUtil.areCharsetsEqual(i.encoding, current_file_encoding)) {
								item.setSelection(true);
							}

							item.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent e) {
									if (item.getSelection()) {
										IActiveDocumentAgentHandler handler = agent.getHandler();
										if (i.isContentType || (i.isInheritance && handler.getContentTypeEncoding() == null)) {
											handler.setEncoding(null); // Inheritance
										} else {
											handler.setEncoding(i.encoding);
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

		if (handler.isContentWriteable() && current_line_ending != null) {

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
							item.setText("Please save the document first.");
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
