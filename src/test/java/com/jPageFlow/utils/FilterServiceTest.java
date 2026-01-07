package com.jPageFlow.utils;

import org.junit.jupiter.api.*;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class FilterServiceTest {
    record TestRecord1(String value) {
    }

    record TestRecord2(TestRecord1 record1, List<TestRecord1> record1s) {
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
