package de.embl.cba.registration;

import de.embl.cba.registration.ui.RegistrationPlugin;
import ij.IJ;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import javax.swing.*;

public class Logger
{

    public static LogService logService;
    public static StatusService statusService;

    public static void setLoggers( RegistrationPlugin plugin )
    {
        logService = plugin.logService;
        statusService = plugin.statusService;
    }

    public static void showStatus( int min, int max, String message )
    {
        if ( statusService != null )
        {
            statusService.showStatus( min, max, message );
        }
        else
        {
            IJ.log( message + " " + min + "/" + max );
            System.out.print( message + " " + min + "/" + max + "\n" );
        }
    }

    public static void debug(String message )
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                info( message );
            }
        });
    }

    public static void info(String message )
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if ( logService != null )
                {
                    logService.info( message );
                }
                else
                {
                    IJ.log( message ); // TODO: remove this
                    System.out.print( message + "\n" );
                }
            }
        });
    }

    public static long start( String message )
    {
        info( message );
        return System.currentTimeMillis();
    }

    public static void doneIn( long startTimeMilliseconds )
    {
        // TODO: depending on the time, show seconds or minutes
        long timeInSeconds = ( System.currentTimeMillis() - startTimeMilliseconds ) / 1000;
        String message = "...done in " + timeInSeconds + " seconds.";
        info( message );
    }



}
