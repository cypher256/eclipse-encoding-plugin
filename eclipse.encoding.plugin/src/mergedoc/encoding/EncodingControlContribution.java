package mergedoc.encoding;

import static mergedoc.encoding.Activator.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import mergedoc.encoding.EncodingPreferenceInitializer.PreferenceKey;
import mergedoc.encoding.document.ActiveDocument;

/**
 * Show the file encoding information for the active document.
 * @author Tsoi Yat Shing
 * @author Shinji Kashihara
 */
public class EncodingControlContribution extends
		WorkbenchWindowControlContribution implements IActiveDocumentAgentCallback, PreferenceKey {

	// The agent is responsible for monitoring the encoding information of the active document.
	private final ActiveDocumentAgent agent = new ActiveDocumentAgent(this);
	private Composite statusBar;
	private EncodingLabel encodingLabel;
	private LineSeparatorLabel lineSeparatorLabel;

	public EncodingControlContribution() {
	}

	public EncodingControlContribution(String id) {
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

		encodingLabel = new EncodingLabel(agent, statusBar, 100);
		Label separator = new Label(statusBar, SWT.SEPARATOR | SWT.VERTICAL);
		separator.setLayoutData(new GridData(GridData.FILL_BOTH));
		lineSeparatorLabel = new LineSeparatorLabel(agent, statusBar, 40);

		fillControl();
		return statusBar;
	}

	private void fillControl() {

		ActiveDocument doc = agent.getDocument();

		// Autodetect: Set Automatically
		if (
			!agent.isDocumentDirty() &&
			doc.canChangeFileEncoding() &&
			doc.mismatchesEncoding() &&
			prefIs(PREF_AUTODETECT_CHANGE)
		) {
			String message = "Encoding has been set %s automatically.";
			String detectedEncoding = doc.getDetectedCharset();
			IFile file = doc.getFile();
			if (file == null) {
				// Non workspace file, mismatch workspace preferences
				String workspaceEncoding = ResourcesPlugin.getEncoding();
				if (!Charsets.equals(detectedEncoding, workspaceEncoding)) {
					doc.setEncoding(detectedEncoding);
					doc.infoMessage(message, detectedEncoding);
					return;
				}
			}
			else {
				// Workspace file, if file properties null
				if (doc.getFilePropertiesEncoding() == null) {
					doc.setEncoding(detectedEncoding);
					doc.infoMessage(message, detectedEncoding);
					return;
				}
			}
		}

		encodingLabel.initMenu();
		lineSeparatorLabel.initMenu();
	}

	@Override
	public void dispose() {
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
	@Override
	public void encodingChanged() {

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
