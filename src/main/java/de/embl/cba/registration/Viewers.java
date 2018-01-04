package de.embl.cba.registration;

import bdv.util.AxisOrder;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.cba.registration.Logger;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;

import static bdv.viewer.DisplayMode.GROUP;

public abstract class Viewers
{

    public static void showImagePlusUsingImpShow( ImagePlus imp )
    {
        long start = Logger.start( "# Showing ImagePlus using imp.show()..." );
        imp.show();
        Logger.doneIn( start );
    }


    public static void showRAIUsingIjUiShow( RandomAccessibleInterval rai, ImageJ ij )
    {
        long start = Logger.start( "# Showing RAI using ij.ui().show()..." );
        ij.ui().show( rai );
        Logger.doneIn( start );
    }

    public static void showImgPlusUsingIjUiShow( ImgPlus imgPlus, ImageJ ij )
    {
        long start = Logger.start( "# Showing ImgPlus using ij.ui().show()..." );
        ij.ui().show( imgPlus );
        Logger.doneIn( start );
    }

    public static void showRAIUsingBdv( RandomAccessibleInterval rai, String title, long numSpatialDimensions, AxisOrder axisOrder )
    {
        long startTime = Logger.start("# Showing RAI using BigDataViewer...");

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

        //bdv.getBdvHandle().getViewerPanel().setDisplayMode( GROUP );

        Logger.doneIn( startTime );
    }
}
