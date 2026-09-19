LAB: sjv-l0-2 — Collections, generics and the error contract for Ledger

Built: Seed.java (500+ payments / 12 merchants incl. headline MR-4471 payment,
128450 minor units GBP, 2026-03-04), PaymentRepository (HashMap merchant index +
payment-id index, computeIfAbsent, Optional-returning lookups, TreeSet merchant
ids, generic Page<T>), LedgerException / MerchantNotFoundException (unchecked),
DuplicatePaymentException (checked), SettlementReport, GenericsProof.

Compile: `javac -Xlint:all` — clean, zero warnings.

Run output (SettlementReport):
  skipped duplicate: payment already stored with id: PAY-90312
seeded 501 payments across 12 merchants (1 duplicate rejected)
total payments stored: 501

merchant: MR-4471
  payment count : 50
  total minor   : 6561399

merchant: MR-0000
  MerchantNotFoundException: no merchant found with id: MR-0000
  (id available on the exception: MR-0000)

-- findPaymentById, corrected version --
PAY-90312 -> 128450 GBP on 2026-03-04
PAY-00000 -> not found

-- merchant ids, ordered --
[MR-1002, MR-1187, MR-2240, MR-2891, MR-3115, MR-3376, MR-4471, MR-5520,
 MR-6108, MR-7734, MR-8802, MR-9046]

Confirmed: unknown merchant produces MerchantNotFoundException naming the id,
not a NullPointerException.

GenericsProof (Page<T> with two unrelated row types):
Page<Payment>  : rows=5 totalRows=50 pageNumber=0
Page<Merchant> : rows=5 totalRows=12 pageNumber=0
(A commented-out line assigning a Payment row to a Merchant variable makes javac
reject the file with "incompatible types" — proof the generic is doing work.)

Cost table (costs.md):
Operation                       | Type                          | Cost
Lookup by merchant id           | HashMap<String,List<Payment>> | O(1) avg
Append a payment                | ArrayList (via computeIfAbsent)| O(1) amortised
Membership test on merchant id  | HashMap.containsKey            | O(1) avg
Ordered iteration by merchant id| TreeSet<String>                | O(n log n) build, O(n) walk
Access by position              | ArrayList.get(int)             | O(1)

Measured (240,000 payments / 12 merchants, JDK 25):
flat list, one scan per merchant : 87 ms  (total 60,025,153,463)
indexed map lookup per merchant  : 22 ms  (total 60,025,153,463)
totals agree: true

Justification: HashMap chosen over TreeMap because every access is exact-key
(no range queries); ordering, where needed, is built once in merchantIds() via
TreeSet rather than taxing every lookup. Would switch to TreeMap if range
queries (e.g. "merchants between MR-4000 and MR-5000") became a requirement.

AI-agent audit (findPaymentById from prompt "Write a findPaymentById method for
PaymentRepository that returns a single payment by its payment id"):
1. Returns null instead of Optional? YES (lines 10, 21, 24)
2. Catches Exception broadly?        YES (lines 22-25)
3. Uses raw List or Map?             YES (lines 8, 12, 13, 14, 15)
Corrected version: `Optional.ofNullable(byPaymentId.get(paymentId))` — O(1),
fully parameterised, no catch.

Documentation research (research.md):
- Optional API note: "Optional is primarily intended for use as a method
  return type" — decided Optional is never used as a field or parameter here.
- HashMap javadoc: "constant-time performance for the basic operations (get
  and put)" conditional on good hash dispersion — basis for the HashMap choice.

Files (all src/*.java, costs.md, research.md, agent-output.java) sent separately
as sjv-l0-2.zip.
