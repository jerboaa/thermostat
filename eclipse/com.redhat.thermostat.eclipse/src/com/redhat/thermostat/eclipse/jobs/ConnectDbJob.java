package com.redhat.thermostat.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.redhat.thermostat.common.ThreadPoolTimerFactory;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.storage.Connection;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;
import com.redhat.thermostat.common.storage.ConnectionException;
import com.redhat.thermostat.common.storage.MongoStorageProvider;
import com.redhat.thermostat.common.storage.StorageProvider;
import com.redhat.thermostat.eclipse.Activator;
import com.redhat.thermostat.eclipse.ConnectionConfiguration;
import com.redhat.thermostat.eclipse.LoggerFacility;

public class ConnectDbJob extends Job {

    private ConnectionConfiguration configuration;
    
    public ConnectDbJob(String name, ConnectionConfiguration configuration) {
        super(name);
        this.configuration = configuration;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(
                "Connecting to " + configuration.getDBConnectionString(),
                IProgressMonitor.UNKNOWN);
        try {
            connectToBackEnd();
            return Status.OK_STATUS;
        } catch (InvalidConfigurationException | ConnectionException e) {
            LoggerFacility.getInstance().log(IStatus.ERROR,
                    "Could not connect to DB", e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not connect to DB", e);
        }
    }
    
    /*
     * Establish a Mongo DB connection.
     */
    private void connectToBackEnd() throws InvalidConfigurationException, ConnectionException {
        StorageProvider connProv = new MongoStorageProvider(configuration);
        DAOFactory daoFactory = new MongoDAOFactory(connProv);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);
        TimerFactory timerFactory = new ThreadPoolTimerFactory(1);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        Connection connection = daoFactory.getConnection();
        ConnectionListener connectionListener = new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case DISCONNECTED:
                    LoggerFacility.getInstance().log(IStatus.WARNING,
                            "Unexpected disconnect event.");
                    break;
                case CONNECTING:
                    LoggerFacility.getInstance().log(IStatus.INFO,
                            "Connecting to storage.");
                    break;
                case CONNECTED:
                    LoggerFacility.getInstance().log(IStatus.INFO,
                            "Connected to storage.");
                    Activator.getDefault().setConnected(true);
                    break;
                case FAILED_TO_CONNECT:
                    LoggerFacility.getInstance().log(IStatus.WARNING,
                            "Could not connect to storage.");
                default:
                    LoggerFacility.getInstance().log(IStatus.WARNING,
                            "Unfamiliar ConnectionStatus value");
                }
            }
        };
        connection.addListener(connectionListener);
        LoggerFacility.getInstance().log(IStatus.INFO,
                "Connecting to storage...");
        connection.connect();
    }

}
