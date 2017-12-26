package de.embl.cba.registration;

import ij.IJ;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import javax.swing.*;

public abstract class PackageLogService
{

    public static LogService logService;
    public static StatusService statusService;

    public static void debug(String message )
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                /*
                if ( statusService != null )
                {
                    statusService.showStatus(message);
                }
                */

                //logService.info( message );
            }
        });
    }

    public static void info(String message )
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                /*
                if ( statusService != null )
                {
                    statusService.showStatus(message);
                }
                */

                IJ.log( message );
            }
        });
    }

    public static long start( String message )
    {
        info( message );
        return System.currentTimeMillis();
    }

    public static void doneInDuration(long startTimeMilliseconds )
    {
        // TODO: depending on the time, show seconds or minutes
        long timeInSeconds = ( System.currentTimeMillis() - startTimeMilliseconds ) / 1000;
        String message = "...done in " + timeInSeconds + " seconds.";
        info( message );
    }


}
