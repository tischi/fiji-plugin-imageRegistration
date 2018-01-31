import de.embl.cba.registration.*;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.util.MetaImage;
import net.imagej.DefaultDatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Register3D7rebor
{

    public static String LOCAL_FOLDER = "/Users/tischer/Documents/fiji-plugin-imageRegistration--data/7rebor";

    public static void main(final String... args) throws Exception
    {
        String path = LOCAL_FOLDER + "/Registration_Test_GitHub_ND_reg_plugin_240118_RL.tif";

        Services.executorService = Executors.newFixedThreadPool( 4 );
        Services.datasetService = new DefaultDatasetService();

        final ImageJ ij = new ImageJ();
        Services.ij = ij;

        ij.ui().showUI();

        MetaImage input = Readers.openUsingImageJ1( path );

        Viewers.showRAIUsingImageJFunctions( input.rai, input.axisTypes, input.title );

        Settings settings = createSettings( input.rai, input.axisTypes );

        Registration registration = new Registration( settings );

        registration.run();

        registration.logTransformations();

        MetaImage transformed = registration.getTransformedImage( OutputIntervalSizeType.InputImage );

        Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions, transformed.axisOrder );

        //Viewers.showRAIUsingImageJFunctions( transformed.rai, transformed.axisTypes, transformed.title );

        //Writers.saveMetaImageUsingScifio( transformed, path + "--reg.ics" );
        Writers.saveMetaImageUsingScifio( transformed,  new File(path + "--reg.tif") );

    }


    private static Settings createSettings( RandomAccessibleInterval rai, ArrayList< AxisType > axisTypes )
    {
        Settings settings = new Settings( );

        settings.rai = rai;
        settings.axisTypes = axisTypes;

        settings.registrationAxisTypes = new ArrayList<>(  );
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( rai );
        long[] max = Intervals.maxAsLongArray( rai );

        min[ 0 ] = 840; max[ 0 ] = min[ 0 ] + 30;
        min[ 1 ] = 210; max[ 1 ] = min[ 1 ] + 80;
        min[ 2 ] = 0; max[ 2 ] = 0;
        min[ 4 ] = 0; max[ 4 ] = 1;

        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalSizeType = OutputIntervalSizeType.InputImage;

        settings.filterSettings = new FilterSettings();
        settings.filterSettings.filterTypes = new ArrayList<>(  );
        settings.filterSettings.filterTypes.add( FilterType.SubSample );
        settings.filterSettings.subSampling = new long[]{ 10L, 10L, 1L  };
        settings.filterSettings.filterTypes.add( FilterType.Threshold );
        //settings.filterSettings.filterTypes.add( FilterType.DifferenceOfGaussian );
        settings.filterSettings.thresholdMin = 33000.0D;
        settings.filterSettings.thresholdMax = 50000.0D;
        //settings.filterSettings.gaussSigmaSmaller = new double[]{ 2.0D };
        //settings.filterSettings.gaussSigmaLarger = new double[]{ 6.0D };

        settings.setAxes();

        settings.transformSettings = new TransformSettings();
        //settings.transformSettings.maximalTranslations = new double[] { 3000.0D };
        settings.transformSettings.transformFinderType = TransformFinderType.Translation__PhaseCorrelation;
        settings.transformSettings.maximalRotations = new double[] { 0.0D };

        return settings;
    }
}
