/**
 * Thrown when a payment id that is already stored is saved again.

 * CHECKED (extends Exception, not LedgerException) because it is genuinely recoverable
 * and the recovery is a business decision the caller must make: a redelivered webhook
 * or a re-run import should skip the duplicate and carry on, while an internal caller
 * generating its own ids should treat it as a bug. The compiler forces that decision to
 * be made at the call site rather than defaulted.
 */
public class DuplicatePaymentException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String paymentId;

    public DuplicatePaymentException(String paymentId) {
        super("payment already stored with id: " + paymentId);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}