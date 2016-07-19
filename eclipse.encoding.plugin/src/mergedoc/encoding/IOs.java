package mergedoc.encoding;

import java.io.Closeable;
import java.io.IOException;

public class IOs {

	private IOs() {
	}

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
