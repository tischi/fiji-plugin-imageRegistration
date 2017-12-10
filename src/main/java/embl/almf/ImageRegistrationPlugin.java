package embl.almf;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import embl.almf.filter.ImageFilterType;
import embl.almf.filter.ImageFilterParameters;
import ij.IJ;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;
import java.util.*;

/**
 * This example illustrates how to create an ImageJ {@link Command} plugin.
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 * <p>
 * You should replace the parameter fields with your own inputs and outputs,
 * and replace the {@link run} method implementation with your own logic.
 * </p>
 */
@Plugin(
        type = Command.class,
        menuPath = "Plugins>Image Registration",
        initializer = "initFields")
public class ImageRegistrationPlugin<T extends RealType<T>>  extends DynamicCommand {
    //
    // Feel free to add more parameters here...
    //

    @Parameter
    private Dataset dataset;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    public String getAxisType( final int d ) {
        final String value = typeInput(d).getValue(this);
        return value;
    }


    @Override
    public void run() {

        final Img<T> image = (Img<T>) dataset.getImgPlus();


        for (int d = 0; d < dataset.numDimensions(); d++)
        {
            IJ.log( "Axis type " + getAxisType(d) );
        }



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

        // Figure out which axes we have
        //
        ArrayList< AxisType > axisTypes = new ArrayList<>(  );
        for (int d = 0; d < dataset.numDimensions(); d++)
        {
            axisTypes.add( dataset.axis( d ).type() );
        }

        // Use heuristics to create registration suggestions
        //
        String[] axisRegistrationTypes = new String[ dataset.numDimensions() ];
        Arrays.fill( axisRegistrationTypes, ImageRegistration.TRANSFORMABLE_DIMENSION );

        if ( axisTypes.contains( Axes.TIME ) )
        {
            axisRegistrationTypes[ axisTypes.indexOf( Axes.TIME ) ]
                = ImageRegistration.SEQUENCE_DIMENSION;

            if ( axisTypes.contains( Axes.Z ) )
            {
                axisRegistrationTypes[ axisTypes.indexOf( Axes.Z ) ]
                        = ImageRegistration.TRANSFORMABLE_DIMENSION;
            }
        }
        else if ( axisTypes.contains( Axes.Z ) )
        {
            axisRegistrationTypes[ axisTypes.indexOf( Axes.Z ) ]
                    = ImageRegistration.SEQUENCE_DIMENSION;
        }

        if ( axisTypes.contains( Axes.X ) )
        {
            axisRegistrationTypes[ axisTypes.indexOf( Axes.X ) ]
                    = ImageRegistration.TRANSFORMABLE_DIMENSION;
        }

        if ( axisTypes.contains( Axes.Y ) )
        {
            axisRegistrationTypes[ axisTypes.indexOf( Axes.Y ) ]
                    = ImageRegistration.TRANSFORMABLE_DIMENSION;
        }

        if ( axisTypes.contains( Axes.CHANNEL) )
        {
            axisRegistrationTypes[ axisTypes.indexOf( Axes.CHANNEL ) ]
                    = ImageRegistration.FIXED_DIMENSION;
        }

        // Create GUI
        //

        List< String > choices = new ArrayList<>(  );
        choices.add( ImageRegistration.FIXED_DIMENSION );
        choices.add( ImageRegistration.TRANSFORMABLE_DIMENSION );
        choices.add( ImageRegistration.SEQUENCE_DIMENSION );

        for (int d = 0; d < dataset.numDimensions(); d++)
        {

            final MutableModuleItem<String> axisItem =
                    addInput("axis" + d, String.class);
            axisItem.setPersisted(false);
            axisItem.setVisibility( ItemVisibility.MESSAGE );
            axisItem.setValue(this, "-- Axis #" + (d + 1) + " --");

            final MutableModuleItem<String> typeItem =
                    addInput(typeName(d), String.class);
            typeItem.setPersisted(false);
            typeItem.setLabel("Type");
            typeItem.setChoices( choices );
            typeItem.setValue(this, ImageRegistration.SEQUENCE_DIMENSION );

            String var = "min";
            final MutableModuleItem<Long> minItem =
                    addInput(varName(d, var), Long.class);
            minItem.setPersisted(false);
            minItem.setLabel(var);
            minItem.setValue(this, dataset.min( d ));

            var = "max";
            final MutableModuleItem<Long> maxItem =
                    addInput(varName(d, var), Long.class);
            maxItem.setPersisted(false);
            maxItem.setLabel(var);
            maxItem.setValue(this, dataset.max( d ));

        }

    }


    // -- Helper methods --

    private String typeName(final int d) {
        return "type" + d;
    }

    private String varName(final int d, final String var) {
        return "var" + d + ":" + var;
    }

    @SuppressWarnings("unchecked")
    private MutableModuleItem<String> typeInput(final int d) {
        return (MutableModuleItem<String>) getInfo().getInput(typeName(d));
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


        boolean GUI = false;

        // ask the user for a file to open
        //final File file = ij.ui().chooseFile(null, "open");

        final File file =
                new File( "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/2d_t_2ch_drift_synthetic_blur.tif");

        Dataset dataset = null;

        if (file != null)
        {
            // load the dataset
            dataset = ij.scifio().datasetIO().open( file.getPath() );
        }

        int n = dataset.numDimensions();

        if ( GUI )
        {
            // show the inputRAI
            ij.ui().show( dataset );

            // invoke the plugin
            ij.command().run(ImageRegistrationPlugin.class, true);
        }
        else
        {
            ij.ui().show( dataset );

            String[] dimensionTypes = new String[n];
            dimensionTypes[ 0 ] = ImageRegistration.TRANSFORMABLE_DIMENSION;
            dimensionTypes[ 1 ] = ImageRegistration.TRANSFORMABLE_DIMENSION;
            dimensionTypes[ 2 ] = ImageRegistration.FIXED_DIMENSION;
            dimensionTypes[ 3 ] = ImageRegistration.SEQUENCE_DIMENSION;


            long[] min = Intervals.minAsLongArray( dataset );
            long[] max = Intervals.maxAsLongArray( dataset );
            min[ 0 ] = 50; max[ 0 ] = 220;
            min[ 1 ] = 50; max[ 1 ] = 220;
            min[ 2 ] = 0; max[ 2 ] = 0; // fixed dimension, chosen reference
            min[ 3 ] = 0; max[ 3 ] = 8; // sequence dimension, which time-points to register

            FinalInterval interval = new FinalInterval( min, max );

            long[] searchRadius = new long[ 2 ];
            searchRadius[ 0 ] = 0;
            searchRadius[ 1 ] = 0;

            // Configure image filtering
            //
            Map< String, Object > imageFilterParameters = new HashMap<>();

            ImageFilterType imageFilterType = ImageFilterType.GAUSS;
            imageFilterParameters.put(
                    ImageFilterParameters.GAUSS_SIGMA,
                    new double[]{ 10.0D, 1.0D} );

            /*
            ImageFilterType imageFilterType = ImageFilterType.THRESHOLD;
            imageFilterParameters.put( ImageFilterParameters.THRESHOLD_VALUE, 20.0D );
            */

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