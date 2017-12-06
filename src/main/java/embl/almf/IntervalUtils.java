package embl.almf;

import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.Translation;
import net.imglib2.util.Intervals;

public class IntervalUtils {

    public static FinalRealInterval translateRealInterval(
            FinalRealInterval interval,
            Translation translation )
    {

        double[] minSource = Intervals.minAsDoubleArray( interval );
        double[] maxSource = Intervals.maxAsDoubleArray( interval );

        double[] minTarget = new double[ interval.numDimensions() ];
        double[] maxTarget = new double[ interval.numDimensions() ];

        translation.apply( minSource, minTarget );
        translation.apply( maxSource, maxTarget );

        FinalRealInterval translatedInterval =
                new FinalRealInterval( minTarget, maxTarget );

        return ( translatedInterval );
    }

    public static FinalRealInterval increment(
            FinalRealInterval realInterval,
            int dimension,
            double value )
    {

        double[] min = Intervals.minAsDoubleArray( realInterval );
        double[] max = Intervals.maxAsDoubleArray( realInterval );

        min[ dimension ] += value;
        max[ dimension ] += value;

        FinalRealInterval interval = new FinalRealInterval( min, max );

        return interval;

    }

    public static FinalRealInterval fixDimension(
            FinalRealInterval realInterval,
            int dimension,
            double value )
    {

        double[] min = Intervals.minAsDoubleArray( realInterval );
        double[] max = Intervals.maxAsDoubleArray( realInterval );

        min[ dimension ] = value;
        max[ dimension ] = value;

        FinalRealInterval interval = new FinalRealInterval( min, max );

        return interval;

    }

    public static FinalInterval realToInt(
            FinalRealInterval realInterval )
    {

        long[] min = doubleToLong( Intervals.minAsDoubleArray( realInterval ) );
        long[] max = doubleToLong( Intervals.maxAsDoubleArray( realInterval ) );

        FinalInterval interval =
                new FinalInterval( min, max );

        return interval;

    }


    private static long[] doubleToLong( double[] in )
    {
        long[] out = new long[ in.length ];
        for( int i = 0; i < in.length; ++i )
        {
            out[i] = (long) in[i];
        }

        return out;
    }


}
