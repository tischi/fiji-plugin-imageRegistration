package de.embl.cba.registration.axessettings;

import net.imglib2.FinalInterval;

import java.util.Arrays;
import java.util.HashMap;

/**
 *  Important: the referenceInterval has the dimensionality of numFixedAxes
 *  maybe this is not good
 */
public class FixedAxesSettings
{
    public int[] axes;
    public FinalInterval referenceInterval;

    public FixedAxesSettings(
            int[] dimensions,
            FinalInterval referenceInterval )
    {
        this.axes = dimensions;
        this.referenceInterval = referenceInterval;
    }


    public HashMap< Integer, Long > getReferenceAxisCoordinateMap()
    {
        HashMap< Integer, Long > map = new HashMap<>(  );

        for ( int i = 0; i < axes.length; ++i )
        {
            map.put( axes[ i ], referenceInterval.min( i ) );
        }

        return map;
    }


}
