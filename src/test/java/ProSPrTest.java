import de.embl.cba.registration.ui.ProSPr;

import java.io.File;
import java.util.ArrayList;

public class ProSPrTest
{

    public static void main( String... args )
    {
        File directory = new File( "/Users/tischer/Documents/detlev-arendt-clem-registration--data" );

        ArrayList< String > geneList = ProSPr.getGeneList( directory );

        int a = 1;

    }

}
