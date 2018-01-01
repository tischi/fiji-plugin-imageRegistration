import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.filter.FilterType;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import de.embl.cba.registration.ui.Settings;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.scijava.thread.ThreadService;
import scala.Int;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Executors;

public class Register2DEdgeAs1DProjection
{
    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration/test-data/2d_t_1ch_drift_synthetic_edge_noise_small.tif";
        final File file = new File( PATH );
        Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
        ij.ui().show(dataset);

        Settings settings = new Settings( );

        settings.registrationAxisTypes = new ArrayList<>(  );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Transformable );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        long[] min = Intervals.minAsLongArray( dataset );
        long[] max = Intervals.maxAsLongArray( dataset );
        settings.interval = new FinalInterval( min, max );

        settings.executorService = Executors.newFixedThreadPool( 4 );
        settings.outputIntervalType = OutputIntervalType.InputDataSize;

        settings.filterSettings.filterTypes = new ArrayList<>(  );
        settings.filterSettings.filterTypes.add( FilterType.None );

        settings.setAxes();

        settings.transformSettings.maximalTranslations = new double[] { 30.0D };
        settings.transformSettings.transformFinderType = TransformFinderType.Translation__PhaseCorrelation;
        settings.transformSettings.maximalRotations = new double[] { 0.0D };
        


    }
}
