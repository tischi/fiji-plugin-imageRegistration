import de.embl.cba.registration.*;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.util.MetaImage;
import de.embl.cba.registration.Readers;
import de.embl.cba.registration.Viewers;
import net.imagej.DefaultDatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Register2DFibSEM1DTranslationFullSize
{
    public static String LOCAL_FOLDER = "/Users/tischer/Documents/fiji-plugin-imageRegistration";

    public static void main(final String... args) throws Exception
    {
        Services.executorService = Executors.newFixedThreadPool( 4 );
        Services.datasetService = new DefaultDatasetService();

        String path ="/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop.tif";

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        MetaImage input = Readers.openUsingDefaultSCIFIO( path, ij );
        //input.axisTypes = new ArrayList<>(  );
        //input.axisTypes.add( net.imagej.axis.Axes.X );
        //input.axisTypes.add( net.imagej.axis.Axes.Y );
        //input.axisTypes.add( net.imagej.axis.Axes.Z );
        //Viewers.showImagePlusUsingImpShow( input.imp );
        Viewers.showImgPlusUsingIjUiShow( input.imgPlus, ij );

        Settings settings = createSettings( input.rai, input.axisTypes );
        Registration registration = new Registration( settings );
        registration.run();
        registration.logTransformations();

        MetaImage transformed = registration.transformedImage( OutputIntervalType.InputImageSize );

        /*
        long start = Logger.start( "# Copy image to RAM..." );
        ImageFilter imageFilter = new ImageFilterAsArrayImg( null );
        RandomAccessibleInterval rai = imageFilter.apply( transformed.rai );
        Logger.doneIn( start );
        */

        //Viewers.showRAIUsingIjUiShow( transformed.rai, ij );
        Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions,transformed.axisOrder );


        // Output output = registration.output();
        //ij.ui().show( output.referenceImgPlus );
        //BDV.show( registration.getTransformedInput(), "registered", output.transformedNumSpatialDimensions, output.transformedAxisOrder );
        //BDV.show( output.referenceImgPlus, output.referenceNumSpatialDimensions, output.referenceAxisOrder );

    }

    private static Settings createSettings( RandomAccessibleInterval rai, ArrayList< AxisType > axisTypes )
    {
        Settings settings = new Settings( );

        settings.rai = rai;
        settings.axisTypes = axisTypes;

        settings.registrationAxisTypes = new ArrayList<>(  );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( rai );
        long[] max = Intervals.maxAsLongArray( rai );

        min[ 0 ] = 0; max[ 0 ] = 50;
        min[ 1 ] = 200; max[ 1 ] = 270;
        min[ 2 ] = 0; max[ 2 ] = 50;

        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalType = OutputIntervalType.InputImageSize;

        settings.filterSettings = new FilterSettings();
        settings.filterSettings.filterTypes = new ArrayList<>(  );
        settings.filterSettings.filterTypes.add( FilterType.SubSample );
        settings.filterSettings.subSampling = new long[]{ 1L };
        settings.filterSettings.filterTypes.add( FilterType.Threshold );
        settings.filterSettings.filterTypes.add( FilterType.DifferenceOfGaussian );
        settings.filterSettings.thresholdMin = 5.0D;
        settings.filterSettings.thresholdMax = 150.0D;
        settings.filterSettings.gaussSigmaSmaller = new double[]{ 2.0D };
        settings.filterSettings.gaussSigmaLarger = new double[]{ 6.0D };

        settings.setAxes();

        settings.transformSettings = new TransformSettings();
        settings.transformSettings.maximalTranslations = new double[] { 3000.0D };
        settings.transformSettings.transformFinderType = TransformFinderType.Translation__PhaseCorrelation;
        settings.transformSettings.maximalRotations = new double[] { 0.0D };

        return settings;
    }
}
