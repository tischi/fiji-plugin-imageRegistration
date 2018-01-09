import de.embl.cba.registration.Corners;
import net.imglib2.FinalInterval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CornersTest
{
    public static void main( final String... args ) throws Exception
    {
        long[] min = new long[ ] { 0, 0, 0 };
        long[] max = new long[ ] { 10, 10, 20 };
        FinalInterval interval = new FinalInterval( min, max );

        List< long[] > corners = Corners.corners( interval );

        for ( long[] corner : corners )
        {
            System.out.print(  Arrays.toString( corner ) + "\n" );
        }


    }

}
