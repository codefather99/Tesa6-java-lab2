/**
 * A business that Ledger settles money to.

 * NOTE ON A CHANGE FROM LAB sjv-l0-1: in that lab Merchant was deliberately a class,
 * because bankAccountRef is mutable state on an entity with a continuous identity.
 * This lab's brief calls it "the Merchant record", so it is a record here. The trade
 * is explicit: updateBankAccountRef is gone, replaced by withBankAccountRef, which
 * returns a new Merchant instead of mutating one. That is a defensible model - it makes
 * the merchant index safe to share across threads and safe to use as a Map key - but it
 * means a bank change must be written back into whatever holds the merchants, rather
 * than being applied in place.
 */
public record Merchant(String id, String displayName, String bankAccountRef) {

    public Merchant {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be null or blank");
        }
        if (bankAccountRef == null || bankAccountRef.isBlank()) {
            throw new IllegalArgumentException("bankAccountRef must not be null or blank");
        }
    }

    /** Returns a copy of this merchant pointing at a different payout account. */
    public Merchant withBankAccountRef(String newBankAccountRef) {
        return new Merchant(id, displayName, newBankAccountRef);
    }
}