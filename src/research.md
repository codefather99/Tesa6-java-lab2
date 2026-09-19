# research.md

Lab: sjv-l0-2 — Collections, generics and the error contract for Ledger
Author: Henry Emefo
Toolchain: javac 25.0.1 java 25.0.1 2025-10-21 LTS Java(TM) SE Runtime Environment (build 25.0.1+8-LTS-27) 
Java HotSpot(TM) 64-Bit Server VM (build 25.0.1+8-LTS-27, mixed mode, sharing)

---

## 1. Primary documentation: may Optional be used as a field?

**Page title: "Optional (Java SE 25 & JDK 25)"**
(`java.util.Optional`, https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Optional.html)

The sentence that decided it, from the class-level API Note:

> "Optional is primarily intended for use as a method return type"

The note goes on to give the reason — a return type is where there is a clear need to
represent "no result" and where returning `null` is likely to cause errors — and adds that
a variable of type `Optional` should never itself be `null`.

That settled the design. `Optional` appears in this codebase **only** in return positions:
`findByMerchant` returns `Optional<List<Payment>>` and `findPaymentById` returns
`Optional<Payment>`. It is never a field, because a field of type `Optional` can itself be
`null`, which reintroduces the exact null check the type was adopted to remove, and it is
never a parameter, because a caller then has to wrap a value it already holds. Where a
field genuinely might be absent, an empty collection or a dedicated state does the job.

A second page confirms the collection side of the design — **"HashMap (Java SE 25 & JDK
25)"** states that the implementation gives constant-time performance for the basic get
and put operations, conditional on the hash function dispersing entries evenly across the
buckets. That conditional is quoted and applied in costs.md.

---

## 2. Checked versus unchecked: which is which, and why

Ledger now has two exception families and they are split on one question: can the caller
do something about it at the call site?

`LedgerException` is **unchecked** (it extends `RuntimeException`), and
`MerchantNotFoundException` extends it. Asking the repository for a merchant id that does
not exist is not a transient condition the caller can retry or work around — it means the
id came from a bad request, a stale report definition or a typo, and the honest response
is to fail loudly with the id in the message. Making it checked would force every method
in the settlement pipeline either to declare `throws MerchantNotFoundException` all the
way up, or — far more likely in practice — to wrap the call in a `catch` that logs and
carries on, which is exactly the swallowed-error pattern that let the original null return
go unnoticed. Unchecked lets the failure travel to the one boundary that can turn it into
an HTTP 404 or a failed report row, and lets every layer in between stay silent about it.

`DuplicatePaymentException` is **checked** (it extends `Exception` directly, deliberately
not `LedgerException`, so it cannot be caught by a handler reaching for the unchecked
family). Saving a payment id that is already stored is a genuinely recoverable, genuinely
expected condition: payment gateways redeliver webhooks, and an interrupted import gets
re-run. The right action is a business decision the compiler should force the caller to
make — an ingest path skips the duplicate and continues, while an internal caller
generating its own ids should treat it as a defect. `Seed.trySave` shows the first
handling: it catches, logs the id and keeps seeding, and the run output below shows the
duplicate being rejected without stopping anything.

Checked was the right call for that one and only that one. The rule I applied: checked for
conditions a reasonable caller has a plan for, unchecked for conditions that mean the
request itself is wrong.

---

## 3. AI agent verification — `findPaymentById`

**Provenance.** Agent: Claude (Anthropic), via the Claude chat interface, 18 September
2026. Exact prompt: "Write a findPaymentById method for PaymentRepository that returns a
      single payment by its payment id." No constraints were given, which is the point — the
      defects below are what an unguided agent produces. The output is saved verbatim in
      `agent-output.java`; nothing was added, removed or reformatted, including the class name
      it chose and the fact that it does not compile against this repository.

