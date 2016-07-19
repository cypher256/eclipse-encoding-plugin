package mergedoc.encoding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
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

import mergedoc.encoding.EncodingPreferenceInitializer.PreferenceKey;

/**
 * Show the file encoding information for the active document.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class EncodingControlContribution extends
		WorkbenchWindowControlContribution implements IActiveDocumentAgentCallback, PreferenceKey {

	// The agent is responsible for monitoring the encoding information of the active document.
	private ActiveDocumentAgent agent = new ActiveDocumentAgent(this);
	private Composite status_bar;

	private CLabel file_encoding_label;
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

	public EncodingControlContribution() {
	}
	public EncodingControlContribution(String id) {
		super(id);
	}

	private IPreferenceStore pref() {
		return Activator.getDefault().getPreferenceStore();
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
		{
			file_encoding_label = new CLabel(status_bar, SWT.LEFT);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
			gridData.widthHint = 100;
			file_encoding_label.setLayoutData(gridData);
		}
		{
			Label separator = new Label(status_bar, SWT.SEPARATOR | SWT.VERTICAL);
			separator.setLayoutData(new GridData(GridData.FILL_BOTH));
		}
		{
			line_ending_label = new Label(status_bar, SWT.LEFT);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
			gridData.widthHint = 40;
			line_ending_label.setLayoutData(gridData);
		}
		
		fillControl();
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

	private void fillControl() {

		ActiveDocumentHandler handler = agent.getHandler();
		if (
			!agent.isDocumentDirty() &&
			handler.canChangeFileEncoding() &&
			handler.isMismatchEncoding() &&
			pref().getBoolean(PREF_AUTODETECT_CHANGE)
		) {
			String detectedEncoding = handler.getDetectedEncoding();
			IFile file = handler.getFile();
			if (file == null) {
				// Non workspace file, mismatch workspace preferences
				String workspaceEncoding = ResourcesPlugin.getEncoding();
				if (!EncodingUtil.areCharsetsEqual(detectedEncoding, workspaceEncoding)) {
					handler.setEncoding(detectedEncoding);
					handler.infoMessage("Encoding has been set %s automatically.", detectedEncoding);
					return;
				}
			}
			else {
				// Workspace file, file properties null
				try {
					String fileEncoding = file.getCharset(false);
					if (fileEncoding == null) {
						handler.setEncoding(detectedEncoding);
						handler.infoMessage("Encoding has been set %s automatically.", detectedEncoding);
						return;
					}
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		
		createFileEncodingMenu();
		createFileEndingMenu();
	}
	
	@SuppressWarnings("unchecked")
	private void createFileEncodingMenu() {
		
		ActiveDocumentHandler handler = agent.getHandler();
		current_file_encoding = handler.getCurrentEncoding();
		file_encoding_label.setText(current_file_encoding == null ? "" : current_file_encoding);
		
		if (handler.isMismatchEncoding() && pref().getBoolean(PREF_AUTODETECT_WARN)) {
			file_encoding_label.setImage(Activator.getImage("warn"));
		} else {
			file_encoding_label.setImage(null);
		}
		if (current_file_encoding == null) {
			file_encoding_label.setMenu(null);
			file_encoding_label.setToolTipText(null);
			return;
		}

		file_encoding_list = IDEEncoding.getIDEEncodings();
		addFileEncodingItem(current_file_encoding);
		addFileEncodingItem(handler.getInheritedEncoding());
		addFileEncodingItem(handler.getDetectedEncoding());

		if (handler.canChangeFileEncoding()) {
			file_encoding_label.setToolTipText(
				String.format("Right-click to change the encoding of '%s'", handler.getFileName()));
		} else {
			file_encoding_label.setToolTipText(null);
		}
		if (file_encoding_popup_menu != null && !file_encoding_popup_menu.isDisposed()) {
			file_encoding_label.setMenu(file_encoding_popup_menu);
			return;
		}
		file_encoding_popup_menu = new Menu(file_encoding_label);
		file_encoding_label.setMenu(file_encoding_popup_menu);
		
		// Add the menu items dynamically
		file_encoding_popup_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				final ActiveDocumentHandler handler = agent.getHandler();

				// Remove existing menu items.
				for (MenuItem item: file_encoding_popup_menu.getItems()) item.dispose();

				// Do not allow changing encoding when the document is dirty.
				boolean enabledAction = !agent.isDocumentDirty() && handler.canChangeFileEncoding();
				handler.warnSaveMessage(!enabledAction);
				
				createPreferenceMenu();
				createShortcutMenu();

				// Create encoding menu meta data
				final List<FileEncodingItem> encodingList = new ArrayList<FileEncodingItem>();
				for (final String encoding : file_encoding_list) {

					FileEncodingItem i = new FileEncodingItem();
					encodingList.add(i);
					i.encoding = encoding;
					i.isInheritance = EncodingUtil.areCharsetsEqual(encoding, handler.getInheritedEncoding());
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
				MenuItem charsetParentItem = new MenuItem(file_encoding_popup_menu, SWT.CASCADE);
				charsetParentItem.setText(String.format("Convert Charset %s to", current_file_encoding));
				charsetParentItem.setImage(Activator.getImage("convert_charset"));
				charsetParentItem.setEnabled(enabledAction);
				if (handler.isMismatchEncoding() && pref().getBoolean(PREF_DISABLE_CONVERSION_MENU)) {
					charsetParentItem.setEnabled(false);
				}
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
							handler.convertCharset(i.encoding);
						}
					});
				}

				// Create change encoding property menu items.
				MenuItem encodingParentItem = new MenuItem(file_encoding_popup_menu, SWT.CASCADE);
				encodingParentItem.setText("Change Encoding to");
				encodingParentItem.setEnabled(enabledAction);
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
								handler.setEncoding(i.encoding);
							}
						}
					});
				}

				// Create change encoding for autodetect
				final MenuItem detectItem = new MenuItem(file_encoding_popup_menu, SWT.NONE);
				detectItem.setImage(Activator.getImage("autodetect"));

				final String detectedEncoding = handler.getDetectedEncoding();
				if (detectedEncoding == null) {
					detectItem.setText("Change Encoding (Cannot Autodetect)");
					detectItem.setEnabled(false);
				}
				else if (EncodingUtil.areCharsetsEqual(detectedEncoding, current_file_encoding)) {
					detectItem.setText("Change Encoding (Matches Autodetect)");
					detectItem.setEnabled(false);
				}
				else {
					detectItem.setText(String.format("Change Encoding to %s (Autodetect)", detectedEncoding));
					detectItem.setEnabled(enabledAction);
					detectItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							for (FileEncodingItem i : encodingList) {
								if (EncodingUtil.areCharsetsEqual(i.encoding, detectedEncoding)) {
									handler.setEncoding(i.encoding);
									break;
								}
							}
						}
					});
				}
			}
		});
	}
	
	private void createFileEndingMenu() {
		
		ActiveDocumentHandler handler = agent.getHandler();
		current_line_ending = handler.getLineEnding();
		line_ending_label.setText(current_line_ending == null ? "" : current_line_ending);
		if (current_line_ending == null) {
			line_ending_label.setMenu(null);
			line_ending_label.setToolTipText(null);
			return;
		}

		if (line_ending_list == null) {
			line_ending_list = new ArrayList<LineEndingItem>();
			line_ending_list.add(new LineEndingItem("CRLF", "(\\r\\n, 0D0A, Windows)"));
			line_ending_list.add(new LineEndingItem("CR", "(\\r, 0D)"));
			line_ending_list.add(new LineEndingItem("LF", "(\\n, 0A, Unix)"));
		}

		if (handler.canConvertLineEnding()) {
			line_ending_label.setToolTipText(
				String.format("Right-click to convert the line ending of '%s'", handler.getFileName()));
		} else {
			line_ending_label.setToolTipText(null);
		}
		if (line_ending_popup_menu != null && !line_ending_popup_menu.isDisposed()) {
			line_ending_label.setMenu(line_ending_popup_menu);
			return;
		}
		line_ending_popup_menu = new Menu(line_ending_label);
		line_ending_label.setMenu(line_ending_popup_menu);

		// Add the menu items dynamically.
		line_ending_popup_menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				ActiveDocumentHandler handler = agent.getHandler();

				// Remove existing menu items.
				for (MenuItem item: line_ending_popup_menu.getItems()) item.dispose();

				// Do not allow changing encoding when the document is dirty.
				boolean enabledAction = !agent.isDocumentDirty() && handler.canConvertLineEnding();
				handler.warnSaveMessage(!enabledAction);

				// Add menu items.
				for (final LineEndingItem lineEndingItem : line_ending_list) {

					final MenuItem item = new MenuItem(line_ending_popup_menu, SWT.RADIO);
					item.setText(lineEndingItem.value + " " + lineEndingItem.desc);
					item.setEnabled(enabledAction);
					if (handler.isMismatchEncoding() && pref().getBoolean(PREF_DISABLE_CONVERSION_MENU)) {
						item.setEnabled(false);
					}
					if (lineEndingItem.value.equals(current_line_ending)) {
						item.setSelection(true);
					}
					item.setImage(Activator.getImage(lineEndingItem.value));

					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (item.getSelection()) {
								ActiveDocumentHandler handler = agent.getHandler();
								handler.setLineEnding(lineEndingItem.value);
							}
						}
					});
				}
			}
		});
	}
	
	private void createPreferenceMenu() {
		
		createCheckMenuItem(PREF_AUTODETECT_CHANGE, "Autodetect: Set Automatically");
		createCheckMenuItem(PREF_AUTODETECT_WARN, "Autodetect: Show Warning");
		createCheckMenuItem(PREF_DISABLE_CONVERSION_MENU, "Autodetect: Disable Conversion Menu");
		new MenuItem(file_encoding_popup_menu, SWT.SEPARATOR);
	}
	
	private void createCheckMenuItem(final String prefKey, String message) {
		
		final MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.CHECK);
		item.setText(message);
		item.setSelection(pref().getBoolean(prefKey));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean sel = !pref().getBoolean(prefKey);
				item.setSelection(sel);
				pref().setValue(prefKey, sel);
			}
		});
	}
	
	private void createShortcutMenu() {

		ActiveDocumentHandler handler = agent.getHandler();

		// Menu for Open Workspace Preferences
		{
			MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
			item.setText("Workspace Preferences..." + getEncodingLabel(ResourcesPlugin.getEncoding()));
			item.setImage(Activator.getImage("workspace"));
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
						"org.eclipse.ui.preferencePages.Workspace", null, null).open();
				}
			});
		}

		// Menu for Open Project Properties
		{
			final IProject project = handler.getProject();
			String encoding = null;
			if (project != null) {
				try {
					encoding = project.getDefaultCharset(false);
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
				if (encoding == null) {
					encoding = "Inheritance";
				}
			}
			MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
			item.setText("Project Properties..." + getEncodingLabel(encoding));
			item.setImage(Activator.getImage("project"));
			item.setEnabled(project != null);
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(),
						project,
						"org.eclipse.ui.propertypages.info.file", null, null).open();
				}
			});
		}

		// Menu for Open src/jar Package Root Properties
		final PackageRoot packageRoot = handler.getPackageRoot();
		if (packageRoot != null && packageRoot.element != null) {
			String encoding = packageRoot.encoding;
			if (encoding == null) {
				encoding = "Inheritance";
			}
			MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
			item.setText("Package Root Properties..." + getEncodingLabel(encoding));
			item.setImage(Activator.getImage("root"));
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(),
						packageRoot.element,
						"org.eclipse.jdt.ui.propertyPages.SourceAttachmentPage", null, null).open();
				}
			});
		}

		// Menu for Open File Properties
		{
			final IFile file = handler.getFile();
			String encoding = null;
			if (file != null) {
				try {
					encoding = file.getCharset(false);
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
				if (encoding == null) {
					encoding = "Inheritance";
				}
			}
			MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
			item.setText("File Properties..." + getEncodingLabel(encoding));
			item.setImage(Activator.getImage("file"));
			item.setEnabled(file != null);
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(),
						file,
						"org.eclipse.ui.propertypages.info.file", null, null).open();
				}
			});
		}

		// Menu for Open Content Type Preferences
		{
			String encoding = handler.getContentTypeEncoding();
			if (encoding == null) {
				encoding = "Not Set";
			}
			MenuItem item = new MenuItem(file_encoding_popup_menu, SWT.NONE);
			item.setText("Content Types Preferences..." + getEncodingLabel(encoding));
			item.setImage(Activator.getImage("content"));
			item.setEnabled(handler.enabledContentTypeEnding());
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
						"org.eclipse.ui.preferencePages.ContentTypes", null, null).open();
				}
			});
		}
		new MenuItem(file_encoding_popup_menu, SWT.SEPARATOR);
	}

	private String getEncodingLabel(String encoding) {
		if (encoding != null) {
			return " (" + encoding + ")";
		}
		return "";
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
					fillControl();
				}
			}
		});
	}
}
