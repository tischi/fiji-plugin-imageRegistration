package de.embl.cba.registration;

import ij.IJ;
import org.scijava.log.LogService;

import javax.swing.*;

public abstract class PackageLogService {

    public static LogService logService;

    public static void info( String message )
    {
        IJ.log( message );

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                IJ.log( "[INVOKE LATER] " +message );
            }
        });
    }

}
