import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a deterministic test set: 12 merchants and 500+ payments.

 * Deterministic on purpose - a fixed Random seed means the printed report is identical on
 * every run and on the marker's machine, so the numbers in research.md can be checked.
 */
public class Seed {

    public static final String TARGET_MERCHANT = "MR-4471";

    /** The payment named in the brief: 128450 minor units GBP, dated 2026-03-04. */
    public static final Payment HEADLINE_PAYMENT = new Payment(
            "PAY-90312",
            TARGET_MERCHANT,
            128_450L,
            "GBP",
            PaymentStatus.RECEIVED,
            LocalDate.of(2026, 3, 4));

    private static final String[] MERCHANT_IDS = {
            "MR-1002", "MR-1187", "MR-2240", "MR-2891", "MR-3115", "MR-3376",
            "MR-4471", "MR-5520", "MR-6108", "MR-7734", "MR-8802", "MR-9046"
    };

    private static final String[] CURRENCIES = { "GBP", "USD", "EUR", "NGN" };

    /** The 12 merchants, as domain objects. Used to prove Page<Merchant> compiles. */
    public static List<Merchant> merchants() {
        List<Merchant> merchants = new ArrayList<>();
        for (int i = 0; i < MERCHANT_IDS.length; i++) {
            merchants.add(new Merchant(
                    MERCHANT_IDS[i],
                    "Merchant " + (i + 1),
                    "GB29-NWBK-6016-13-" + (31900 + i)));
        }
        return merchants;
    }

    /**
     * Fills a repository with 500 payments spread across the 12 merchants, plus the
     * headline MR-4471 payment.
     */
    public static PaymentRepository populate() {
        PaymentRepository repository = new PaymentRepository();
        Random random = new Random(4471L);           // fixed seed: reproducible run
        LocalDate start = LocalDate.of(2026, 1, 1);
        PaymentStatus[] statuses = PaymentStatus.values();

        int saved = 0;
        for (int i = 0; i < 500; i++) {
            Payment payment = new Payment(
                    String.format("PAY-%05d", 10_000 + i),
                    MERCHANT_IDS[random.nextInt(MERCHANT_IDS.length)],
                    1_000L + random.nextInt(250_000),
                    CURRENCIES[random.nextInt(CURRENCIES.length)],
                    statuses[random.nextInt(statuses.length)],
                    start.plusDays(random.nextInt(120)));
            saved += trySave(repository, payment);
        }

        saved += trySave(repository, HEADLINE_PAYMENT);

        // Deliberate second attempt at the same id, to show the checked exception doing
        // its job: the duplicate is skipped and seeding continues.
        int duplicates = 1 - trySave(repository, HEADLINE_PAYMENT);

        System.out.println("seeded " + saved + " payments across "
                + repository.merchantIds().size() + " merchants ("
                + duplicates + " duplicate rejected)");
        return repository;
    }

    /**
     * Saves one payment, recovering from the checked DuplicatePaymentException the way a
     * redelivered webhook should be handled: log it, skip it, keep going.
     */
    private static int trySave(PaymentRepository repository, Payment payment) {
        try {
            repository.save(payment);
            return 1;
        } catch (DuplicatePaymentException e) {
            System.out.println("  skipped duplicate: " + e.getMessage());
            return 0;
        }
    }
}