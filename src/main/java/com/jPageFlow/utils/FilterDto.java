package com.jPageFlow.utils;

import lombok.*;

import java.util.*;

@Data
public class FilterDto {
    private Integer page; // Le numéro de page
    private int offset; // A défaut du numéro de page, page = offset / size
    private int size = 10; // La taille de la page
    private String sort; // Le champ et l'ordre de tri (ex: "name,asc")'
    private Map<String, String> filterParams;

    public int getPage() {
        return Objects.requireNonNullElseGet(page, () -> offset / size);
    }

    public FilterSort getSort() {
        if (sort == null || sort.isEmpty()) {
            return null;
        }
        if (sort.contains(",")) {
            String[] split = sort.split(",");
            return new FilterSort(split[0], split[1]);
        }
        return new FilterSort(sort);
    }
}
