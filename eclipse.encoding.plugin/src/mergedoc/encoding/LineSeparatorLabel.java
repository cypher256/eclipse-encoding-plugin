package mergedoc.encoding;

import static mergedoc.encoding.Activator.*;
import static mergedoc.encoding.Langs.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.dialogs.PreferencesUtil;

import mergedoc.encoding.EncodingPreferenceInitializer.PreferenceKey;
import mergedoc.encoding.document.ActiveDocument;

/**
 * Line separetor label shown in the status bar.
 * @author Shinji Kashihara
 */
public class LineSeparatorLabel implements PreferenceKey {

	private final ActiveDocumentAgent agent;
	private final CLabel label;
	private Menu popupMenu;
	private List<SeparatorItem> separatorItemList;

	private static class SeparatorItem {
		public String value;
		public String desc;
		public SeparatorItem(String value, String desc) {
			this.value = value;
			this.desc = desc;
		}
	}

	public LineSeparatorLabel(ActiveDocumentAgent agent, Composite statusBar, int widthHint) {
		this.agent = agent;
		label = new CLabel(statusBar, SWT.LEFT);
		GridData gridData = new GridData();
		gridData.widthHint = widthHint;
		label.setLayoutData(gridData);
	}

	public void initMenu() {

		ActiveDocument doc = agent.getDocument();
		if (doc.getLineSeparator() == null) {
			label.setText(null);
			label.setMenu(null);
			label.setToolTipText(null);
			return;
		}
		label.setText(doc.getLineSeparator());

		if (separatorItemList == null) {
			separatorItemList = new ArrayList<SeparatorItem>();
			separatorItemList.add(new SeparatorItem("CRLF", "(\\r\\n, 0D0A, Windows)"));
			separatorItemList.add(new SeparatorItem("CR", "(\\r, 0D)"));
			separatorItemList.add(new SeparatorItem("LF", "(\\n, 0A, Unix)"));
		}

		if (doc.canConvertContent()) {
			label.setToolTipText(format("Right-click to convert the line ending of '%s'", doc.getFileName()));
		} else {
			label.setToolTipText(null);
		}
		if (popupMenu != null && !popupMenu.isDisposed()) {
			label.setMenu(popupMenu);
			return;
		}
		popupMenu = new Menu(label);
		label.setMenu(popupMenu);

		// Add the menu items dynamically.
		popupMenu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {

				ActiveDocument doc = agent.getDocument();
				doc.warnDirtyMessage(agent.isDocumentDirty() && doc.canConvertContent());

				// Remove existing menu items.
				for (MenuItem item: popupMenu.getItems()) item.dispose();

				createShortcutMenu();
				createSelectionMenu();
			}
		});
	}

	private void createShortcutMenu() {

		final ActiveDocument doc = agent.getDocument();

		// Workspace Preferences
		{
			MenuItem menuItem = new MenuItem(popupMenu, SWT.NONE);
			menuItem.setText(formatLabel("Workspace Preferences...", LineSeparators.ofWorkspace()));
			menuItem.setImage(Activator.getImage("workspace"));
			menuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPreferenceDialogOn(Display.getDefault().getActiveShell(),
						"org.eclipse.ui.preferencePages.Workspace", null, null).open();
				}
			});
		}

		// Project Properties
		{
			final IProject project = doc.getProject();
			String lineSeparator = null;
			if (project != null) {
				lineSeparator = LineSeparators.ofProject(project);
				if (lineSeparator == null) {
					lineSeparator = "Inheritance";
				}
			}
			MenuItem menuItem = new MenuItem(popupMenu, SWT.NONE);
			menuItem.setText(formatLabel("Project Properties...", lineSeparator));
			menuItem.setImage(Activator.getImage("project"));
			menuItem.setEnabled(project != null);
			menuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferencesUtil.createPropertyDialogOn(Display.getDefault().getActiveShell(),
						project,
						"org.eclipse.ui.propertypages.info.file", null, null).open();
				}
			});
		}
		new MenuItem(popupMenu, SWT.SEPARATOR);
	}

	private void createSelectionMenu() {

		final ActiveDocument doc = agent.getDocument();
		boolean nonDirty = !agent.isDocumentDirty() && doc.canConvertContent();

		for (final SeparatorItem separatorItem : separatorItemList) {

			final MenuItem menuItem = new MenuItem(popupMenu, SWT.RADIO);
			menuItem.setText(separatorItem.value + " " + separatorItem.desc);
			menuItem.setEnabled(nonDirty);
			// Allow change if detectedEncoding is null for english only
			if (prefIs(PREF_DISABLE_DISCOURAGED_OPERATION) && doc.mismatchesEncoding()) {
				menuItem.setEnabled(false);
			}
			if (separatorItem.value.equals(doc.getLineSeparator())) {
				menuItem.setSelection(true);
			}
			menuItem.setImage(Activator.getImage(separatorItem.value));

			menuItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					doc.setLineSeparator(separatorItem.value);
				}
			});
		}
	}
}
