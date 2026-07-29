package com.jPageFlow.utils;

import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;

import java.math.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class FilterServiceTest {
    record TestRecord1(String value) {
    }

    record TestRecord2(TestRecord1 record1, List<TestRecord1> record1s) {
    }

    // Declaration order intentionally differs from alphabetical order (ZEBRA, APPLE, MANGO vs
    // APPLE, MANGO, ZEBRA) so sorting by ordinal is distinguishable from sorting by name.
    enum TestOrder {
        ZEBRA, APPLE, MANGO
    }

    // AB contains A as a substring — used to check that filtering by "A" doesn't also match AB.
    enum TestCode {
        A, AB
    }

    record PriceRecord(BigDecimal price) {
    }

    record OrderRecord(TestOrder order) {
    }

    record CodeRecord(TestCode code) {
    }

    record TablesRecord(List<Integer> tableNumbers) {
    }

    record TagsRecord(Map<String, String> tags) {
    }


    @Test
    public void testFilter() {
        List<TestRecord1> testRecord1s = createListRecord1("value", 10);

        Page<TestRecord1> page = FilterService.filterData(testRecord1s, null, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(10);
    }

    @Test
    public void testFilterEmptyList() {
        List<TestRecord1> testRecord1s = new ArrayList<>();
        Page<TestRecord1> page = FilterService.filterData(testRecord1s, null, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    public void testFilterWithFilterParams() {
        List<TestRecord1> testRecord1s = createListRecord1("value", 10);
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("value", "value1");
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setFilterParams(filterParams);
        Page<TestRecord1> page = FilterService.filterData(testRecord1s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().value()).isEqualTo("value1");
    }

    @Test
    public void testFilterWithSort() {
        List<TestRecord1> testRecord1s = new ArrayList<>();
        testRecord1s.add(new TestRecord1("b"));
        testRecord1s.add(new TestRecord1("a"));
        testRecord1s.add(new TestRecord1("c"));

        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setSort("value,asc");
        Page<TestRecord1> page = FilterService.filterData(testRecord1s, filterDto, (a) -> a);
        assertThat(page.getContent().get(0).value()).isEqualTo("a");
        assertThat(page.getContent().get(1).value()).isEqualTo("b");
        assertThat(page.getContent().get(2).value()).isEqualTo("c");

        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setSort("value,desc");
        page = FilterService.filterData(testRecord1s, filterDto, (a) -> a);
        assertThat(page.getContent().get(0).value()).isEqualTo("c");
        assertThat(page.getContent().get(1).value()).isEqualTo("b");
        assertThat(page.getContent().get(2).value()).isEqualTo("a");
    }

    @Test
    public void testFilterWithPagination() {
        List<TestRecord1> testRecord1s = createListRecord1("value", 10);

        FilterDto filterDto = new FilterDto();
        filterDto.setPage(1);
        filterDto.setSize(5);
        Page<TestRecord1> page = FilterService.filterData(testRecord1s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent().size()).isEqualTo(5);
        assertThat(page.getContent().getFirst().value()).isEqualTo("value5");
    }

    @Test
    public void testFilterWithOffset() {
        List<TestRecord1> testRecord1s = createListRecord1("value", 10);

        FilterDto filterDto = new FilterDto();
        filterDto.setOffset(5);
        filterDto.setSize(5);
        Page<TestRecord1> page = FilterService.filterData(testRecord1s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent().size()).isEqualTo(5);
        assertThat(page.getContent().getFirst().value()).isEqualTo("value5");
    }

    @Test
    public void testFilterWithEverything() {
        List<TestRecord1> testRecord1s = createListRecord1("value", 100);
        testRecord1s.add(new TestRecord1("specialvalue1"));

        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("value", "value");

        FilterDto filterDto = new FilterDto();
        filterDto.setPage(1);
        filterDto.setSize(5);
        filterDto.setFilterParams(filterParams);
        filterDto.setSort("value,desc");
        Page<TestRecord1> page = FilterService.filterData(testRecord1s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(101);
        assertThat(page.getContent().size()).isEqualTo(5);
        assertThat(page.getContent().getFirst().value()).isEqualTo("value94");
    }

    @Test
    public void testFilterWithSubValues() {
        List<TestRecord2> testRecord2s = createListRecord2(10, 10);
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(5);
        filterDto.setFilterParams(Map.of("record1.value", "value1"));
        Page<TestRecord2> page = FilterService.filterData(testRecord2s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().size()).isEqualTo(1);
    }

    @Test
    public void testFilterWithSubValues_ko() {
        List<TestRecord2> testRecord2s = createListRecord2(10, 10);
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(5);
        filterDto.setFilterParams(Map.of("record1.noField", "value1"));
        Page<TestRecord2> page = FilterService.filterData(testRecord2s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getContent().size()).isEqualTo(0);
    }

    @Test
    public void testSortWithSubValues() {
        List<TestRecord2> testRecord2s = createListRecord2(10, 10);
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(5);
        filterDto.setSort("record1.value,desc");
        Page<TestRecord2> page = FilterService.filterData(testRecord2s, filterDto, (a) -> a);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getContent().size()).isEqualTo(5);
        assertThat(page.getContent().get(0).record1().value()).isEqualTo("value9");
        assertThat(page.getContent().get(1).record1().value()).isEqualTo("value8");
        assertThat(page.getContent().get(2).record1().value()).isEqualTo("value7");
        assertThat(page.getContent().get(3).record1().value()).isEqualTo("value6");
        assertThat(page.getContent().get(4).record1().value()).isEqualTo("value5");
    }

    @Test
    public void testFilterWithSortOnBigDecimal() {
        // Regression: BigDecimal has no dedicated branch in compare() before the Comparable-based
        // rewrite, so it fell back to toString().compareTo() — a lexicographic, not numeric, order.
        // "50.00" < "8.00" lexicographically ('5' < '8'), which would wrongly sort 50.00 before 8.00.
        List<PriceRecord> records = List.of(
                new PriceRecord(new BigDecimal("50.00")),
                new PriceRecord(new BigDecimal("5.00")),
                new PriceRecord(new BigDecimal("8.00"))
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setSort("price,asc");
        Page<PriceRecord> page = FilterService.filterData(records, filterDto, a -> a);
        assertThat(page.getContent()).extracting(PriceRecord::price)
                .containsExactly(new BigDecimal("5.00"), new BigDecimal("8.00"), new BigDecimal("50.00"));
    }

    @Test
    public void testFilterWithSortOnEnum() {
        // Regression: Enum has no dedicated branch either, so it fell back to toString() (= name()),
        // sorting alphabetically instead of by declaration/ordinal order.
        List<OrderRecord> records = List.of(
                new OrderRecord(TestOrder.MANGO),
                new OrderRecord(TestOrder.ZEBRA),
                new OrderRecord(TestOrder.APPLE)
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setSort("order,asc");
        Page<OrderRecord> page = FilterService.filterData(records, filterDto, a -> a);
        // Declaration order: ZEBRA, APPLE, MANGO. Alphabetical would give APPLE, MANGO, ZEBRA.
        assertThat(page.getContent()).extracting(OrderRecord::order)
                .containsExactly(TestOrder.ZEBRA, TestOrder.APPLE, TestOrder.MANGO);
    }

    @Test
    public void testFilterWithFilterParamsOnListOfIntegers_noSubstringCollision() {
        // Regression: a leaf field of type List<Integer> falls straight into fieldContains() without
        // going through the element-by-element Collection branch in checkFieldReccursively (that branch
        // only triggers when the object being tested is itself a collection, e.g. a nested dotted path).
        // Before the fix, fieldContains() stringified the whole list ("[11]") and did a substring test,
        // so searching "1" would wrongly match a list containing only 11.
        List<TablesRecord> records = List.of(
                new TablesRecord(List.of(1)),
                new TablesRecord(List.of(11))
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setFilterParams(Map.of("tableNumbers", "1"));
        Page<TablesRecord> page = FilterService.filterData(records, filterDto, a -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().tableNumbers()).containsExactly(1);
    }

    @Test
    public void testFilterWithFilterParamsOnListOfIntegers_matchesAnyElement() {
        List<TablesRecord> records = List.of(
                new TablesRecord(List.of(1, 2, 3)),
                new TablesRecord(List.of(4, 5, 6))
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setFilterParams(Map.of("tableNumbers", "2"));
        Page<TablesRecord> page = FilterService.filterData(records, filterDto, a -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().tableNumbers()).containsExactly(1, 2, 3);
    }

    @Test
    public void testFilterWithFilterParamsOnMapValues() {
        List<TagsRecord> records = List.of(
                new TagsRecord(Map.of("color", "red")),
                new TagsRecord(Map.of("color", "blue"))
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setFilterParams(Map.of("tags", "red"));
        Page<TagsRecord> page = FilterService.filterData(records, filterDto, a -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void testFilterWithFilterParamsOnEnum_noSubstringCollision() {
        // Regression: before the fix, Enum fell into the substring branch of fieldContains() (it
        // wasn't in the Long/Integer/Double/Boolean whitelist), so filtering "A" would also match
        // AB ("ab".contains("a") == true).
        List<CodeRecord> records = List.of(
                new CodeRecord(TestCode.A),
                new CodeRecord(TestCode.AB)
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setFilterParams(Map.of("code", "A"));
        Page<CodeRecord> page = FilterService.filterData(records, filterDto, a -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().code()).isEqualTo(TestCode.A);
    }

    @Test
    public void testFilterWithFilterParamsOnBigDecimal_exactMatchNotSubstring() {
        // Regression: before the fix, BigDecimal fell into the substring branch, so filtering "5.00"
        // would also match 15.00 ("15.00".contains("5.00") == true).
        List<PriceRecord> records = List.of(
                new PriceRecord(new BigDecimal("5.00")),
                new PriceRecord(new BigDecimal("15.00"))
        );
        FilterDto filterDto = new FilterDto();
        filterDto.setPage(0);
        filterDto.setSize(10);
        filterDto.setFilterParams(Map.of("price", "5.00"));
        Page<PriceRecord> page = FilterService.filterData(records, filterDto, a -> a);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().price()).isEqualByComparingTo("5.00");
    }

    private static List<TestRecord1> createListRecord1(String base, int nbRecord1) {
        List<TestRecord1> testRecord1s = new ArrayList<>();
        for (int i = 0; i < nbRecord1; i++) {
            testRecord1s.add(new TestRecord1(base + i));
        }
        return testRecord1s;
    }

    private static List<TestRecord2> createListRecord2(int nbRecord2, int nbRecord1) {
        List<TestRecord2> testRecord2s = new ArrayList<>();
        for (int i = 0; i < nbRecord2; i++) {
            TestRecord2 testRecord2 = new TestRecord2(new TestRecord1("value" + i), createListRecord1("value" + i, nbRecord1));
            testRecord2s.add(testRecord2);
        }
        return testRecord2s;
    }
}
