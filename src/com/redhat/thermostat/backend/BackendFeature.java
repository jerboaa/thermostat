package com.redhat.thermostat.backend;

/**
 * A specific feature provided by a {@link Backend}. Each {@link Backend}
 * provides a different set of features. Some may even provide none.
 */
public class BackendFeature {
    public static final BackendFeature MXBEANS = new BackendFeature("mxbeans");
    public static final BackendFeature PERF_COUNTER = new BackendFeature("perfcounter");
    public static final BackendFeature PROC = new BackendFeature("proc");
    public static final BackendFeature STARTUP_ARGUMENTS = new BackendFeature("startup-arguments");
    public static final BackendFeature SYSTEM_PROPERTIES = new BackendFeature("system-properties");
    public static final BackendFeature HEAP = new BackendFeature("heap");

    private String featureName;

    public BackendFeature(String featureName) {
        this.featureName = featureName;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (other.getClass() == this.getClass()) {
            BackendFeature otherFeature = (BackendFeature) other;
            if (otherFeature.featureName.equals(featureName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return featureName.hashCode();
    }

}
