package mergedoc.encoding;

import static java.lang.String.*;

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
	private Composite statusBar;

	private CLabel encodingLabel;
	private Menu encodingPopupMenu;
	private List<String> encodingList;

	private Label lineSeparatorLabel;
	private Menu lineSeparatorPopupMenu;
	private List<LineSeparatorItem> lineSeparatorItemList;

	private static class EncodingItem {
		public String encoding;
		public boolean isInheritance;
		public boolean isAutodetect;
		public boolean isContentType;
		public String menuText;
	}

	private static class LineSeparatorItem {
		public String value;
		public String desc;
		public LineSeparatorItem(String value, String desc) {
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
		statusBar = new Composite(parent, SWT.NONE);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		gridLayout.marginHeight = 0;
		statusBar.setLayout(gridLayout);
		{
			encodingLabel = new CLabel(statusBar, SWT.LEFT);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
			gridData.widthHint = 100;
			encodingLabel.setLayoutData(gridData);
		}
		{
			Label separator = new Label(statusBar, SWT.SEPARATOR | SWT.VERTICAL);
			separator.setLayoutData(new GridData(GridData.FILL_BOTH));
		}
		{
			lineSeparatorLabel = new Label(statusBar, SWT.LEFT);
			GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
			gridData.widthHint = 40;
			lineSeparatorLabel.setLayoutData(gridData);
		}
		
		fillControl();
		return statusBar;
	}

	private void fillControl() {

		ActiveDocument doc = agent.getDocument();
		if (
			!agent.isDocumentDirty() &&
			doc.canChangeFileEncoding() &&
			doc.mismatchesEncoding() &&
			pref().getBoolean(PREF_AUTODETECT_CHANGE)
		) {
			String message = "Encoding has been set %s automatically.";
			String detectedEncoding = doc.getDetectedEncoding();
			IFile file = doc.getFile();
			if (file == null) {
				// Non workspace file, mismatch workspace preferences
				String workspaceEncoding = ResourcesPlugin.getEncoding();
				if (!Encodings.areCharsetsEqual(detectedEncoding, workspaceEncoding)) {
					doc.setEncoding(detectedEncoding);
					doc.infoMessage(message, detectedEncoding);
					return;
				}
			}
			else {
				// Workspace file, file properties null
				try {
					String fileEncoding = file.getCharset(false);
					if (fileEncoding == null) {
						doc.setEncoding(detectedEncoding);
						doc.infoMessage(message, detectedEncoding);
						return;
					}
				} catch (CoreException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		
		createEncodingMenu();
		createLineSeparatorMenu();
	}

	private void addEncodingItem(String encoding) {
		if (encoding != null) {
			for (String e : encodingList) {
				if (Encodings.areCharsetsEqual(e, encoding)) {
					return;
				}
			}
			encodingList.add(encoding);
			Collections.sort(encodingList);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void createEncodingMenu() {
		
		ActiveDocument doc = agent.getDocument();
		if (doc.getCurrentEncoding() == null) {
			encodingLabel.setText(null);
			encodingLabel.setToolTipText(null);
			encodingLabel.setImage(null);
			encodingLabel.setMenu(null);
			return;
		}
		encodingLabel.setText(doc.getCurrentEncoding());
		
		if (doc.mismatchesEncoding() && pref().getBoolean(PREF_AUTODETECT_WARN)) {
			String message = "Detected charset %s (Current setting: %s)";
			doc.warnMessage(message, doc.getDetectedEncoding(), doc.getCurrentEncoding());
			encodingLabel.setToolTipText(format(message, doc.getDetectedEncoding(), doc.getCurrentEncoding()));
			encodingLabel.setImage(Activator.getImage("warn"));
		} else {
			doc.warnMessage(null);
			encodingLabel.setImage(null);
			if (doc.canChangeFileEncoding()) {
				encodingLabel.setToolTipText(format("Right-click to change the encoding of '%s'", doc.getFileName()));
			} else {
				encodingLabel.setToolTipText(null);
			}
		}

		encodingList = IDEEncoding.getIDEEncodings();
		addEncodingItem(doc.getCurrentEncoding());
		addEncodingItem(doc.getInheritedEncoding());
		addEncodingItem(doc.getDetectedEncoding());

		if (encodingPopupMenu != null && !encodingPopupMenu.isDisposed()) {
			encodingLabel.setMenu(encodingPopupMenu);
			return;
		}
		encodingPopupMenu = new Menu(encodingLabel);
		encodingLabel.setMenu(encodingPopupMenu);
		
		// Add the menu items dynamically
		encodingPopupMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				final ActiveDocument doc = agent.getDocument();

				// Remove existing menu items.
				for (MenuItem item: encodingPopupMenu.getItems()) item.dispose();

				// Do not allow changing encoding when the document is dirty.
				warnSaveMessage(agent.isDocumentDirty() && doc.canChangeFileEncoding());
				boolean enabledAction = !agent.isDocumentDirty() && doc.canChangeFileEncoding();
				
				createPreferenceMenu();
				createShortcutMenu();

				// Create encoding menu meta data
				final List<EncodingItem> encodingItemList = new ArrayList<EncodingItem>();
				for (final String encoding : encodingList) {

					EncodingItem i = new EncodingItem();
					encodingItemList.add(i);
					i.encoding = encoding;
					i.isInheritance = Encodings.areCharsetsEqual(encoding, doc.getInheritedEncoding());
					i.isAutodetect  = Encodings.areCharsetsEqual(encoding, doc.getDetectedEncoding());
					i.isContentType = Encodings.areCharsetsEqual(encoding, doc.getContentTypeEncoding());

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

				// Convert Charset
				MenuItem charsetParentItem = new MenuItem(encodingPopupMenu, SWT.CASCADE);
				charsetParentItem.setText(String.format("Convert Charset %s to", doc.getCurrentEncoding()));
				charsetParentItem.setImage(Activator.getImage("convert_charset"));
				charsetParentItem.setEnabled(enabledAction);
				if (pref().getBoolean(PREF_DISABLE_DANGER_OPERATION) &&
						(doc.getDetectedEncoding() == null || doc.mismatchesEncoding())) {
					charsetParentItem.setEnabled(false);
				}
				Menu convertMenu = new Menu(charsetParentItem);
				charsetParentItem.setMenu(convertMenu);

				for (final EncodingItem i : encodingItemList) {

					if (Encodings.areCharsetsEqual(i.encoding, doc.getCurrentEncoding())) {
						continue;
					}
					MenuItem item = new MenuItem(convertMenu, SWT.NONE);
					item.setText(i.menuText);
					item.setImage(Encodings.getImage(i.encoding));

					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							doc.convertCharset(i.encoding);
						}
					});
				}

				// Change Encoding
				MenuItem encodingParentItem = new MenuItem(encodingPopupMenu, SWT.CASCADE);
				encodingParentItem.setText("Change Encoding to");
				encodingParentItem.setEnabled(enabledAction);
				if (pref().getBoolean(PREF_DISABLE_DANGER_OPERATION) && doc.matchesEncoding()) {
					encodingParentItem.setEnabled(false);
				}
				encodingParentItem.setImage(Activator.getImage("change_encoding"));
				Menu encodingMenu = new Menu(encodingParentItem);
				encodingParentItem.setMenu(encodingMenu);

				for (final EncodingItem i : encodingItemList) {

					final MenuItem item = new MenuItem(encodingMenu, SWT.RADIO);
					item.setText(i.menuText);
					item.setImage(Encodings.getImage(i.encoding));
					if (Encodings.areCharsetsEqual(i.encoding, doc.getCurrentEncoding())) {
						item.setSelection(true);
					}
					// Converted to one line and freeze on big file
					else if (pref().getBoolean(PREF_DISABLE_DANGER_OPERATION) && i.encoding.startsWith("UTF-16")) {
						item.setEnabled(false);
					}
					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (item.getSelection()) {
								doc.setEncoding(i.encoding);
							}
						}
					});
				}

				// Create change encoding for autodetect
				final MenuItem detectItem = new MenuItem(encodingPopupMenu, SWT.NONE);
				detectItem.setImage(Activator.getImage("autodetect"));
				
				if (doc.getDetectedEncoding() == null) {
					detectItem.setText("Change Encoding (Cannot Autodetect)");
					detectItem.setEnabled(false);
				}
				else if (doc.matchesEncoding()) {
					detectItem.setText("Change Encoding (Matches Autodetect)");
					detectItem.setEnabled(false);
				}
				else {
					detectItem.setText(String.format("Change Encoding to %s (Autodetect)", doc.getDetectedEncoding()));
					detectItem.setEnabled(enabledAction);
					detectItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							for (EncodingItem i : encodingItemList) {
								if (Encodings.areCharsetsEqual(i.encoding, doc.getDetectedEncoding())) {
									doc.setEncoding(i.encoding);
									break;
								}
							}
						}
					});
				}
			}
		});
	}

	private void warnSaveMessage(boolean showsWarn) {
		ActiveDocument doc = agent.getDocument();
		if (showsWarn) {
			doc.warnMessage("Editor must be saved before status bar action.");
		} else {
			doc.warnMessage(null);
		}
	}
	
	private void createLineSeparatorMenu() {
		
		ActiveDocument doc = agent.getDocument();
		if (doc.getLineSeparator() == null) {
			lineSeparatorLabel.setText(null);
			lineSeparatorLabel.setMenu(null);
			lineSeparatorLabel.setToolTipText(null);
			return;
		}
		lineSeparatorLabel.setText(doc.getLineSeparator());

		if (lineSeparatorItemList == null) {
			lineSeparatorItemList = new ArrayList<LineSeparatorItem>();
			lineSeparatorItemList.add(new LineSeparatorItem("CRLF", "(\\r\\n, 0D0A, Windows)"));
			lineSeparatorItemList.add(new LineSeparatorItem("CR", "(\\r, 0D)"));
			lineSeparatorItemList.add(new LineSeparatorItem("LF", "(\\n, 0A, Unix)"));
		}

		if (doc.canConvertLineEnding()) {
			lineSeparatorLabel.setToolTipText(
				String.format("Right-click to convert the line ending of '%s'", doc.getFileName()));
		} else {
			lineSeparatorLabel.setToolTipText(null);
		}
		if (lineSeparatorPopupMenu != null && !lineSeparatorPopupMenu.isDisposed()) {
			lineSeparatorLabel.setMenu(lineSeparatorPopupMenu);
			return;
		}
		lineSeparatorPopupMenu = new Menu(lineSeparatorLabel);
		lineSeparatorLabel.setMenu(lineSeparatorPopupMenu);

		// Add the menu items dynamically.
		lineSeparatorPopupMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				ActiveDocument doc = agent.getDocument();

				// Remove existing menu items.
				for (MenuItem item: lineSeparatorPopupMenu.getItems()) item.dispose();

				// Do not allow changing encoding when the document is dirty.
				warnSaveMessage(agent.isDocumentDirty() && doc.canConvertLineEnding());
				boolean enabledAction = !agent.isDocumentDirty() && doc.canConvertLineEnding();

				// Add menu items.
				for (final LineSeparatorItem lineEndingItem : lineSeparatorItemList) {

					final MenuItem item = new MenuItem(lineSeparatorPopupMenu, SWT.RADIO);
					item.setText(lineEndingItem.value + " " + lineEndingItem.desc);
					item.setEnabled(enabledAction);
					// Allow change if detectedEncoding is null for english only
					if (pref().getBoolean(PREF_DISABLE_DANGER_OPERATION) && doc.mismatchesEncoding()) {
						item.setEnabled(false);
					}
					if (lineEndingItem.value.equals(doc.getLineSeparator())) {
						item.setSelection(true);
					}
					item.setImage(Activator.getImage(lineEndingItem.value));

					item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (item.getSelection()) {
								ActiveDocument doc = agent.getDocument();
								doc.setLineSeparator(lineEndingItem.value);
							}
						}
					});
				}
			}
		});
	}
	
	private void createPreferenceMenu() {
		
		createToggleMenuItem(PREF_AUTODETECT_CHANGE, "Autodetect: Set Automatically")
			.setToolTipText("This only applies when the file properties encoding is not specified");
		
		createToggleMenuItem(PREF_AUTODETECT_WARN, "Autodetect: Show Warning");
		createToggleMenuItem(PREF_DISABLE_DANGER_OPERATION, "Autodetect: Disable Dangerous Operations");
		new MenuItem(encodingPopupMenu, SWT.SEPARATOR);
	}
	
	private MenuItem createToggleMenuItem(final String prefKey, String message) {
		
		final MenuItem item = new MenuItem(encodingPopupMenu, SWT.CHECK);
		item.setText(message);
		item.setSelection(pref().getBoolean(prefKey));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean sel = !pref().getBoolean(prefKey);
				item.setSelection(sel);
				pref().setValue(prefKey, sel);
				encodingInfoChanged();
			}
		});
		return item;
	}
	
	private void createShortcutMenu() {

		ActiveDocument doc = agent.getDocument();

		// Menu for Open Workspace Preferences
		{
			MenuItem item = new MenuItem(encodingPopupMenu, SWT.NONE);
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
			final IProject project = doc.getProject();
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
			MenuItem item = new MenuItem(encodingPopupMenu, SWT.NONE);
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
		final PackageRoot packageRoot = doc.getPackageRoot();
		if (packageRoot != null && packageRoot.element != null) {
			String encoding = packageRoot.encoding;
			if (encoding == null) {
				encoding = "Inheritance";
			}
			MenuItem item = new MenuItem(encodingPopupMenu, SWT.NONE);
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
			final IFile file = doc.getFile();
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
			MenuItem item = new MenuItem(encodingPopupMenu, SWT.NONE);
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
			String encoding = doc.getContentTypeEncoding();
			if (encoding == null) {
				encoding = "Not Set";
			}
			MenuItem item = new MenuItem(encodingPopupMenu, SWT.NONE);
			item.setText("Content Types Preferences..." + getEncodingLabel(encoding));
			item.setImage(Activator.getImage("content"));
			item.setEnabled(doc.enabledContentTypeEnding());
			item.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
						"org.eclipse.ui.preferencePages.ContentTypes", null, null).open();
				}
			});
		}
		new MenuItem(encodingPopupMenu, SWT.SEPARATOR);
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
				} else if (!statusBar.isDisposed()) {
					fillControl();
				}
			}
		});
	}
}
