package de.embl.cba.registration.views;

import bdv.util.AxisOrder;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.cba.registration.Logger;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;

import static bdv.viewer.DisplayMode.GROUP;

public abstract class BDV
{
    public static void show( RandomAccessibleInterval rai, String title, long numSpatialDimensions, AxisOrder axisOrder )
    {
        long startTime = Logger.start("Preparing BigDataViewer display...");

        Bdv bdv = null;

        if ( numSpatialDimensions == 2 )
        {
            bdv = BdvFunctions.show(
                    rai,
                    title,
                    Bdv.options().is2D().axisOrder( axisOrder ) );

        }
        else if ( numSpatialDimensions == 3 )
        {
            bdv = BdvFunctions.show(
                    rai,
                    title,
                    Bdv.options().axisOrder( axisOrder ) );
        }

        bdv.getBdvHandle().getViewerPanel().setDisplayMode( GROUP );

        Logger.doneIn( startTime );
    }
}
