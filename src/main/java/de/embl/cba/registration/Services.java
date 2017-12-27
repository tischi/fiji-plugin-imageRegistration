package de.embl.cba.registration;

import de.embl.cba.registration.ui.RegistrationPlugin;
import net.imagej.DatasetService;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;

import java.util.concurrent.ExecutorService;

public abstract class Services
{
    public static DatasetService datasetService;
    public static LogService logService;
    public static ExecutorService executorService;
    public static StatusService statusService;
    public static UIService uiService;

    public static void setServices( RegistrationPlugin plugin )
    {
        datasetService = plugin.datasetService;
        logService = plugin.logService;
        executorService = plugin.threadService.getExecutorService();
        statusService = plugin.statusService;
        uiService = plugin.uiService;
    }

}
