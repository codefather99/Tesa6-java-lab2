/**
 * Base type for every Ledger failure that signals a programming or data error the
 * caller cannot sensibly recover from at the call site.
 *
 * Unchecked (extends RuntimeException) on purpose: these conditions mean the request is
 * wrong, not that the system is momentarily unavailable. Forcing every caller in the
 * settlement pipeline to declare or catch them would produce exactly the empty catch
 * blocks this lab is trying to prevent.
 */
public class LedgerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LedgerException(String message) {
        super(message);
    }

    public LedgerException(String message, Throwable cause) {
        super(message, cause);
    }
}