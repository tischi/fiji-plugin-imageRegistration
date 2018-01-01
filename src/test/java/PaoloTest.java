import de.embl.cba.registration.Axes;
import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.filter.FilterSettings;
import de.embl.cba.registration.transformfinder.TransformFinderSettings;
import de.embl.cba.registration.ui.RegistrationPlugin;
import de.embl.cba.registration.ui.Settings;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.FinalInterval;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class PaoloTest
{

    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String PATH;
        PATH = "/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop.tif";
        PATH = "/Users/tischer/Documents/paolo-ronchi--em-registration/chemfix_O6_crop--z1-5.tif";

        final File file = new File( PATH );
        Dataset dataset = ij.scifio().datasetIO().open(file.getPath());
        ij.ui().show(dataset);

        Settings settings = new Settings( );

        settings.registrationAxisTypes = new ArrayList<>(  );
        settings.registrationAxisTypes.add( RegistrationAxisType.Other );
        settings.registrationAxisTypes.add( RegistrationAxisType.Transformable );
        settings.registrationAxisTypes.add( RegistrationAxisType.Sequence );

        /*
        public Map< String, Object > filterParameters;
        public TransformFinderSettings transformSettings;
        public FilterSettings filterSettings;
        public OutputIntervalType outputIntervalType;
        public FinalInterval interval;
        public ExecutorService executorService;
        public Axes axes;*/

    }
}
