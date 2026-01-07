package com.jPageFlow.utils;

import org.springframework.beans.support.*;
import org.springframework.data.domain.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class FilterService {
    public static final String FILTER_DESCRIPTION = """
            Page starts at 0.<br/>
            If size is 0, all data will be returned and Page will be ignored.<br/>
            Sort and FilterParams must contains fields presents in the DTO will get. Example : [sort=id].<br/>
            Sort can be "asc" (by default if absent) or "desc", using a comma after the field name. Example : [sort=id,desc].<br/>
            In swagger, do not use FilterParams (just remove it from the object body).
            """;

    public static <RETURN, ENTRY> Page<RETURN> filterData(List<ENTRY> list, FilterDto filterDto, Function<List<ENTRY>, List<RETURN>> callback) {
        if (list.isEmpty()) {
            return Page.empty();
        }
        if (filterDto == null) {
            return getPage(callback.apply(list));
        }

        List<ENTRY> filteredList = getFilteredList(list, filterDto);
        List<ENTRY> limitedList = filteredList.stream()
                .skip((long) filterDto.getPage() * filterDto.getSize())
                .limit(filterDto.getSize())
                .toList();
        List<RETURN> apply = callback.apply(limitedList);

        return getPage(filterDto, apply, filteredList.size());
    }

    private static <T> List<T> getFilteredList(List<T> list, FilterDto filterDto) {
        Map<String, String> filterParams = filterDto.getFilterParams();
        FilterSort sort = filterDto.getSort();
        Stream<T> stream = list.stream();

        if (filterParams != null) {
            Predicate<T> filterPredicate = dto ->
                    filterParams.entrySet().stream().allMatch(filterEntry ->
                            checkValuesForFieldReccursivelySafe(filterEntry.getKey(), filterEntry.getValue(), dto)
                    );
            stream = stream.filter(filterPredicate);
        }

        if (sort != null) {
            boolean ascending = sort.ascendingOrder();
            Comparator<T> comparator = (o1, o2) -> {
                int result = compare(o1, o2, sort.fieldName());
                return ascending ? result : -result;
            };
            stream = stream.sorted(comparator);
        }

        return stream.toList();
    }

    private static boolean checkValuesForFieldReccursivelySafe(String fieldPath, String expected, Object obj) {
        try {
            return checkValuesForFieldReccursively(fieldPath, expected, obj);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkValuesForFieldReccursively(String key, String value, Object object) throws IllegalAccessException {
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1); // Les valeurs du tableau "0, 1"
            String[] filterValues = value.split(","); // Les valeurs splittées ["0", "1"]
            if (object instanceof Collection<?>) {
                // Si object est une collection, on veut que toutes les valeurs du filtre match notre objet
                for (String subFilterValue : filterValues) {
                    if (!checkFieldReccursively(key, subFilterValue, object)) {
                        return false;
                    }
                }
                return true;
            } else {
                // Sinon, on veut que la valeur de l'objet match une des valeurs du filtre
                for (String subFilterValue : filterValues) {
                    if (checkFieldReccursively(key, subFilterValue, object)) {
                        return true;
                    }
                }
                return false;
            }
        } else {
            return checkFieldReccursively(key, value, object);
        }
    }

    private static boolean checkFieldReccursively(String key, String value, Object object) throws IllegalAccessException {
        if (object instanceof Collection<?> collection) {
            // key = id
            // value = [1]
            // object = [{id: 1}, {id: 3}]
            for (Object o : collection) {
                if (checkFieldReccursively(key, value, o)) {
                    return true;
                }
            }
            return false;
        }
        String[] subKeys = key.split("\\.", 2);
        //Note, this will throw an exception if the field doesn't exist.
        Field field = getField(subKeys[0], object.getClass());
        field.setAccessible(true);
        if (subKeys.length > 1) {
            return checkFieldReccursively(subKeys[1], value, field.get(object));
        }
        return fieldContains(field.get(object), value);
    }

    private static boolean fieldContains(Object fieldValue, String filterValue) {
        String fieldValueLC = fieldValue.toString().toLowerCase().trim();
        String filterValueLC = filterValue.toLowerCase().trim();
        if (fieldValue instanceof Long || fieldValue instanceof Integer || fieldValue instanceof Double || fieldValue instanceof Boolean) {
            return fieldValueLC.equals(filterValueLC);
        } else {
            return fieldValueLC.contains(filterValueLC);
        }
    }

    private static <T> int compare(T o1, T o2, String fieldName) {
        try {
            Object valO1 = getFieldReccursively(fieldName, o1);
            Object valO2 = getFieldReccursively(fieldName, o2);
            if (valO1 instanceof Long) {
                return Long.compare((Long) valO1, (Long) valO2);
            } else if (valO1 instanceof Integer) {
                return Integer.compare((Integer) valO1, (Integer) valO2);
            } else if (valO1 instanceof Double) {
                return Double.compare((Double) valO1, (Double) valO2);
            } else if (valO1 instanceof Boolean) {
                return Boolean.compare((Boolean) valO1, (Boolean) valO2);
            } else if (valO1 == null || valO2 == null) {
                return (valO1 == null ? -1 : 0) + (valO2 == null ? 1 : 0);
            } else {
                return valO1.toString().compareTo(valO2.toString());
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getFieldReccursively(String key, Object object) throws IllegalAccessException {
        if (object instanceof ArrayList<?>) {
            throw new IllegalArgumentException("Cannot get field on an array");
        }
        String[] subKeys = key.split("\\.", 2);
        //Note, this will throw an exception if the field doesn't exist.
        Field field = getField(subKeys[0], object.getClass());
        field.setAccessible(true);
        if (subKeys.length > 1) {
            return getFieldReccursively(subKeys[1], field.get(object));
        }
        return field.get(object);
    }

    private static Field getField(String fieldName, Class<?> clazz) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return getField(fieldName, clazz.getSuperclass());
        }
    }

    private static <T> Page<T> getPage(List<T> list) {
        return new PageImpl<>(list, PageRequest.of(0, list.size()), list.size());
    }

    private static <T> Page<T> getPage(FilterDto filterDto, List<T> list, int size) {
        if (list.isEmpty()) {
            return Page.empty();
        }
        PagedListHolder<T> page = new PagedListHolder<>(list);
        int pageSize, pageIndex;
        if (filterDto.getSize() > 0) {
            pageSize = filterDto.getSize();
            pageIndex = filterDto.getPage();
        } else {
            pageSize = list.size();
            pageIndex = 0;
        }
        page.setPageSize(pageSize);
        page.setPage(pageIndex);
        List<T> pagedList = page.getPageList();
        return new PageImpl<>(pagedList, PageRequest.of(pageIndex, pageSize), size);
    }
}
