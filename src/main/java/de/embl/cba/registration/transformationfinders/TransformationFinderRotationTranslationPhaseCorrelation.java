package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.ImageRegistration;
import de.embl.cba.registration.ImageRegistrationUtils;
import de.embl.cba.registration.filter.ImageFilter;
import net.imglib2.FinalInterval;
import net.imglib2.FinalRealInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.*;
import java.util.concurrent.ExecutorService;

public class TransformationFinderRotationTranslationPhaseCorrelation
        < R extends RealType< R > & NativeType < R > >
        implements TransformationFinder {

    Double[] maximalTranslations;
    Double[] maximalRotations;
    FinalRealInterval rotationInterval;

    double rotationStep = 1.0D;
    private RandomAccessibleInterval fixedRAI;
    private RandomAccessible movingRA;
    private TransformationFinder transformationFinderTranslationPhaseCorrelation;


    TransformationFinderRotationTranslationPhaseCorrelation( Map< String, Object > transformationParameters )
    {
        this.maximalTranslations =
                ( Double[] ) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_TRANSLATIONS );

        this.maximalRotations =
                ( Double[] ) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_ROTATIONS );

        double[] min = Arrays.stream( maximalRotations ).mapToDouble( x -> -x ).toArray();
        double[] max = Arrays.stream( maximalRotations ).mapToDouble( x -> x ).toArray();
        this.rotationInterval = new FinalRealInterval( min, max );


        // get a transformationFinder for the translation

        Map< String, Object > transformationTranslationParameters
                = new HashMap<>( transformationParameters );

        transformationTranslationParameters.put(
                TransformationFinderParameters.TRANSFORMATION_FINDER_TYPE,
                TransformationFinderType.Translation__PhaseCorrelation );

        this.transformationFinderTranslationPhaseCorrelation
                = TransformationFinderFactory.create( transformationTranslationParameters );

    }

    public RealTransform findTransform(
             RandomAccessibleInterval fixedRAI,
             RandomAccessible movingRA )
    {

        this.fixedRAI = fixedRAI;
        this.movingRA = movingRA;

        // Recursively loop through all possible rotations
        // - calling the TransformationFinderTranslationPhaseCorrelation
        // - keeping the rotations, translations and x-correlations in a list
        // return the best one


        return null;
    }


    //    https://de.wikipedia.org/wiki/Eulersche_Winkel#Standard-x-Konvention_(z,_x%E2%80%B2,_z%E2%80%B3)

    private void testRotation(
            Double[] rotations,
            ArrayList< ArrayList< Double[] > > rotationsTranslationsXCorrList
    )
    {

        for ( int d = 0; d < rotations.length; ++d )
        {
            if ( rotations[ d ] == Double.NaN )
            {
                for ( Double r = rotationInterval.realMin( d );
                      r <= rotationInterval.realMax( d );
                      r += rotationStep )
                {
                    rotations[ d ] = r;
                    testRotation( rotations,
                            rotationsTranslationsXCorrList );
                }
            }

        }

        // all rotations are set => compute best shift and add to list


        AffineTransform3D rotation3D = new AffineTransform3D();

        for ( int d = 0; d < rotations.length; ++d )
        {
            rotation3D.rotate( d, rotations[ d ] );
        }

        RandomAccessible< R > rotatedMovingRA = ImageRegistrationUtils.getTransformedRA( movingRA, rotation3D );


        transformationFinderTranslationPhaseCorrelation.findTransform( fixedRAI, rotatedMovingRA );

    }


    private RandomAccessible< R > getRotatedView(
            RandomAccessible< R > ra,
            Double[] rotations )
    {
        return null;
    }



}
