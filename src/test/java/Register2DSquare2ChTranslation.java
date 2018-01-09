import de.embl.cba.registration.*;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.transformfinder.TransformSettings;
import de.embl.cba.registration.ui.Settings;
import de.embl.cba.registration.util.MetaImage;
import net.imagej.*;
import net.imagej.axis.AxisType;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Intervals;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Register2DSquare2ChTranslation
{
    public static String LOCAL_FOLDER = "/Users/tischer/Documents/fiji-plugin-imageRegistration";

    public static void main(final String... args) throws Exception
    {
        Services.executorService = Executors.newFixedThreadPool( 4 );
        Services.datasetService = new DefaultDatasetService();

        String path = LOCAL_FOLDER + "/src/test/resources/x90-y94-c2-t3--square--translation.tif";

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        Services.uiService = ij.ui();

        MetaImage input = Readers.openUsingDefaultSCIFIO( path, ij );
        Viewers.showImgPlusUsingUIService( input.imgPlus, ij.ui() );
        Viewers.showRAIUsingBdv( input.imgPlus, "input", 2, input.axisOrder );

        Settings settings = createSettings( input.rai, input.axisTypes );
        Registration registration = new Registration( settings );
        registration.run();
        registration.logTransformations();

        MetaImage transformed = registration.transformedImage( OutputIntervalSizeType.InputImage );

        Viewers.showRAIUsingBdv( transformed.rai, transformed.title, transformed.numSpatialDimensions,transformed.axisOrder );
        Viewers.showRAIAsImgPlusWithUIService( transformed.rai, Services.datasetService, transformed.axisTypes, transformed.title, Services.uiService );


        //DatasetIOService datasetIOService = new DefaultDatasetIOService();
        //DatasetService datasetService = new DefaultDatasetService();
        //datasetIOService.save( datasetService.create( transformed.rai  ),   )
        //Dataset dataset = Services.datasetService.create( Views.zeroMin( transformed.rai ) );


        //AxisType[] axisTypes = transformed.axisTypes.toArray( new AxisType[0]);

        //final ImageJ ij = new ImageJ();
        //ij.ui().showUI();
        //UIService uiService = new DefaultUIService();


        //Img img = OpService.run( net.imagej.ops.create.img.CreateImgFromRAI.class, transformed.rai );
        //ops.run(net.imagej.ops.copy.CopyRAI.class, img, rai);
        //Viewers.showImgPlusWithIJUI( imgPlus, ij );


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
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( rai );
        long[] max = Intervals.maxAsLongArray( rai );

        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalSizeType = OutputIntervalSizeType.InputImage;

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
