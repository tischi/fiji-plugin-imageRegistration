package de.embl.cba.registration;

import net.imglib2.FinalInterval;

import java.util.ArrayList;
import java.util.List;

public class Corners
{
    public static final int MIN = 0;
    public static final int MAX = 1;
    public static int[] MIN_MAX = new int[] { MIN, MAX };


    public static long[] corner( int[] minMax, FinalInterval interval )
    {
        assert minMax.length == interval.numDimensions();

        long[] corner = new long[ minMax.length ];

        for ( int d = 0; d < corner.length; ++d )
        {
            if ( minMax[ d ] == MIN )
            {
                corner[ d ] = interval.min( d );
            }
            else if ( minMax[ d ] == MAX )
            {
                corner[ d ] = interval.max( d );
            }
        }

        return corner;
    }


    public static List< long[] > corners( FinalInterval interval )
    {
        int[] minMaxArray = new int[ interval.numDimensions() ];
        ArrayList< long[] > corners = new ArrayList<>(  );
        setCorners( corners, interval, minMaxArray,-1 );
        return corners;
    }

    public static void setCorners( ArrayList< long[] > corners, FinalInterval interval, int[] minMaxArray, int d )
    {
        ++d;

        for ( int minMax : MIN_MAX )
        {
            minMaxArray[ d ] = minMax;

            if ( d == minMaxArray.length - 1 )
            {
                corners.add( corner( minMaxArray, interval ) );
            }
            else
            {
                setCorners( corners, interval, minMaxArray, d );
            }
        }

    }


}
