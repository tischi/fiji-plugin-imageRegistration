package embl.almf.registration;

import embl.almf.IntervalUtils;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.Translation;

import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public abstract class TranslationPhaseCorrelation {


    public static RealTransform compute( RandomAccessibleInterval fixedRAI,
                                         RandomAccessible movingRA,
                                         long[] searchRadius,
                                         ExecutorService service)
    {
        final int n = fixedRAI.numDimensions();

        final int numPeaksToCheck = 5;
        final long minOverlap = 10;
        final int extensionValue = 10;
        final boolean doSubpixel = true;
        final boolean interpolateCrossCorrelation = true;

        FinalInterval movingInterval = IntervalUtils.expand( fixedRAI, searchRadius );

        final int[] extension = new int[ fixedRAI.numDimensions() ];
        Arrays.fill( extension, extensionValue );

        RandomAccessibleInterval movingRAI = Views.interval( movingRA, movingInterval );

        final RandomAccessibleInterval< FloatType > pcm =
                PhaseCorrelation2.calculatePCM(
                        fixedRAI,
                        movingRAI,
                        extension,
                        new ArrayImgFactory< FloatType >(),
                        new FloatType(),
                        new ArrayImgFactory< ComplexFloatType >(),
                        new ComplexFloatType(),
                        service );

        final PhaseCorrelationPeak2 shiftPeak =
                PhaseCorrelation2.getShift( pcm,
                        fixedRAI,
                        movingRAI,
                        numPeaksToCheck,
                        minOverlap,
                        doSubpixel,
                        interpolateCrossCorrelation,
                        service );

        //System.out.println( "Actual overlap of best shift is: " + shiftPeak.getnPixel() );

        // the best peak is horrible or no peaks were found at all, return null
        if ( shiftPeak == null || Double.isInfinite( shiftPeak.getCrossCorr() ) )
            return null;

        final double[] shift = new double[ n ];

        if ( shiftPeak.getSubpixelShift() == null )
            shiftPeak.getShift().localize( shift );
        else
            shiftPeak.getSubpixelShift().localize( shift );

        Translation translation = new Translation( shift );

        return translation;

    }

}
