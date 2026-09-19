import java.util.List;

/**
 * Proof that Page<T> is genuinely generic: the same record type carries Payment rows and
 * Merchant rows, with no cast, no raw type and no second implementation. If Page were not
 * generic, one of these two lines would not compile.
 */
public class GenericsProof {

    public static void main(String[] args) {
        PaymentRepository repository = Seed.populate();

        Page<Payment> paymentPage = repository.paged(Seed.TARGET_MERCHANT, 0, 5);

        List<Merchant> merchants = Seed.merchants();
        Page<Merchant> merchantPage = new Page<>(merchants.subList(0, 5), merchants.size(), 0);

        System.out.println("Page<Payment>  : rows=" + paymentPage.rows().size()
                + " totalRows=" + paymentPage.totalRows()
                + " pageNumber=" + paymentPage.pageNumber());
        System.out.println("Page<Merchant> : rows=" + merchantPage.rows().size()
                + " totalRows=" + merchantPage.totalRows()
                + " pageNumber=" + merchantPage.pageNumber());

        // The compiler knows the row types without any cast on our part:
        Payment firstPayment = paymentPage.rows().get(0);
        Merchant firstMerchant = merchantPage.rows().get(0);
        System.out.println("first payment  : " + firstPayment.id()
                + " " + firstPayment.amountMinor() + " " + firstPayment.currency());
        System.out.println("first merchant : " + firstMerchant.id()
                + " " + firstMerchant.displayName());

        // The line below is commented out deliberately. Uncomment it and javac rejects it
        // with "incompatible types: Payment cannot be converted to Merchant" - that
        // compile-time rejection IS the proof the generic parameter is doing work.
        // Merchant wrong = paymentPage.rows().get(0);
    }
}