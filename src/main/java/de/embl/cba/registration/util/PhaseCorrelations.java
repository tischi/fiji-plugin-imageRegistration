package de.embl.cba.registration.util;

import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PhaseCorrelations
{

    public static List< PhaseCorrelationPeak2 > sensiblePeaks(
            List< PhaseCorrelationPeak2 > peaks,
            Dimensions pcmDims )
    {
        List<PhaseCorrelationPeak2> sensiblePeaks = new ArrayList<>(  );

        for ( PhaseCorrelationPeak2 peak : peaks )
        {
            boolean isSensible = true;
            for ( int d = 0; d < peak.getPcmLocation().numDimensions(); ++d )
            {
                if ( Math.abs( peak.getShift().getLongPosition( d ) ) > 0 )
                {
                    if ( peak.getPcmLocation().getLongPosition( d ) == peak.getShift().getLongPosition( d ) )
                    {
                        isSensible = false;
                        continue;
                    }
                }

                if ( Math.abs( peak.getShift().getLongPosition( d ) ) >= pcmDims.dimension( d ) )
                {
                    isSensible = false;
                    continue;
                }
            }

            if ( isSensible )
            {
                sensiblePeaks.add( peak );
            }
        }

        return sensiblePeaks;
    }


    public static ArrayList< PhaseCorrelationPeak2 > pcmMaximum( RandomAccessibleInterval< FloatType > pcm )
    {
        final Cursor< FloatType > cursor = Views.iterable( pcm ).localizingCursor();

        Localizable maximumLocalization = cursor.copyCursor();
        float maximumValue = Float.MIN_VALUE;

        while( cursor.hasNext() )
        {
            final float value = cursor.next().get();

            if ( value > maximumValue )
            {
                maximumValue = value;
                maximumLocalization = cursor.copyCursor();
            }
        }

        ArrayList< PhaseCorrelationPeak2 > peaks = asPeakList( maximumLocalization, maximumValue );

        return peaks;
    }

    private static ArrayList< PhaseCorrelationPeak2 > asPeakList( Localizable maximumLocalization, float maximumValue )
    {
        PhaseCorrelationPeak2 peak = new PhaseCorrelationPeak2( maximumLocalization, maximumValue  );
        ArrayList< PhaseCorrelationPeak2 > peaks = new ArrayList<>(  );
        peaks.add( peak );
        
        return peaks;
    }

    public static class ComparatorByPhaseCorrelation implements Comparator<PhaseCorrelationPeak2>
    {
        @Override
        public int compare(PhaseCorrelationPeak2 o1, PhaseCorrelationPeak2 o2) {
            int ccCompare = Double.compare(o1.getPhaseCorr(), o2.getPhaseCorr());
            if (ccCompare != 0){
                return ccCompare;
            } else {
                return (int)(o1.getnPixel() - o2.getnPixel());
            }
        }
    }


}
