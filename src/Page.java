import java.util.List;

/**
 * One page of any row type.

 * Generic because paging is a property of the transport, not of the payment domain:
 * the report pages payments, the admin screen pages merchants, and neither should need
 * its own copy of this type. T is unbounded on purpose - Page makes no demand on its
 * rows beyond holding them.

 * totalRows is the size of the whole result set, not of this page, so a caller can work
 * out the last page number without a second round trip.
 */
public record Page<T>(List<T> rows, int totalRows, int pageNumber) {

    public Page {
        if (rows == null) {
            throw new IllegalArgumentException("rows must not be null");
        }
        if (totalRows < 0) {
            throw new IllegalArgumentException("totalRows must not be negative");
        }
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        // Defensive copy: the record's field is final, but the List it points at is not.
        rows = List.copyOf(rows);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }
}