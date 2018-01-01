import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.ui.Settings;
import net.imagej.Dataset;
import net.imagej.ImageJ;

import java.io.File;
import java.util.ArrayList;

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
        public TransformSettings transformSettings;
        public FilterSettings filterSettings;
        public OutputIntervalType outputIntervalType;
        public FinalInterval interval;
        public ExecutorService executorService;
        public Axes axes;*/

    }
}
