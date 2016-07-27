package mergedoc.encoding;

public interface IActiveDocumentAgentCallback {

	/**
	 * Called by ActiveDocumentAgent when the encoding information of the active document is updated.
	 */
	public void statusChanged();
}
