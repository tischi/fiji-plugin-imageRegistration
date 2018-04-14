import de.embl.cba.registration.ui.RegistrationPlugin;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class Paolo2DFibSemUI
{
    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        //String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration--data/7rebor/Registration_Test_GitHub_ND_reg_plugin_240118_RL.tif";
        //String path = "/Users/tischer/Desktop/crop_rotated-cropped.tif";
        String path = "/Users/tischer/Documents/fiji-plugin-imageRegistration/src/test/resources/crop_rotated-cropped-1-10.zip";

        // Load and show data
        ImagePlus imp = IJ.openImage( path );
        imp.show();

        // invoke the plugin
        ij.command().run( RegistrationPlugin.class, true );
    }
}
