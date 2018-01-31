import de.embl.cba.registration.ui.RegistrationPlugin;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

public class Register3D7reborShowingUI
{

    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String PATH = "/Users/tischer/Documents/fiji-plugin-imageRegistration--data/7rebor/Registration_Test_GitHub_ND_reg_plugin_240118_RL.tif";

        // Load and show data
        ImagePlus imp = IJ.openImage( PATH );
        imp.show();

        // invoke the plugin
        ij.command().run( RegistrationPlugin.class, true );
    }
}
