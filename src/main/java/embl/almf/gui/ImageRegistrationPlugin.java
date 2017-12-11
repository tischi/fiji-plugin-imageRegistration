package embl.almf.gui;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import com.google.common.base.Enums;
import embl.almf.*;
import embl.almf.filter.ImageFilterType;
import ij.IJ;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.Interactive;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.NumberWidget;

import java.io.File;
import java.util.*;

import static embl.almf.filter.ImageFilterParameters.*;

@Plugin(type = Command.class,
        menuPath = "Plugins>Image Registration",
        initializer = "initFields")
public class ImageRegistrationPlugin<T extends RealType<T>>
        extends DynamicCommand
{

    @Parameter
    private Dataset dataset;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Parameter(label = "Transformation",
            choices = "Translation" )
    private String transformationTypeInput;

    @Parameter(label = "Transformation finding method",
            choices = "PhaseCorrelation" )
    private String transformationFindingMethodInput;

    @SuppressWarnings("unchecked")
    private MutableModuleItem<Long> varInput(final int d, final String var) {
        return (MutableModuleItem<Long>) getInfo().getInput( varName(d, var) );
    }

    @SuppressWarnings("unchecked")
    private MutableModuleItem<String> typeInput( final int d ) {
        return (MutableModuleItem<String>) getInfo().getInput( typeName( d ) );
    }


    @Override
    public void run() {

        // Transformation type
        //
        TransformationType transformationType
                = TransformationType.valueOf( transformationTypeInput );

        TransformationFindingMethod transformationFindingMethod
                = TransformationFindingMethod.valueOf( transformationFindingMethodInput );

        // Get axis types and interval from GUI
        //
        long[] min = Intervals.minAsLongArray( dataset );
        long[] max = Intervals.maxAsLongArray( dataset );
        String[] axisTypes = new String[ dataset.numDimensions() ];
        for ( int d = 0; d < dataset.numDimensions(); ++d )
        {
            axisTypes[ d ] = typeInput( d ).getValue( this );
            min[ d ] = varInput( d, "min" ).getValue( this );
            max[ d ] = varInput( d, "max" ).getValue( this );
        }
        FinalInterval interval = new FinalInterval( min, max );

        // Get search radius

        final Img<T> image = (Img<T>) dataset.getImgPlus();



        IJ.log( " AAAAAA " );
        IJ.log( " AAAAAA " );

        //TransformationType.values();

        /*

        //
        // Enter inputRAI processing code here ...
        // The following is just a Gauss filtering example
        //
        final double[] sigmas = {1.0, 3.0, 5.0};

        List<RandomAccessibleInterval<T>> results = new ArrayList<>();

        for (double sigma : sigmas) {
            results.add(opService.filter().gauss(inputRAI, sigma));
        }

        // display result
        for (RandomAccessibleInterval<T> elem : results) {
            uiService.show(elem);
        }
        */
    }


    protected void initFields()
    {

        int n = dataset.numDimensions();

        ArrayList< String > axisTypes = TransformationAxisType.asStringList();
        ArrayList< String > axisNames = ImageRegistrationParameters.getAxisNamesAsStringList( dataset );

        // Guess default axisType choices
        //
        String sequenceDefault = axisNames.get( 0 );
        if ( axisNames.contains( Axes.TIME.toString() ) )
        {
            sequenceDefault = Axes.TIME.toString();
        }
        else if ( axisNames.contains( Axes.Z.toString() ) )
        {
            sequenceDefault = Axes.Z.toString();
        }
        else if ( axisNames.contains( Axes.CHANNEL.toString() ) )
        {
            sequenceDefault = Axes.CHANNEL.toString();
        }

        for (int d = 0; d < dataset.numDimensions(); d++)
        {

            String var;

            // Message
            //
            final MutableModuleItem<String> axisItem =
                    addInput( axisNames.get( d ), String.class);
            axisItem.setPersisted( false );
            axisItem.setVisibility( ItemVisibility.MESSAGE );
            axisItem.setValue(this, axisNames.get( d ) );

            // Axis type selection
            //
            var = "type";
            final MutableModuleItem<String> typeItem =
                    addInput( typeName( d ), String.class);
            typeItem.setPersisted( false );
            typeItem.setLabel( "Type" );
            typeItem.setChoices( axisTypes );
            if ( axisNames.get( d ).equals( sequenceDefault ) )
                typeItem.setValue( this, "" + TransformationAxisType.SEQUENCE_DIMENSION );
            else if ( axisNames.get( d ).equals( Axes.X.toString() ))
                typeItem.setValue( this, "" + TransformationAxisType.TRANSFORMABLE_DIMENSION );
            else if ( axisNames.get( d ).equals( Axes.Y.toString() ))
                typeItem.setValue( this, "" + TransformationAxisType.TRANSFORMABLE_DIMENSION );
            else if ( axisNames.get( d ).equals( Axes.Z.toString() ))
                typeItem.setValue( this, "" + TransformationAxisType.TRANSFORMABLE_DIMENSION );
            else if ( axisNames.get( d ).equals( Axes.CHANNEL.toString() ))
                typeItem.setValue( this, "" + TransformationAxisType.FIXED_DIMENSION );

            // Interval minimum
            //
            var = "min";
            final MutableModuleItem<Long> minItem =
                    addInput( varName(d, var), Long.class);
            minItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            minItem.setPersisted( false );
            minItem.setLabel( var );
            minItem.setValue(this, dataset.min( d ));
            minItem.setMinimumValue( dataset.min( d ) );
            minItem.setMaximumValue( dataset.max( d ) );

            // Interval maximum
            //
            var = "max";
            final MutableModuleItem<Long> maxItem =
                    addInput( varName(d, var), Long.class);
            maxItem.setWidgetStyle( NumberWidget.SLIDER_STYLE );
            maxItem.setPersisted( false );
            maxItem.setLabel( var );
            maxItem.setValue(this, dataset.max( d ) );
            maxItem.setMinimumValue( dataset.min( d ) );
            maxItem.setMaximumValue( dataset.max( d ) );


        }

    }


    // -- Helper methods --

    private String typeName( final int d ) {
        return "type" + d;
    }

    private String varName( final int d, final String var ) {
        return "var" + d + ":" + var;
    }


    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();


        boolean GUI = true;
        boolean TEST = false;


        // ask the user for a file to open
        //final File file = ij.ui().chooseFile(null, "open");

        final File file =
                new File( "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/2d_t_2ch_drift_synthetic_blur.tif");

        Dataset dataset = null;
        int n = 0;
        if (file != null)
        {
            // load and show dataset
            //
            dataset = ij.scifio().datasetIO().open( file.getPath() );
            n = dataset.numDimensions();
            ij.ui().show( dataset );


//            IJ.run("Image Sequence...",
//                    "open=/Users/tischi/Documents/fiji-plugin-imageRegistration--data/mri-stack-16bit sort use");
//            ImagePlus imp = IJ.getImage(); n = 3;

            // convert of cellImg that is lazily loaded
            //
//            Img< UnsignedShortType > img = ConvertVirtualStackToCellImg.getCellImgUnsignedShort( imp );
//            ImgPlus< UnsignedShortType > imgp = new ImgPlus<>( img, "title", new AxisType[]{ Axes.X, Axes.Y, Axes.Z } );
////            ij.get(LegacyService.class).getImageMap().addMapping(  ); // but it's private...
//            //imp.hide(); ImageJFunctions.show( img );
//            ij.ui().show( imgp );
        }



        if ( GUI )
        {
            // invoke the plugin
            ij.command().run( ImageRegistrationPlugin.class, true );
        }
        else if ( TEST )
        {

            String[] dimensionTypes = new String[ n ];
            dimensionTypes[ 0 ] = ""+ TransformationAxisType.TRANSFORMABLE_DIMENSION;
            dimensionTypes[ 1 ] = ""+ TransformationAxisType.TRANSFORMABLE_DIMENSION;
            dimensionTypes[ 2 ] = ""+ TransformationAxisType.FIXED_DIMENSION;
            dimensionTypes[ 3 ] = ""+ TransformationAxisType.SEQUENCE_DIMENSION;

            long[] min = Intervals.minAsLongArray( dataset );
            long[] max = Intervals.maxAsLongArray( dataset );
            min[ 0 ] = 50; max[ 0 ] = 220;
            min[ 1 ] = 50; max[ 1 ] = 220;
            min[ 2 ] = 0; max[ 2 ] = 0; // fixed dimension, chosen reference
            min[ 3 ] = 0; max[ 3 ] = 8; // sequence dimension, which time-points to register

            FinalInterval interval = new FinalInterval( min, max );

            long[] searchRadius = new long[ 2 ];
            searchRadius[ 0 ] = 30;
            searchRadius[ 1 ] = 30;

            // Configure image filtering
            //
            Map< String, Object > imageFilterParameters = new HashMap<>();

            ImageFilterType imageFilterType = ImageFilterType.DOG_THRESHOLD;

            imageFilterParameters.put(
                    GAUSS_SIGMA, new double[]{ 10.0D, 1.0D} );
            imageFilterParameters.put(
                    THRESHOLD_VALUE, 20.0D );
            imageFilterParameters.put(
                    DOG_SIGMA_SMALLER, new double[]{ 1.0D, 1.0D} );
            imageFilterParameters.put(
                    DOG_SIGMA_LARGER, new double[]{ 10.0D, 10.0D} );


            boolean showFixedImageSequence = true;

            //imageFilterType = null;

            ImageRegistration imageRegistration =
                    new ImageRegistration(
                            dataset,
                            dimensionTypes,
                            interval,
                            searchRadius,
                            3,
                            imageFilterType,
                            imageFilterParameters,
                            showFixedImageSequence );

            /*
            ImageJFunctions.show(
                    imageRegistration.getFilteredImage(),
                    "filtering preview");
                    */

            imageRegistration.run();
        }


    }

}