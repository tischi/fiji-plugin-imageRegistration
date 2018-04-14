package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.filter.FilterSequence;
import de.embl.cba.registration.util.RandomAccessibleIntervalUtils;
import de.embl.cba.registration.util.Transforms;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TransformFinderTranslationMaximum< R extends RealType< R > & NativeType< R > > implements TransformFinder
{

    private int numDimensions;
    private double[] translation;
    RealTransform translationTransform;

    TransformFinderTranslationMaximum( TransformSettings settings ) {}

    @Override
    public RealTransform findTransform( RandomAccessibleInterval fixedRAI, RandomAccessible movingRA, FilterSequence filterSequence )
    {
        numDimensions = fixedRAI.numDimensions();

        RandomAccessibleInterval movingRAI = Views.interval( movingRA, fixedRAI );

        double[] fixedPeakPosition = getMaximumPosition( fixedRAI, filterSequence );
        double[] movingPeakPosition = getMaximumPosition( movingRAI, filterSequence );

        translation = getShift( fixedPeakPosition, movingPeakPosition );

        translationTransform = Transforms.translationAsRealTransform( translation );

        return translationTransform;
    }

    private double[] getShift( double[] vector1, double[] vector2 )
    {
        int n = vector1.length;
        double[] shift = new double[ n ];

        for ( int i = 0; i < n; ++i )
        {
            shift[ i ] = vector1[ i ] - vector2[ i ];
        }

        return  shift;
    }


    private double[] getMaximumPosition( RandomAccessibleInterval rai, FilterSequence filterSequence )
    {
        RandomAccessibleInterval processedRai = filterSequence.apply( rai );
        PhaseCorrelationPeak2 peak = RandomAccessibleIntervalUtils.getMaximum( processedRai );
        double[] peakPosition = new double[ numDimensions ];
        peak.getPcmLocation().localize( peakPosition );
        return peakPosition;
    }

    @Override
    public String asString()
    {
        String string = "";
        string += "Translation: ";
        string += Arrays.stream( translation ).mapToObj( Double::toString ).collect( Collectors.joining("," ) );

        return string;
    }
}