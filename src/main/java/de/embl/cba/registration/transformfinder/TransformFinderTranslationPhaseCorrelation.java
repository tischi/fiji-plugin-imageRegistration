package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.Logger;
import de.embl.cba.registration.Services;
import de.embl.cba.registration.filter.FilterSequence;
import de.embl.cba.registration.util.PhaseCorrelations;
import de.embl.cba.registration.util.Transforms;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2Util;
import net.imglib2.algorithm.phasecorrelation.PhaseCorrelationPeak2;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.realtransform.RealTransform;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import net.imglib2.algorithm.phasecorrelation.PhaseCorrelation2;

import java.util.*;
import java.util.stream.Collectors;

// TODO: still bug with concatenate for Translation?

public class TransformFinderTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType< R > >
        implements TransformFinder
{



    private int numDimensions;
    private int[] extension;
    private long[] subSampling;

    // not used (from PCMMaximaFinder)
    private boolean interpolateCrossCorrelation;
    private boolean subpixelAccuracy;
    private int nHighestPeaks;
    private long minOverlap;

    // not used, because this is practically determined from the fixedRegion size
    private final double[] maximalTranslations = null;

    // output
    private double[] translation;
    RealTransform translationTransform;
    private double crossCorrelation;
    private double phaseCorrelation;
    private RandomAccessibleInterval< FloatType > pcm;


    TransformFinderTranslationPhaseCorrelation( TransformSettings settings ) {}

    public RealTransform findTransform( RandomAccessibleInterval fixedRAI, RandomAccessible movingRA, FilterSequence filterSequence )
    {
        configureTransformFindingSettings( fixedRAI, filterSequence );

        RandomAccessibleInterval movingRAI = Views.interval( movingRA, fixedRAI );
        RandomAccessibleInterval img1 = filterSequence.apply( fixedRAI );
        RandomAccessibleInterval img2 = filterSequence.apply( movingRAI );

        calculatePCM( img1, img2 );
        determineTranslationFromPCM( img1, img2 );
        setTranslationTransform( );

        return translationTransform;
    }

    private void determineTranslationFromPCM( RandomAccessibleInterval img1, RandomAccessibleInterval img2 )
    {
        List< PhaseCorrelationPeak2 > peaks = PhaseCorrelations.pcmMaximum( pcm ); //  peaks = PhaseCorrelation2Util.getPCMMaxima( pcm, nHighestPeaks, subpixelAccuracy );
        PhaseCorrelation2Util.expandPeakListToPossibleShifts(peaks, pcm, img1, img2);
        List<PhaseCorrelationPeak2> sensiblePeaks = PhaseCorrelations.sensiblePeaks( peaks, pcm, img1, img2 );
        Collections.sort( sensiblePeaks, Collections.reverseOrder( new PhaseCorrelations.ComparatorByPhaseCorrelation() ) );
        PhaseCorrelationPeak2 peak = getFirstShiftWithinAllowedRange( pcm, sensiblePeaks );
        setTranslationFromShiftPeak( peak );
    }

    private void setTranslationTransform()
    {
        translationTransform = Transforms.translationAsRealTransform( translation );
    }

    private void setTranslationFromShiftPeak( PhaseCorrelationPeak2 shiftPeak )
    {
        translation = new double[ numDimensions ];

        if ( shiftPeak != null )
        {
            if ( shiftPeak.getSubpixelShift() == null )
                shiftPeak.getShift().localize( translation );
            else
                shiftPeak.getSubpixelShift().localize( translation );

            correctTranslationForSubSampling();

            crossCorrelation = shiftPeak.getCrossCorr();
            phaseCorrelation = shiftPeak.getPhaseCorr();

        }
        else
        {
            Logger.debug( "No sensible translation found => returning zero translation." );

            crossCorrelation = Double.MIN_VALUE;
            phaseCorrelation = Double.MIN_VALUE;
            Arrays.fill( translation, 0.0 );
        }
    }

    private void modifyTranslationToBeWithinAllowedRange()
    {
        for ( int d = 0; d < translation.length; ++d )
        {
            if ( Math.abs( translation[ d  ] ) > maximalTranslations[ d ] )
            {
                translation[ d ] = maximalTranslations[ d ] * Math.signum( translation[ d ] );
                Logger.debug( "Shift was larger than allowed => restricting to allowed range." );

            }
        }
    }

    private void correctTranslationForSubSampling()
    {
        for ( int d = 0; d < translation.length; ++d )
        {
            translation[ d ] *= subSampling[ d ];
        }
    }

    private PhaseCorrelationPeak2 getFirstShiftWithinAllowedRange( RandomAccessibleInterval< FloatType > pcm ,
                                                                   List< PhaseCorrelationPeak2 > peaks )
    {
        // Services.uiService.show( pcm );

        for ( PhaseCorrelationPeak2 peak : peaks )
        {
            boolean isOK = true;

            for ( int d = 0; d < numDimensions; ++d )
            {
                double shift = peak.getShift().getDoublePosition( d );
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

        return null;

    }

    private void calculatePCM( RandomAccessibleInterval filteredFixedRAI, RandomAccessibleInterval filteredMovingRAI )
    {
        pcm = PhaseCorrelation2.calculatePCM(
                filteredFixedRAI,
                filteredMovingRAI,
                extension,
                new ArrayImgFactory< FloatType >(),
                new FloatType(),
                new ArrayImgFactory< ComplexFloatType >(),
                new ComplexFloatType(),
                Services.executorService );
    }

    private void configureTransformFindingSettings( RandomAccessibleInterval fixedRAI, FilterSequence filterSequence )
    {
        numDimensions = fixedRAI.numDimensions();
        configurePhaseCorrelation();
        configurePCMMaximaFinder( fixedRAI );
        setSubSampling( filterSequence );

    }

    private void configurePhaseCorrelation()
    {
        final int extensionValue = 10; // TODO: what makes sense here?
        extension = new int[ numDimensions ];
        Arrays.fill( extension, extensionValue );
    }

    private void setSubSampling( FilterSequence filterSequence )
    {
        if ( filterSequence.subSampling() != null )
        {
            subSampling = filterSequence.subSampling();
        }
        else
        {
            long[] subSampling = new long[ numDimensions ];
            Arrays.fill( subSampling, 1L );
            this.subSampling = subSampling;
        }
    }

    private void configurePCMMaximaFinder( RandomAccessibleInterval fixedRAI )
    {
        nHighestPeaks = 20;
        subpixelAccuracy = true;
        interpolateCrossCorrelation = true;
        minOverlap = 1;
        for ( int d = 0; d < numDimensions; ++d )
        {
            minOverlap *= ( fixedRAI.dimension( d ) - maximalTranslations[ d ] );
        }
    }

    public double[] translation()
    {
        return translation;
    }

    public double crossCorrelation()
    {
        return crossCorrelation;
    }

    public double phaseCorrelation()
    {
        return phaseCorrelation;
    }


    public String toString()
    {
        String string = "";
        string += "Translation: ";
        string += Arrays.stream( translation ).mapToObj( Double::toString ).collect( Collectors.joining("," ) );
        string += "\n";
        string += "Phase-correlation: ";
        string += phaseCorrelation;

        return string;
    }

}
