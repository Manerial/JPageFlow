package com.jPageFlow.utils;

/**
 * Represents a sorting order for a specific field.
 *
 * @param fieldName      : The field name to sort by
 * @param ascendingOrder : True by default, false for descending (desc) order
 */
public record FilterSort(String fieldName, boolean ascendingOrder) {
    public FilterSort(String fieldName, String order) {
        this(fieldName, !order.equalsIgnoreCase("desc"));
    }

    public FilterSort(String fieldName) {
        this(fieldName, true);
    }
}