| # | Defect checked | Answer | Line number(s) | Detail |
| --- | --- | --- | --- | --- |
| 1 | Does it return `null` instead of `Optional`? | **Yes** | 10, 21, 24 | Return type is `Payment` (10); returns `null` on the not-found path (21) and again from the catch block (24). This is precisely the behaviour the lab brief describes — callers keep forgetting to check — and the second `null` is worse, because it makes a failure indistinguishable from a legitimate miss |
| 2 | Does it catch `Exception` broadly? | **Yes** | 22–25 | `catch (Exception e)` wraps the entire method. It would swallow the `NullPointerException` from the uninitialised `byMerchant` field (8), and any `ClassCastException` from the casts on 13 and 15, print a stack trace to stdout, and hand the caller a `null` as though the payment simply did not exist |
| 3 | Does it use a raw `List` or `Map`? | **Yes** | 8, 12, 13, 14, 15 | `private Map byMerchant` is raw (8); `List payments` is raw (13); consequently the loop variables are `Object` (12, 14) and the code needs two unchecked casts, `(List)` on 13 and `(Payment)` on 15. Raw types also disable generic type checking on the whole expression, so a `Map<String, List<Merchant>>` would compile here and fail at runtime |

**Three for three.** A fourth problem is worth naming even though the brief did not ask:
the method is O(n) in the total number of payments, walking every merchant's list on every
call. That is the same full scan this lab exists to eliminate.

**The corrected version** is in `PaymentRepository.findPaymentById`:

```java
public Optional<Payment> findPaymentById(String paymentId) {
    return Optional.ofNullable(byPaymentId.get(paymentId));
}
```

Four changes in two lines. It returns `Optional<Payment>`, so a caller cannot forget the
absent case. It catches nothing, because nothing here throws a recoverable exception and a
broad catch could only hide a bug. Every type is fully parameterised, so there is no cast
and no unchecked warning under `-Xlint:all`. And it reads a dedicated payment-id index, so
it is O(1) instead of O(n).

---

## 4. Run output

`javac -Xlint:all -d out src/*.java` compiles clean — no warnings, no unchecked
operations.

### SettlementReport

```
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
[MR-1002, MR-1187, MR-2240, MR-2891, MR-3115, MR-3376, MR-4471, MR-5520, MR-6108, MR-7734, MR-8802, MR-9046]
```

**Confirmed:** the unknown merchant produces `MerchantNotFoundException` with the id in
the message, not a `NullPointerException`. The id is also available as a field on the
exception, so a handler can act on it without parsing the message text. The headline
payment — `PAY-90312`, `MR-4471`, 128450 minor units GBP, dated 2026-03-04 — is present
and retrievable by id.

### GenericsProof — `Page<T>` carries two unrelated row types

```
Page<Payment>  : rows=5 totalRows=50 pageNumber=0
Page<Merchant> : rows=5 totalRows=12 pageNumber=0
first payment  : PAY-10010 189034 EUR
first merchant : MR-1002 Merchant 1
```

Both instantiations compile and run with no cast and no raw type. `GenericsProof` also
carries a commented-out line assigning a `Payment` row to a `Merchant` variable;
uncommenting it makes `javac` reject the file with *incompatible types: Payment cannot be
converted to Merchant*, which is the real proof that the type parameter is doing work
rather than being decorative.

---

## 5. Two carried-forward changes worth flagging to the marker

1. **`Payment` gained a `LocalDate receivedOn` component.** The brief requires a seeded
   payment "dated 2026-03-04", and the lab-1 record had no date. `LocalDate` rather than a
   `String` or an epoch `long`, so it cannot be mis-parsed or accidentally compared
   against an amount.
2. **`Merchant` is a record here, and was a class in lab 1.** This brief calls it "the
   Merchant record". The trade is documented at the top of `Merchant.java`:
   `updateBankAccountRef` is replaced by `withBankAccountRef`, which returns a new
   instance. That makes merchants safe to share and safe to use as map keys, at the cost
   of a bank change having to be written back rather than applied in place.