package ${package}.storage.internal;

import java.util.HashSet;

import java.util.Set;

import com.redhat.thermostat.storage.core.auth.CategoryRegistration;

public class ExampleCategoryRegistration implements CategoryRegistration {
    @Override
    public Set<String> getCategoryNames() {
        Set<String> categories = new HashSet<>(1);
        categories.add(ExampleDAOImpl.exampleCategory.getName());
        return categories;
    }
}
