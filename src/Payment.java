import java.time.LocalDate;

/**
 * A single payment taken by Ledger on behalf of a merchant.

 * Carried forward from lab sjv-l0-1 with ONE addition: the component
 * LocalDate receivedOn. This lab requires a seeded payment "dated 2026-03-04", and a
 * settlement report that runs weekly has no meaning without a date on the payment.
 * LocalDate, not a String and not a long epoch value, so the date cannot be
 * mis-parsed and cannot be compared against an amount by accident.

 * amountMinor stays a long count of minor units. Never a double.
 */

public record Payment(String id, String merchantId, long amountMinor, String currency, PaymentStatus status, LocalDate receivedOn) {


    public Payment {

        if (id == null || id.isBlank()) {
             throw new IllegalArgumentException("Id must not be null or blank");
        }

        if (merchantId == null) {
            throw new IllegalArgumentException("MerchantId must not be null");
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Money value must not be 0 or negative ");
        }

        if (currency== null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency format name must not be longer than 3");
        }

        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (receivedOn == null) {
            throw new IllegalArgumentException("receivedOn must not be null");
        }
    }


}
