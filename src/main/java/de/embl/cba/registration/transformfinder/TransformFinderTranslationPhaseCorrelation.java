package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.Logger;
import de.embl.cba.registration.Services;
import de.embl.cba.registration.filter.FilterSequence;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2Util;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.realtransform.RealTransform;

import net.imglib2.realtransform.Translation;
import net.imglib2.realtransform.Translation2D;
import net.imglib2.realtransform.Translation3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;

import java.util.*;


public class TransformFinderTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType< R > >
        implements TransformFinder
{

    private final double[] maximalTranslations;
    private FilterSequence filterSequence;

    private double[] translation;

    private double crossCorrelation;
    private boolean interpolateCrossCorrelation;
    private boolean subpixelAccuracy;
    private int nHighestPeaks;
    private int numDimensions;
    private int[] extension;
    private long minOverlap;
    private long[] subSampling;


    TransformFinderTranslationPhaseCorrelation( TransformFinderSettings settings )
    {
        this.maximalTranslations = settings.maximalTranslations;
    }

    public RealTransform findTransform(
             RandomAccessibleInterval fixedRAI,
             RandomAccessible movingRA,
             FilterSequence filterSequence )
    {

        configureAlgorithm( fixedRAI, filterSequence );

        RandomAccessibleInterval movingRAI = Views.interval( movingRA, fixedRAI );
        RandomAccessibleInterval filteredFixedRAI = filterSequence.apply( fixedRAI );
        RandomAccessibleInterval filteredMovingRAI = filterSequence.apply( movingRAI );

        Services.uiService.show( filteredFixedRAI );

        final RandomAccessibleInterval< FloatType > pcm = calculatePCM( filteredFixedRAI, filteredMovingRAI );
        final List< PhaseCorrelationPeak2 > shiftPeaks = getShiftPeaks( pcm, filteredFixedRAI, filteredMovingRAI );
        PhaseCorrelationPeak2 shiftPeak = getFirstShiftWithinAllowedRange( shiftPeaks );

        RealTransform transform = createTranslationTransform( shiftPeak );

        return transform;

    }

    private RealTransform createTranslationTransform( PhaseCorrelationPeak2 shiftPeak )
    {
        translation = new double[ numDimensions ];

        if ( shiftPeak != null || ! Double.isInfinite( shiftPeak.getCrossCorr() ) )
        {
            if ( shiftPeak.getSubpixelShift() == null )
                shiftPeak.getShift().localize( translation );
            else
                shiftPeak.getSubpixelShift().localize( translation );

            correctForSubSampling();

            logShift( shiftPeak );

            crossCorrelation = shiftPeak.getCrossCorr();
        }
        else
        {
            Logger.debug( "No sensible translations found => returning zero translations.\n" +
                    "Consider increasing the maximal translations range." );

            crossCorrelation = Double.NaN;
        }

        for ( int d = 0; d < translation.length; ++d )
        {
            if ( Math.abs( translation[ d  ] ) > maximalTranslations[ d ] )
            {
                translation[ d ] = maximalTranslations[ d ] * Math.signum( translation[ d ] );
                Logger.info( "Shift was larger than allowed => restricting to allowed range." );

            }
        }

        RealTransform transform = getTranslationAsRealTransform();

        return transform;

    }

    private RealTransform getTranslationAsRealTransform()
    {
        if ( translation.length == 2 )
        {
            return new Translation2D( translation );
        }
        else if ( translation.length == 3 )
        {
            return new Translation3D( translation );
        }
        else
        {
            // TODO: still bug with concatenate?
            return new Translation( translation );
        }
    }

    private void logShift( PhaseCorrelationPeak2 shiftPeak )
    {
        for ( double s : translation )
        {
            Logger.debug( "translations "+ s );
        }
        Logger.debug("phase-correlation " + shiftPeak.getPhaseCorr());
    }

    private void correctForSubSampling()
    {
        for ( int d = 0; d < translation.length; ++d )
        {
            translation[ d ] *= subSampling[ d ];
        }
    }

    private List<PhaseCorrelationPeak2> getShiftPeaks( RandomAccessibleInterval< FloatType > pcm, RandomAccessibleInterval img1, RandomAccessibleInterval img2 )
    {

        List<PhaseCorrelationPeak2> peaks = PhaseCorrelation2Util.getPCMMaxima( pcm, Services.executorService, nHighestPeaks, subpixelAccuracy);
        PhaseCorrelation2Util.expandPeakListToPossibleShifts(peaks, pcm, img1, img1);

        return peaks;


        /*return PhaseCorrelation2.getShift(
                pcm,
                img1,
                img1,
                nHighestPeaks,
                minOverlap,
                subpixelAccuracy,
                interpolateCrossCorrelation,
                Services.executorService  );*/

    }

    private PhaseCorrelationPeak2 getFirstShiftWithinAllowedRange( List< PhaseCorrelationPeak2 > peaks )
    {

        for ( PhaseCorrelationPeak2 peak : peaks )
        {
            boolean isOK = true;

            for ( int d = 0; d < numDimensions; ++d )
            {
                double shift = peak.getSubpixelShift().getDoublePosition( d );
                double allowedShift = maximalTranslations[ d ] / subSampling[ d ];

                if ( Math.abs( shift ) > allowedShift )
                {
                    isOK = false;
                    continue;
                }

            }

            if ( isOK )
            {
                return peak;
            }

        }

        Logger.info( "No shift within allowed range was found; returning shift with highest phase-correlation." );
        return peaks.get( 0 );

    }

    private RandomAccessibleInterval< FloatType > calculatePCM( RandomAccessibleInterval filteredFixedRAI, RandomAccessibleInterval filteredMovingRAI )
    {
        return PhaseCorrelation2.calculatePCM(
                filteredFixedRAI,
                filteredMovingRAI,
                extension,
                new ArrayImgFactory< FloatType >(),
                new FloatType(),
                new ArrayImgFactory< ComplexFloatType >(),
                new ComplexFloatType(),
                Services.executorService );
    }

    private void configureAlgorithm( RandomAccessibleInterval fixedRAI, FilterSequence filterSequence )
    {
        this.filterSequence = filterSequence;
        subSampling = filterSequence.subSampling();
        numDimensions = fixedRAI.numDimensions();
        nHighestPeaks = 5;
        subpixelAccuracy = true;
        interpolateCrossCorrelation = true;

        final int extensionValue = 10;
        extension = new int[ numDimensions ];
        Arrays.fill( extension, extensionValue );

        minOverlap = 1;
        for ( int d = 0; d < numDimensions; ++d )
        {
            minOverlap *= ( fixedRAI.dimension( d ) - maximalTranslations[ d ] );
        }
    }

    public double[] getTranslation()
    {
        return translation;
    }

    public double getCrossCorrelation()
    {
        return crossCorrelation;
    }


}
