public  enum PaymentStatus {
    // Money has arrived from the payer and is held by Ledger, but nothing has
    // been paid out to the merchant yet. Fees are calculated against it, but
    // it does not appear on a merchant payout.
    RECEIVED,

    // Fees have been deducted and the net amount has been paid out to the
    // merchant's bank account reference. This is a terminal state.
    SETTLED,

    // The payment did not complete: it was declined, reversed or abandoned.
    // No fee is charged and no payout is owed. This is a terminal state.
    FAILED



}
