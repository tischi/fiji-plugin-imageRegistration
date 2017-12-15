package de.embl.cba.registration.transformationfinders;

import de.embl.cba.registration.filter.ImageFilter;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class TransformationFinderRotationTranslationPhaseCorrelation
        implements TransformationFinder {

    Double[] maximalTranslations;
    Double[] maximalRotations;
    ImageFilter imageFilter;

    TransformationFinderRotationTranslationPhaseCorrelation(
            Map< String, Object > transformationParameters,
            ImageFilter imageFilter )
    {
        this.maximalTranslations =
                ( Double[] ) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_TRANSLATIONS );
        this.maximalRotations =
                ( Double[] ) transformationParameters
                        .get( TransformationFinderParameters.MAXIMAL_ROTATIONS );
        this.imageFilter = imageFilter;
    }

    public < R extends RealType< R > & NativeType< R > > RealTransform findTransform(
            RandomAccessibleInterval fixedRAI,
            RandomAccessible movingRA,
            ExecutorService service )
    {

        // Recursively loop through all possible rotations
        // - calling the TransformationFinderTranslationPhaseCorrelation
        // - keeping the rotations, translations and x-correlations in a list
        // return the best one



        return null;
    }


    private void testRotations(
            Map< Double[], Double[] >  rotationsTranslationsMap,
            Map< Integer, Long > dimensionCoordinateMap,
            Map< Long, T > transformations )
    {
        /*
        if ( dimensionCoordinateMap.containsValue( null ) )
        {
            List< RandomAccessibleInterval< R > > dimensionCoordinateRAIList = new ArrayList<>(  );

            for ( int d : dimensionCoordinateMap.keySet() )
            {
                if ( dimensionCoordinateMap.get( d ) == null )
                {
                    List < RandomAccessibleInterval< R > > sequenceCoordinateRAIList = new ArrayList<>(  );
                    for ( long c = inputRAI.min( d ); c <= inputRAI.max( d ); ++c )
                    {
                        Map< Integer, Long > newFixedDimensions =
                                new LinkedHashMap<>( dimensionCoordinateMap );

                        newFixedDimensions.put( d, c );

                        sequenceCoordinateRAIList.add(
                                createTransformedInputRAISequence(
                                        transformedSequenceMap,
                                        newFixedDimensions,
                                        transformations ) );

                    }
                    dimensionCoordinateRAIList.add(  Views.stack( sequenceCoordinateRAIList ) );
                }
            }

            return Views.stack( dimensionCoordinateRAIList );

        }
        else
        {
            List< RandomAccessibleInterval< R > > transformedRAIList = new ArrayList<>();

            for ( long s = inputRAI.min( sequenceAxisProperties.axis );
                  s <= inputRAI.max( sequenceAxisProperties.axis );
                  ++s )
            {
                if ( transformations.containsKey( s ) )
                {

                    transformedRAIList.add(
                            getTransformedRAI(
                                    s,
                                    transformations.get( s ),
                                    dimensionCoordinateMap,
                                    getTransformableDimensionsOutputInterval()
                            ) );
                }

            }

            // combine time-series of multiple channels into a channel stack
            RandomAccessibleInterval< R > transformedSequence = Views.stack( transformedRAIList );

            transformedSequenceMap.put( dimensionCoordinateMap, transformedSequence );

            return transformedSequence;
        }
        */
    }

}
