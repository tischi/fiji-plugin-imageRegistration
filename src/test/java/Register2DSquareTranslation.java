import de.embl.cba.registration.*;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.util.MetaImage;
import net.imagej.Dataset;
import net.imagej.DefaultDatasetService;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;
import org.scijava.ui.DefaultUIService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Register2DSquareTranslation
{
    public static String LOCAL_FOLDER = "/Users/tischer/Documents/fiji-plugin-imageRegistration";

    public static void main(final String... args) throws Exception
    {
        Services.executorService = Executors.newFixedThreadPool( 4 );
        Services.datasetService = new DefaultDatasetService();
        Services.uiService = new DefaultUIService();

        String path = LOCAL_FOLDER+"/src/test/resources/x90-y94-t3--square--translation.tif";

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        MetaImage input = Readers.openUsingDefaultSCIFIO( path, ij );
        Viewers.showImgPlusUsingIJUI( input.imgPlus, ij );

        Settings settings = createSettings( input.rai, input.axisTypes );
        Registration registration = new Registration( settings );
        registration.run();
        registration.logTransformations();

        MetaImage transformed = registration.transformedImage( OutputIntervalType.InputImageSize );
        Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions,transformed.axisOrder );


    }

    private static Dataset getDataset(  String path, ImageJ ij ) throws IOException
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
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Registration );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( rai );
        long[] max = Intervals.maxAsLongArray( rai );

        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalType = OutputIntervalType.InputImageSize;

        settings.filterSettings = new FilterSettings();
        settings.filterSettings.filterTypes = new ArrayList<>(  );
        settings.filterSettings.filterTypes.add( FilterType.SubSample );
        settings.filterSettings.filterTypes.add( FilterType.Threshold );
        settings.filterSettings.thresholdMin = 100;
        settings.filterSettings.thresholdMax = 300;

        settings.filterSettings.subSampling = new long[]{ 1L, 1L };

        settings.setAxes();

        settings.transformSettings = new TransformSettings();
        settings.transformSettings.maximalTranslations = new double[] { 5000, 5000.0D };
        settings.transformSettings.transformFinderType = TransformFinderType.Translation__PhaseCorrelation;
        settings.transformSettings.maximalRotations = new double[] { 0.0D };

        return settings;
    }
}
