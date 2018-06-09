import de.embl.cba.registration.Readers;
import de.embl.cba.registration.Viewers;
import de.embl.cba.registration.utils.MetaImage;
import net.imagej.ImageJ;

public class LoadVirtualStack
{

    public static String LOCAL_FOLDER = "/Users/tischer/Documents/fiji-plugin-imageRegistration";

    public static void main(final String... args) throws Exception
    {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String path = LOCAL_FOLDER+"/src/test/resources/x50-y50-t3--line--translation-rotation.tif";

        MetaImage input = Readers.openVirtualUsingImageJ1( path );
        Viewers.showRAIUsingImageJFunctions( input.rai, input.axisTypes, input.title );
        Viewers.showImagePlusWithImpShow( input.imagePlus );

    }
}
