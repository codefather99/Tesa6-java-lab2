/**
 * Meera's settlement figures, for one known merchant and one unknown one.

 * The unknown merchant is the point of the exercise: the old code returned null and the
 * caller dereferenced it, producing a NullPointerException with no id in the message.
 * Here the repository throws MerchantNotFoundException, whose message names the id.
 */
public class SettlementReport {

    private static final String UNKNOWN_MERCHANT = "MR-0000";

    public static void main(String[] args) {
        PaymentRepository repository = Seed.populate();
        System.out.println("total payments stored: " + repository.size());
        System.out.println();

        report(repository, Seed.TARGET_MERCHANT);
        System.out.println();
        report(repository, UNKNOWN_MERCHANT);

        System.out.println();
        System.out.println("-- findPaymentById, corrected version --");
        System.out.println("PAY-90312 -> " + repository.findPaymentById("PAY-90312")
                .map(p -> p.amountMinor() + " " + p.currency() + " on " + p.receivedOn())
                .orElse("not found"));
        System.out.println("PAY-00000 -> " + repository.findPaymentById("PAY-00000")
                .map(Payment::id)
                .orElse("not found"));

        System.out.println();
        System.out.println("-- merchant ids, ordered --");
        System.out.println(repository.merchantIds());
    }

    /**
     * Prints the count and the minor-unit total for one merchant, handling the unknown
     * case where it belongs: at the boundary, once, with the id in the message.
     */
    private static void report(PaymentRepository repository, String merchantId) {
        System.out.println("merchant: " + merchantId);
        try {
            System.out.println("  payment count : " + repository.countFor(merchantId));
            System.out.println("  total minor   : " + repository.totalMinorFor(merchantId));
        } catch (MerchantNotFoundException e) {
            System.out.println("  MerchantNotFoundException: " + e.getMessage());
            System.out.println("  (id available on the exception: " + e.getMerchantId() + ")");
        }
    }
}