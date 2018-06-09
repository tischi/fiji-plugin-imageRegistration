import de.embl.cba.registration.*;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.projection.ProjectionType;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.utils.MetaImage;
import net.imagej.Dataset;
import net.imagej.DefaultDatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class UteBecherer
{

    public static void main(final String... args) throws Exception
    {
        Services.executorService = Executors.newFixedThreadPool( 4 );
        Services.datasetService = new DefaultDatasetService();

        //String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crop_rotated-cropped-1-10.zip";
        //String path = "/Users/tischer/Desktop/crop_rotated-cropped.tif";
        String path = "/Users/tischer/Downloads/Ute Becherer/Composite.zip";

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        MetaImage input = Readers.openUsingImageJ1( path );
        Viewers.showRAIUsingImageJFunctions( input.rai, input.axisTypes, input.title );

        Settings settings = createSettings( input.rai, input.axisTypes );

        Registration registration = new Registration( settings );
        registration.run();
        //registration.logTransformations();

        MetaImage transformed = registration.getTransformedImage( OutputIntervalSizeType.InputImage );
        Viewers.showRAIUsingImageJFunctions( transformed.rai,  transformed.axisTypes, "transformed" );
        //Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions, transformed.axisOrder );

        MetaImage referenceRegion = registration.getProcessedAndTransformedReferenceImage( );
        Viewers.showRAIUsingImageJFunctions( referenceRegion.rai, referenceRegion.axisTypes, "reference region" );


//        Output output = registration.output();
//
//        ij.ui().show( output.transformedImgPlus );
//        ij.ui().show( output.referenceImgPlus );

        //BDV.show( output.transformedImgPlus, output.transformedNumSpatialDimensions, output.transformedAxisOrder );
        //BDV.show( output.referenceImgPlus, output.referenceNumSpatialDimensions, output.referenceAxisOrder );

    }

    private static Dataset getDataset( String path, ImageJ ij ) throws IOException
    {
        final File file = new File( path );
        return ij.scifio().datasetIO().open(file.getPath());
    }

    private static Settings createSettings( RandomAccessibleInterval rai, ArrayList< AxisType > axisTypes )
    {
        Settings settings = new Settings( );

        settings.rai = rai;
        settings.axisTypes = axisTypes;

        settings.registrationAxisTypes = new ArrayList<>(  );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( rai );
        long[] max = Intervals.maxAsLongArray( rai );
        min[ 0 ] = 100;
        max[ 0 ] = 140;
        min[ 1 ] = 168;
        max[ 1 ] = 259;
        min[ 2 ] = 0;
        max[ 2 ] = 0;

        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalSizeType = OutputIntervalSizeType.InputImage;

        settings.projectionType = ProjectionType.Average;
        settings.filterSettings = new FilterSettings();
        settings.filterSettings.filterTypes = new ArrayList<>(  );

        //addThresholdFilter( settings );
        //addGaussianFilter( settings );
        addDifferenceOfGaussianFilter( settings );
        //addGradientFilter( settings );

        settings.setAxes();

        settings.transformSettings = new TransformSettings();
        settings.transformSettings.transformFinderType = TransformFinderType.Translation__Maximum;

        return settings;
    }

    private static void addDifferenceOfGaussianFilter( Settings settings )
    {
        settings.filterSettings.filterTypes.add( FilterType.DifferenceOfGaussian );
        settings.filterSettings.gaussSigma = new double[]{ 8.0D };
    }

    private static void addGaussianFilter( Settings settings )
    {
        settings.filterSettings.filterTypes.add( FilterType.Gauss );
        settings.filterSettings.gaussSigma = new double[]{ 1.0D };
    }

    private static void addGradientFilter( Settings settings )
    {
        settings.filterSettings.filterTypes.add( FilterType.Gradient );
        settings.filterSettings.gaussSigma = new double[]{ 30.D };
        settings.filterSettings.gradientAxis = 0;
    }

    private static void addThresholdFilter( Settings settings )
    {
        settings.filterSettings.filterTypes.add( FilterType.Threshold );
        settings.filterSettings.thresholdMin = 10.0D;
        settings.filterSettings.thresholdMax = 5000.0D;
    }
}
