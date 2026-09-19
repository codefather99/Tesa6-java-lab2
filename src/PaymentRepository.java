import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * In-memory store of Ledger payments.
 *
 * The problem this solves: Ledger holds 240,000 payments in one flat list, and Meera's
 * Tuesday report scans that list once per merchant. With m merchants that is O(m * n) -
 * about 2.9 million comparisons for 12 merchants, and far worse as the merchant count
 * grows. Indexing by merchant id turns each merchant's slice into a single O(1) lookup,
 * so the whole report becomes O(n) once, spent building the index.
 */
public class PaymentRepository {

    /**
     * Primary index: merchant id -> that merchant's payments, in insertion order.
     * HashMap because the report looks merchants up by exact id and never asks for
     * "the next merchant after MR-4471". See costs.md for the full justification.
     */
    private final Map<String, List<Payment>> byMerchant = new HashMap<>();

    /**
     * Secondary index: payment id -> payment. Without it, findPaymentById has to walk
     * every list, which is the same O(n) scan this class exists to remove. It also makes
     * duplicate payment ids detectable at save time rather than at reconciliation time.
     */
    private final Map<String, Payment> byPaymentId = new HashMap<>();

    /**
     * Stores a payment under its merchant.
     *
     * @throws DuplicatePaymentException if this payment id is already stored - checked,
     *         because a redelivered webhook is a normal condition the caller can skip
     */
    public void save(Payment payment) throws DuplicatePaymentException {
        Objects.requireNonNull(payment, "payment must not be null");

        if (byPaymentId.containsKey(payment.id())) {
            throw new DuplicatePaymentException(payment.id());
        }

        // computeIfAbsent creates the list only on the first payment for that merchant,
        // and returns the existing list on every later call - one lookup, not two.
        byMerchant.computeIfAbsent(payment.merchantId(), id -> new ArrayList<>())
                .add(payment);
        byPaymentId.put(payment.id(), payment);
    }

    /** Total number of payments stored across all merchants. */
    public int size() {
        return byPaymentId.size();
    }

    /**
     * Returns this merchant's payments, or an empty Optional if the merchant is unknown.
     * Never returns null - that is the whole point of the signature.
     *
     * The returned list is unmodifiable, so a caller cannot reach in and mutate the
     * repository's own storage through it.
     */
    public Optional<List<Payment>> findByMerchant(String merchantId) {
        List<Payment> payments = byMerchant.get(merchantId);
        return payments == null
                ? Optional.empty()
                : Optional.of(Collections.unmodifiableList(payments));
    }

    /**
     * Returns the payment with this id, or an empty Optional if there is none.
     *
     * This is the corrected version of the AI agent's findPaymentById - see
     * agent-output.java and the defect table in research.md. Three changes: it returns
     * Optional instead of null, it catches nothing (there is nothing here that throws a
     * recoverable exception, so a broad catch could only hide a bug), and every type is
     * fully parameterised, so no unchecked cast is possible.
     */
    public Optional<Payment> findPaymentById(String paymentId) {
        return Optional.ofNullable(byPaymentId.get(paymentId));
    }

    /**
     * Every merchant id that has at least one payment.
     *
     * TreeSet, not HashSet: the settlement report prints merchants in id order, and this
     * is the only place that ordering is needed. A TreeSet gives it for free at O(log n)
     * per insert, versus a HashSet plus a separate O(n log n) sort at every call site.
     * The set is wrapped unmodifiable so callers cannot add a merchant id that has no
     * payments behind it. If ordering stopped mattering, HashSet would be the right
     * choice - the O(1) contains would beat TreeSet's O(log n).
     */
    public Set<String> merchantIds() {
        return Collections.unmodifiableSet(new TreeSet<>(byMerchant.keySet()));
    }

    /** True if this merchant has any payments. O(1) on the HashMap. */
    public boolean hasMerchant(String merchantId) {
        return byMerchant.containsKey(merchantId);
    }

    /**
     * One page of a merchant's payments.
     *
     * @param pageNumber zero-based
     * @throws MerchantNotFoundException if the merchant id is unknown - unchecked,
     *         because asking to page a merchant that does not exist is a caller bug
     * @throws IllegalArgumentException if pageSize is not positive or pageNumber is negative
     */
    public Page<Payment> paged(String merchantId, int pageNumber, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive, was: " + pageSize);
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative, was: " + pageNumber);
        }

        List<Payment> all = findByMerchant(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));

        int from = Math.min(pageNumber * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());

        return new Page<>(List.copyOf(all.subList(from, to)), all.size(), pageNumber);
    }

    /**
     * Number of payments for a merchant.
     *
     * @throws MerchantNotFoundException if the merchant id is unknown
     */
    public int countFor(String merchantId) {
        return findByMerchant(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId))
                .size();
    }

    /**
     * Total of a merchant's payments in minor units. long arithmetic throughout.
     *
     * @throws MerchantNotFoundException if the merchant id is unknown
     */
    public long totalMinorFor(String merchantId) {
        return findByMerchant(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId))
                .stream()
                .mapToLong(Payment::amountMinor)
                .sum();
    }
}