package de.embl.cba.registration;

import bdv.util.AxisOrder;
import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import de.embl.cba.registration.utils.ViewsUtils;
import ij.ImagePlus;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.view.Views;
import org.scijava.ui.UIService;

import java.util.ArrayList;

public abstract class Viewers
{

    public static ArrayList< AxisType > ImageJAxes = new ArrayList< AxisType > ()
    {
        {
            add( Axes.X );
            add( Axes.Y );
            add( Axes.CHANNEL );
            add( Axes.Z );
            add( Axes.TIME );
        }
    };


    public static void showImagePlusWithImpShow( ImagePlus imp )
    {
        long start = Logger.start( "# Showing ImagePlus using imagePlus.show()..." );
        imp.show();
        Logger.doneIn( start );
    }

    public static void showImgPlusUsingUIService( ImgPlus imgPlus, UIService uiService )
    {
        long start = Logger.start( "# Showing ImgPlus using uiService.show()..." );
        uiService.show( imgPlus );
        Logger.doneIn( start );
    }

    public static void showRAIWithUIService( RandomAccessibleInterval rai, UIService uiService )
    {
        long start = Logger.start( "# Showing ImgPlus using uiService.show()..." );
        uiService.show( rai );
        Logger.doneIn( start );
    }

    public static void showRAIAsImgPlusWithUIService( RandomAccessibleInterval rai,
                                                      DatasetService datasetService,
                                                      ArrayList< AxisType > axisTypes,
                                                      String title,
                                                      UIService uiService )
    {
        ImgPlus imgPlus = convertRAItoImgPlus( rai, datasetService, axisTypes, title );

        long start = Logger.start( "# Showing ImgPlus using uiService.show()..." );

        uiService.show( imgPlus );

        Logger.doneIn( start );
    }


    public static void showRAIUsingImageJFunctions( RandomAccessibleInterval rai,
                                                    ArrayList< AxisType > axisTypes,
                                                    String title )
    {
        long start = Logger.start( "# Showing RAI using ImageJFunctions.show()..." );

        rai = addDimensionsToConformWithImageJ1AxesOrder( rai, axisTypes );
        ImagePlus imp = ImageJFunctions.show( rai );
        imp.setTitle( title );

        Logger.doneIn( start );
    }

    private static RandomAccessibleInterval addDimensionsToConformWithImageJ1AxesOrder( RandomAccessibleInterval rai, ArrayList< AxisType > axisTypes )
    {
        RandomAccessibleInterval raiConformingImageJ = rai;

        for ( int i = 0; i < ImageJAxes.size(); ++i )
        {
            if ( ! axisTypes.contains( ImageJAxes.get( i ) ) )
            {
                raiConformingImageJ = ViewsUtils.insertDimension( raiConformingImageJ, i );
            }
        }

        return raiConformingImageJ;
    }


    public static ImgPlus convertRAItoImgPlus( RandomAccessibleInterval rai,
                                               DatasetService datasetService,
                                               ArrayList< AxisType > axisTypes,
                                               String title )
    {
        long start = Logger.start( "# Converting RAI to ImgPlus using DataService..." );

        AxisType[] axisTypeArray = axisTypes.toArray( new AxisType[0] );

        ImgPlus imgPlus = new ImgPlus( datasetService.create( rai  ), title, axisTypeArray );

        Logger.doneIn( start );

        return imgPlus;
    }

    public static void showRAIUsingBdv( RandomAccessibleInterval rai, String title, long numSpatialDimensions, String axisOrderString )
    {
        long startTime = Logger.start("# Showing RAI using BigDataViewer...");

        if ( axisOrderString.equals( "XYCZT") )
        {
            axisOrderString = "XYZCT";
            rai = Views.permute( rai, 2, 3 );
        }
        else if (  axisOrderString.equals( "XYCZ") )
        {
            axisOrderString = "XYZC";
            rai = Views.permute( rai, 2, 3 );
        }

        Bdv bdv = null;
        AxisOrder axisOrder = AxisOrder.valueOf( axisOrderString );

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
