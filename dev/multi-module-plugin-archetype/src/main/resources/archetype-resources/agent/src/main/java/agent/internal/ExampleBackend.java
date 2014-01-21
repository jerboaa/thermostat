package ${package}.agent.internal;

import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.backend.BaseBackend;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;
import ${package}.storage.ExampleDAO;

public class ExampleBackend extends BaseBackend {

    private static final int CHECK_INTERVAL_MINUTES = 60;
    
    private final ApplicationService appService;
    private Timer timer;
    private final ExampleDAO dao;
    private boolean started;
    
    public ExampleBackend(ApplicationService service, Version version, ExampleDAO dao, WriterID writer) {
        super("Example Backend",
                "Saves a message for an agent",
                "Red Hat, Inc.",
                version.getVersionNumber());
        this.appService = service;
        this.dao = dao;
        this.started = false;
    }
    
    @Override
    public boolean activate() {
        // This is silly and shouldn't really do this every 60 minutes.
        // Anyhow, it's good to illustrate thermostat timers using appService.
        TimerFactory timerFactory = appService.getTimerFactory();
        timer = timerFactory.createTimer();
        timer.setDelay(CHECK_INTERVAL_MINUTES);
        timer.setInitialDelay(0);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setTimeUnit(TimeUnit.MINUTES);
        timer.setAction(new Runnable() {

            @Override
            public void run() {
                dao.putMessage("${helloMessage}");
            }
        });
        timer.start();
        started = true;
        return true;
    }

    @Override
    public boolean deactivate() {
        started = false;
        timer.stop();
        return true;
    }

    @Override
    public boolean isActive() {
        return started;
    }

    @Override
    public int getOrderValue() {
        // offset should be < 100 in this case 88.
        return Ordered.ORDER_CPU_GROUP + 88;
    }

}
