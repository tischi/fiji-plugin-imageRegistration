package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.PackageExecutorService;
import de.embl.cba.registration.Logger;
import de.embl.cba.registration.Services;
import de.embl.cba.registration.filter.FilterSequence;
import de.embl.cba.registration.filter.ImageFilter;
import de.embl.cba.registration.utils.Duplicator;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
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
import java.util.concurrent.ExecutorService;


public class TransformFinderTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType< R > >
        implements TransformFinder
{

    private final double[] maximalTranslations;
    private final ExecutorService service;

    private FilterSequence filterSequence;

    private double[] translation;
    private double crossCorrelation;

    TransformFinderTranslationPhaseCorrelation( TransformFinderSettings settings )
    {
        this.maximalTranslations = settings.maximalTranslations;
        this.service = PackageExecutorService.executorService;

    }

    public RealTransform findTransform(
             RandomAccessibleInterval fixedRAI,
             RandomAccessible movingRA,
             FilterSequence filterSequence)
    {

        // TODO: Clean up!

        Logger.debug("### TransformFinderTranslationPhaseCorrelation");

        final int n = fixedRAI.numDimensions();

        final int numPeaksToCheck = 5;

        // TODO: find a good strategy for the minOverlap!

        long minOverlap = 1;
        for ( int d = 0; d < n; ++d )
        {
            minOverlap *= ( fixedRAI.dimension( d ) - maximalTranslations[ d ] );
        }

        final int extensionValue = 0; //(int) searchRadius[ 0 ];
        final boolean doSubpixel = true;
        final boolean interpolateCrossCorrelation = true;

        final int[] extension = new int[ n ];
        Arrays.fill( extension, extensionValue );

        RandomAccessibleInterval movingRAI = Views.interval( movingRA, fixedRAI );
        RandomAccessibleInterval filteredFixedRAI = filterSequence.apply( fixedRAI );
        RandomAccessibleInterval filteredMovingRAI = filterSequence.apply( movingRAI );

        //ImageJFunctions.show( finalFixedRAI );
        //ImageJFunctions.show( finalMovingRAI );

        // compute best shift
        //
        final RandomAccessibleInterval< FloatType > pcm =
                PhaseCorrelation2.calculatePCM(
                        filteredFixedRAI,
                        filteredMovingRAI,
                        extension,
                        new ArrayImgFactory< FloatType >(),
                        new FloatType(),
                        new ArrayImgFactory< ComplexFloatType >(),
                        new ComplexFloatType(),
                        Services.executorService );

        final PhaseCorrelationPeak2 shiftPeak =
                PhaseCorrelation2.getShift(
                        pcm,
                        filteredFixedRAI,
                        filteredMovingRAI,
                        numPeaksToCheck,
                        minOverlap,
                        doSubpixel,
                        interpolateCrossCorrelation,
                        Services.executorService  );

        //System.out.println( "Actual overlap of best shift is: " + shiftPeak.getnPixel() )

        translation = new double[ n ];

        if ( shiftPeak != null || ! Double.isInfinite( shiftPeak.getCrossCorr() ) )
        {
            if ( shiftPeak.getSubpixelShift() == null )
                shiftPeak.getShift().localize( translation );
            else
                shiftPeak.getSubpixelShift().localize( translation );

            for ( double s : translation )
            {
                Logger.debug( "translations "+ s );
            }
            Logger.debug("x-corr " + shiftPeak.getCrossCorr());

            crossCorrelation = shiftPeak.getCrossCorr();
        }
        else
        {
            Logger.debug(
                    "No sensible translations found => returning zero translations.\n" +
                    "Consider increasing the maximal translations range." );

            crossCorrelation = Double.NaN;
        }

        for ( int d = 0; d < translation.length; ++d )
        {
            if ( Math.abs( translation[ d  ] ) > maximalTranslations[ d ] )
            {
                translation[ d ] = maximalTranslations[ d ] * Math.signum( translation[ d ] );
                Logger.debug(
                        "Shift was larger than allowed => restricting to allowed range.");

            }
        }

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

    public double[] getTranslation()
    {
        return translation;
    }

    public double getCrossCorrelation()
    {
        return crossCorrelation;
    }


}