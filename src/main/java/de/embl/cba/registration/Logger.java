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

    public static void setLogger( RegistrationPlugin plugin )
    {
        logService = plugin.logService;
        statusService = plugin.statusService;
    }

    public static void showStatus( int current, int total, String message )
    {

        if ( statusService != null )
        {
            statusService.showStatus( current, total, message );
        }
        else
        {
            IJ.log( message + " " + current + "/" + total );
        }

        waitSomeTime();

    }

    public static void showStatus( String message )
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if ( statusService != null )
                {
                    statusService.showStatus( message );
                }
                else
                {
                    IJ.log( message );
                }
            }
        });
    }

    public static void debug(String message )
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                System.out.print( message + "\n" );
            }
        });
    }

    public static void info(String message )
    {
        SwingUtilities.invokeLater( new Runnable()
        {
            public void run()
            {
                IJ.log( message );
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
        long timeInSeconds = ( System.currentTimeMillis() - startTimeMilliseconds ) / 1000;
        String message = "...done in " + timeInSeconds + " seconds.";
        info( message );
    }


    public static void waitSomeTime()
    {
        try
        {
            Thread.sleep( 10 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }

}
