package de.embl.cba.registration.utils;

import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

public abstract class RandomAccessibleIntervalUtils
{

    public static PhaseCorrelationPeak2 getMaximum( RandomAccessibleInterval rai )
    {
        // TODO: sub-pixel accuracy

        final Cursor< RealType > cursor = Views.iterable( rai ).localizingCursor();

        Localizable maximumLocalization = cursor.copyCursor();
        float maximumValue = Float.MIN_VALUE;

        while( cursor.hasNext() )
        {
            final float value = (float) cursor.next().getRealDouble();

            if ( value > maximumValue )
            {
                maximumValue = value;
                maximumLocalization = cursor.copyCursor();
            }
        }

        PhaseCorrelationPeak2 peak = new PhaseCorrelationPeak2( maximumLocalization, maximumValue  );

        return peak;
    }
}
