package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.ImageRegistrationUtils;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.*;

public class TransformationFinderRotationTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType < R > >
        implements TransformationFinder {

    FinalRealInterval rotationInterval;
    double rotationStep = 1.0D;

    private RandomAccessibleInterval fixedRAI;
    private RandomAccessible movingRA;
    private TransformationFinderTranslationPhaseCorrelation transformationFinderTranslationPhaseCorrelation;

    private static final String CROSS_CORRELATION = "CrossCorrelation";
    private static final String TRANSLATIONS = "Translations";
    private static final String ROTATIONS = "Rotations";

    TransformationFinderRotationTranslationPhaseCorrelation(
            Map< String, Object > transformationParameters )
    {

        double[] maximalRotations =
                ( double[] ) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_ROTATIONS );

        double[] minRotations = Arrays.stream( maximalRotations ).map( x -> -x ).toArray();
        double[] maxRotations = maximalRotations;
        this.rotationInterval = new FinalRealInterval( minRotations, maxRotations );

        // get a transformationFinder for the translation

        Map< String, Object > transformationTranslationParameters
                = new HashMap<>( transformationParameters );

        transformationTranslationParameters.put(
                TransformationFinderParameters.TRANSFORMATION_FINDER_TYPE,
                TransformationFinderType.Translation__PhaseCorrelation );

        this.transformationFinderTranslationPhaseCorrelation
                = new TransformationFinderTranslationPhaseCorrelation( transformationParameters );

    }

    public RealTransform findTransform(
             RandomAccessibleInterval fixedRAI,
             RandomAccessible movingRA )
    {

        this.fixedRAI = fixedRAI;
        this.movingRA = movingRA;

        // Recursively loop through all possible rotations and compute best translation
        ArrayList< Map< String, Object > > rotationsTranslationsXCorrList = new ArrayList<>(  );

        double[] rotations = new double[ rotationInterval.numDimensions() ];
        Arrays.fill( rotations, Double.MAX_VALUE );

        computeCrossCorrelationAndTranslation( rotations, rotationsTranslationsXCorrList );

        // go through list and and find the best transformation

        double largestCrossCorrelation = Double.MIN_VALUE;
        double[] bestRotations = null;
        double[] bestTranslations = null;

        for ( Map< String, Object > rotationsTranslationsXCorr : rotationsTranslationsXCorrList )
        {
            if ( (double) rotationsTranslationsXCorr.get( CROSS_CORRELATION ) > largestCrossCorrelation )
            {
                largestCrossCorrelation = (double) rotationsTranslationsXCorr.get( CROSS_CORRELATION );
                bestRotations = (double[]) rotationsTranslationsXCorr.get( ROTATIONS );
                bestTranslations = (double[]) rotationsTranslationsXCorr.get( TRANSLATIONS );
            }
        }



        return null;
    }

    private void computeCrossCorrelationAndTranslation(
            double[] rotations,
            ArrayList< Map< String, Object > > rotationsTranslationsXCorrList )
    {

        for ( int d = 0; d < rotations.length; ++d )
        {
            if ( rotations[ d ] == Double.MAX_VALUE)
            {
                for ( double r = rotationInterval.realMin( d );
                      r <= rotationInterval.realMax( d );
                      r += rotationStep )
                {
                    rotations[ d ] = r;
                    computeCrossCorrelationAndTranslation( rotations,
                            rotationsTranslationsXCorrList );
                }
            }

        }

        // all rotations are set => compute best translation and add to list

        // rotate
        RandomAccessible< R > rotatedMovingRA;

        if ( rotations.length == 1 )
        {
            AffineTransform2D rotation2D = new AffineTransform2D();
            rotation2D.rotate( rotations[ 0 ] );
            rotatedMovingRA = ImageRegistrationUtils.getRAasTransformedRA( movingRA, rotation2D );
        }
        else if ( rotations.length == 3)
        {
            AffineTransform3D rotation3D = new AffineTransform3D();
            for ( int d = 0; d < rotations.length; ++d )
            {
                rotation3D.rotate( d, rotations[ d ] );
            }
            rotatedMovingRA = ImageRegistrationUtils.getRAasTransformedRA( movingRA, rotation3D );
        }
        else
        {
            // TODO: throw error (or even better: this should not happen)
            rotatedMovingRA = null;
        }

        // find best translation for this rotation
        transformationFinderTranslationPhaseCorrelation.findTransform( fixedRAI, rotatedMovingRA );

        // add result to list
        Map< String, Object > rotationsTranslationsXCorr = new HashMap<>(  );

        rotationsTranslationsXCorr.put(
                ROTATIONS,
                rotations );

        rotationsTranslationsXCorr.put(
                TRANSLATIONS,
                transformationFinderTranslationPhaseCorrelation.getTranslation() );

        rotationsTranslationsXCorr.put(
                CROSS_CORRELATION,
                transformationFinderTranslationPhaseCorrelation.getCrossCorrelation() );

        rotationsTranslationsXCorrList.add( rotationsTranslationsXCorr );


    }


}
