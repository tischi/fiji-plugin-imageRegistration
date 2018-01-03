package de.embl.cba.registration.views;

import bdv.util.AxisOrder;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import net.imagej.ImgPlus;

import static bdv.viewer.DisplayMode.GROUP;

public abstract class BDV
{
    public static void show( ImgPlus img, long numSpatialDimensions, AxisOrder axisOrder )
    {
        Bdv bdv = null;

        if ( numSpatialDimensions == 2 )
        {
            bdv = BdvFunctions.show(
                    img,
                    img.getName(),
                    Bdv.options().is2D().axisOrder( axisOrder ) );

        }
        else if ( numSpatialDimensions == 3 )
        {
            bdv = BdvFunctions.show(
                    img,
                    img.getName(),
                    Bdv.options().axisOrder( axisOrder ) );
        }

        bdv.getBdvHandle().getViewerPanel().setDisplayMode( GROUP );
    }
}
