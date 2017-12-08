package embl.almf;

/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */


import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;

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
@Plugin(type = Command.class, menuPath = "Plugins>Gauss Filtering")
public class ImageRegistrationPlugin<T extends RealType<T>> implements Command {
    //
    // Feel free to add more parameters here...
    //

    @Parameter
    private Dataset currentData;

    @Parameter
    private UIService uiService;

    @Parameter
    private OpService opService;

    @Override
    public void run() {

        final Img<T> image = (Img<T>)currentData.getImgPlus();

        ImageRegistration imageRegistration = new ImageRegistration( image );

        int[] dimensionTypes = new int[ image.numDimensions() ];
        dimensionTypes[ 0 ] = ImageRegistration.TRANSFORMABLE_DIM;
        dimensionTypes[ 1 ] = ImageRegistration.TRANSFORMABLE_DIM;
        dimensionTypes[ 2 ] = ImageRegistration.FIXED_DIM;
        dimensionTypes[ 3 ] = ImageRegistration.SEQUENCE_DIM;

        long[] min = Intervals.minAsLongArray( image );
        long[] max = Intervals.maxAsLongArray( image );
        //refMin[ 0 ] = 84; refMax[ 0 ] = 104;
        //refMin[ 1 ] = 0; refMax[ 1 ] = 20;
        min[ 2 ] = 0; max[ 2 ] = 0; // fixed dimension, chosen reference
        min[ 3 ] = 0; max[ 3 ] = 3; // sequence dimension, which time-points to register

        FinalInterval interval = new FinalInterval( min, max );

        imageRegistration.setDimensionTypesAndInterval( dimensionTypes, interval );

        long[] searchRadii = new long[ 2 ];
        searchRadii[ 0 ] = 0;
        searchRadii[ 1 ] = 0;

        imageRegistration.setSearchRadii( searchRadii );

        imageRegistration.computeTransforms();

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

        // ask the user for a file to open
        //final File file = ij.ui().chooseFile(null, "open");

        final File file =
                new File( "/Users/tischi/Documents/fiji-plugin-imageRegistration--data/2d_t_2ch_drift_synthetic_blur.tif");

        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open( file.getPath() );

            // show the inputRAI
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(ImageRegistrationPlugin.class, true);
        }
    }

}