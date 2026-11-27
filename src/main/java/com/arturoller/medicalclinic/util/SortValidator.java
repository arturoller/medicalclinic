package com.arturoller.medicalclinic.util;

import com.arturoller.medicalclinic.exception.InvalidSortFieldException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SortValidator {
    public void validate(Sort sort, Set<String> allowedFields) {
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!allowedFields.contains(property)) {
                throw new InvalidSortFieldException("Niedozwolone pole sortowania: " + property
                        + ". Dozwolone pola: " + allowedFields.stream().sorted().collect(Collectors.joining(", ")));
            }
        }
    }
}
