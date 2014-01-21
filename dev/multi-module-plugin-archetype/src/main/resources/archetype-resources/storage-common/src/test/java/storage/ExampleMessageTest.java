package ${package}.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExampleMessageTest {

    @Test
    public void testEquals() {
        ExampleMessage message = new ExampleMessage();
        message.setAgentId("foo");
        message.setMessage("bar");
        
        assertTrue(message.equals(message));
        ExampleMessage message2 = new ExampleMessage();
        assertFalse(message.equals(message2));
        assertFalse(message.equals("I'm a string"));
        
        message2.setAgentId("foo");
        assertFalse(message.equals(message2));
        message2.setMessage("bar");
        assertTrue(message.equals(message2));
    }
    
    @Test
    public void testHashCode() {
        ExampleMessage message = new ExampleMessage();
        message.setAgentId("foo");
        message.setMessage("bar");
        
        assertTrue(message.hashCode() == message.hashCode());
        ExampleMessage message2 = new ExampleMessage();
        assertFalse(message.hashCode() == message2.hashCode());
        assertFalse(message.hashCode() == "I'm a string".hashCode());
        
        message2.setAgentId("foo");
        assertFalse(message.hashCode() == message2.hashCode());
        message2.setMessage("bar");
        assertTrue(message.hashCode() == message2.hashCode());
    }
    
}
