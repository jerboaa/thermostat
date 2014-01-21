package ${package}.storage;

import java.util.Objects;

import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;

/**
 * This is the model class which gets persisted
 *
 */
@Entity
public class ExampleMessage extends BasePojo {
    
    private String message;
    
    public ExampleMessage(String writerId) {
        super(writerId);
    }
    
    // Used for JSON serialization. Don't
    // explicitly use it.
    public ExampleMessage() {
        this(null);
    }
    
    @Persist
    public void setMessage(String message) {
        this.message = message;
    }

    @Persist
    public String getMessage() {
        return message;
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), message);
    }
    
    public boolean equals(Object other) {
        if (!(other instanceof ExampleMessage)) {
            return false;
        }
        ExampleMessage o = (ExampleMessage)other;
        return super.equals(o) &&
                message.equals(o.message);
                
    }
}
