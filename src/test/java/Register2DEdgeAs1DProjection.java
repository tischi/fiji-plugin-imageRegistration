import de.embl.cba.registration.*;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.ui.Settings;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

import org.scijava.app.DefaultStatusService;

public class Register2DEdgeAs1DProjection
{
    public static String LOCAL_FOLDER = "/Users/tischer/Documents/fiji-plugin-imageRegistration";

    public static void main(final String... args) throws Exception
    {

        String path = LOCAL_FOLDER+"/src/test/resources/x22_y25_t3_dy-6_dy+3.tif";

        final ImageJ ij = new ImageJ();

        //ij.ui().showUI();

        Dataset dataset = getDataset( path, ij );

        //ij.ui().show(dataset);

        Settings settings = getSettings( dataset );

        Services.executorService = Executors.newFixedThreadPool( 4 );

        Registration registration = new Registration( dataset, settings );

        registration.run();

        registration.logTransformations();

        Output output = registration.output();

    }

    private static Dataset getDataset(  String path, ImageJ ij ) throws IOException
    {
        final File file = new File( path );
        return ij.scifio().datasetIO().open(file.getPath());
    }

    private static Settings getSettings( Dataset dataset )
    {
        Settings settings = new Settings( );

        settings.dataset = dataset;

        settings.registrationAxisTypes = new ArrayList<>(  );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Transformable );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( dataset );
        long[] max = Intervals.maxAsLongArray( dataset );
        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalType = OutputIntervalType.InputDataSize;

        settings.filterSettings = new FilterSettings();
        settings.filterSettings.filterTypes = new ArrayList<>(  );
        settings.filterSettings.filterTypes.add( FilterType.SubSample );
        settings.filterSettings.subSampling = new long[]{ 1L };

        settings.setAxes();

        settings.transformSettings = new TransformSettings();
        settings.transformSettings.maximalTranslations = new double[] { 30.0D };
        settings.transformSettings.transformFinderType = TransformFinderType.Translation__PhaseCorrelation;
        settings.transformSettings.maximalRotations = new double[] { 0.0D };



        return settings;
    }
}
