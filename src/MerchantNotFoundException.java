/**
 * Thrown when a merchant id does not exist in the repository.

 * This replaces the old behavior of returning null, which callers kept forgetting to
 * check. The offending id is kept as a field as well as being in the message, so a
 * handler can act on it without parsing text.
 */
public class MerchantNotFoundException extends LedgerException {

    private static final long serialVersionUID = 1L;

    private final String merchantId;

    public MerchantNotFoundException(String merchantId) {
        super("no merchant found with id: " + merchantId);
        this.merchantId = merchantId;
    }

    public String getMerchantId() {
        return merchantId;
    }
}