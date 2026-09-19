# costs.md

Lab: sjv-l0-2 — Collections, generics and the error contract for Ledger
Author: Henry Emefo

## Operation costs

| Operation | Structure | Java Type | Cost |
| --- | --- | --- | --- |
| Lookup by merchant id | Hash table, merchant id → that merchant's payments | `java.util.HashMap<String, List<Payment>>` | O(1) average, O(log n) worst case in a degenerate bucket (JDK treeifies long chains); n = number of merchants |
| Append a payment | Growable array at the end of the merchant's list, reached through `computeIfAbsent` | `java.util.ArrayList<Payment>` inside the `HashMap` | O(1) amortised — O(1) map lookup plus O(1) amortised `add`; the occasional array grow is O(k) and spread over k appends |
| Membership test on a merchant id | Same hash table, `containsKey` | `java.util.HashMap` (`containsKey`) | O(1) average |
| Ordered iteration by merchant id | Red-black tree over the key set, built in `merchantIds()` | `java.util.TreeSet<String>` | O(n log n) to build the set, then O(n) to walk it in id order; n = number of merchants |
| Access by position | Backing array of one merchant's payments, indexed directly | `java.util.ArrayList<Payment>` (`get(int)`) | O(1) |

Two notes on the table rather than inside it. First, the `paged` method combines rows two
and five: an O(1) map lookup, then an O(pageSize) slice, so paging never depends on how
many payments the merchant has. Second, `size()` is O(1) because it reads the secondary
`HashMap<String, Payment>` payment-id index rather than summing every list, which would
be O(n) in the number of payments.

## Measured, not just asserted

The flat-list design Ledger runs today is O(m × n): one scan of all 240,000 payments per
merchant. Indexing by merchant id makes each merchant's slice a single lookup. Measured
on JDK 25 with 240,000 payments across 12 merchants, single unwarmed run, so indicative
rather than a rigorous benchmark:

```
flat list, one scan per merchant : 87 ms  (total 60,025,153,463)
indexed map lookup per merchant  : 22 ms  (total 60,025,153,463)
totals agree: true
```

The totals match to the minor unit, so this is a pure cost change and not a behaviour
change. The gap is about 4× rather than the 12× the comparison count suggests, because
the indexed version still has to add up every payment once — the scan is what disappears,
not the summation. The gap widens with the merchant count: at 100 merchants the flat
version does eight times more scanning while the indexed version does exactly the same
work it does now.

## Justification of the merchant index choice

I chose `HashMap` over `TreeMap` for the merchant index because every access this
repository actually performs is an exact-key operation — `findByMerchant`, `save` through
`computeIfAbsent`, `hasMerchant`, `paged` — and `HashMap` serves those in O(1) average
while `TreeMap` charges O(log n) for all of them, which is the cost of an ordering
guarantee nothing here is asking for. The Java SE 25 `HashMap` documentation states that
"This implementation provides constant-time performance for the basic operations
(get and put)", conditional on the hash function spreading entries evenly across the
buckets — and merchant ids are high-entropy identifiers, so that condition holds. The one place ordering is genuinely needed — printing merchants in
id order — is served by building a `TreeSet` inside `merchantIds()`, paying O(n log n)
once at the point of need rather than taxing every lookup. What would change my mind:
range queries. If the report ever needed "every merchant id between MR-4000 and MR-5000",
or the first merchant after a cursor for keyset pagination, `TreeMap`'s `subMap`,
`headMap` and `ceilingKey` would turn an O(n) filter into an O(log n + k) traversal, and
I would switch and accept the slower point lookups. A second trigger would be needing a
concurrent map with a total ordering, since `ConcurrentSkipListMap` is the sorted option
there and the design would already be committed to ordered keys.