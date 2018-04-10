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

    public static void main( final String... args ) throws Exception
    {
        Services.executorService = Executors.newFixedThreadPool( 4 );
        Services.datasetService = new DefaultDatasetService();

        String path ="/Users/tischer/Documents/data/paolo-ronchi--em-registration/chemfix_O6_crop.tif";

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        //MetaImage input = Readers.openUsingDefaultSCIFIO( path, ij );
        MetaImage input = Readers.openVirtualUsingImageJ1( path );

        Viewers.showRAIUsingImageJFunctions( input.rai, input.axisTypes, input.title );

        Settings settings = createSettings( input.rai, input.axisTypes );
        Registration registration = new Registration( settings );
        registration.run();
        registration.logTransformations();

        MetaImage transformed = registration.getTransformedImage( OutputIntervalSizeType.TransformationsEncompassing );
        Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions,transformed.axisOrder );

        //Viewers.showRAIAsImgPlusWithUIService( transformed.rai, ij.dataset(), transformed.axisTypes, transformed.title, ij.ui() );
        Viewers.showRAIUsingImageJFunctions( transformed.rai, transformed.axisTypes, transformed.title );

        //MetaImage reference = registration.getProcessedAndTransformedReferenceImage( );
        //Viewers.showRAIWithUIService( reference.rai, ij.ui() );

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

        min[ 0 ] = 840; max[ 0 ] = min[ 0 ] + 30;
        min[ 1 ] = 210; max[ 1 ] = min[ 1 ] + 80;
        min[ 2 ] = 000; max[ 2 ] = min[ 2 ] + 30;

        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalSizeType = OutputIntervalSizeType.InputImage;

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
