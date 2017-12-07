package embl.almf;

import embl.almf.registration.TranslationPhaseCorrelation;
import net.imglib2.*;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.transform.InvertibleTransform;
import net.imglib2.view.Views;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRegistration {

    final static String TRANSLATION = "Translation";
    final static String MEAN_SQUARE_DIFFERENCE = "Mean square difference";
    final static String MOVING = "Moving";
    final static String FIXED = "Fixed";

    public final static int SEQUENCE_DIM = 0;
    public final static int TRANSFORMABLE_DIM = 1;
    public final static int FIXED_DIM = 2;

    RandomAccessibleInterval image;

    FinalInterval interval;
    int[] dimensionTypes;
    FinalInterval transformableDimensionsInterval;
    int numDimensions;
    private long[] searchRadii;

    int sequenceDim;
    long sRef;
    long ds = 1;

    String registrationType;
    String registrationMethod;
    String referenceType;

    ExecutorService service;

    int numThreads;

    ImageRegistration( RandomAccessibleInterval input )
    {
        this.image = input;
        this.numDimensions = input.numDimensions();

        // set defaults
        numThreads = 1;
        service = Executors.newFixedThreadPool( numThreads );
        referenceType = MOVING;

    }

    public void setNumThreads( int n )
    {
        numThreads = n;
        service = Executors.newFixedThreadPool( numThreads );
    }

    public void setDimensionTypesAndInterval( int[] dimensionTypes,
                                              FinalInterval interval )
    {

        this.dimensionTypes = dimensionTypes;

        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            if ( dimensionTypes[ d ] == SEQUENCE_DIM )
            {
                // TODO: assert that the sequenceDimension occurs only once.
                sequenceDim = d;
                sRef = interval.min( d );
            }
        }

        this.interval = interval;
        setTransformableDimensionsInterval( );

    }

    public void setSearchRadii( long[] searchRadii )
    {
        assert searchRadii.length == transformableDimensionsInterval.numDimensions();

        this.searchRadii = searchRadii;
    }



    public <T extends InvertibleRealTransform & Concatenable< T > > void computeTransforms()
    {

        RandomAccessibleInterval fixedRAI;
        RandomAccessible movingRA;

        T relativeTransformation = null;
        T absoluteTransformation = null;

        for ( long s = interval.min( sequenceDim );
              s <= interval.max( sequenceDim );
              s += ds )
        {
            fixedRAI = getFixedRAI( s, absoluteTransformation );
            movingRA = getMovingRA( s, absoluteTransformation );

            //ImageJFunctions.show( fixedRAI );

            relativeTransformation = (T) TranslationPhaseCorrelation
                    .compute( fixedRAI, movingRA, searchRadii, service );

            if ( s != interval.min( sequenceDim ) )
            {
                absoluteTransformation = ( T ) relativeTransformation
                        .concatenate( absoluteTransformation );
            }
            else
            {
                absoluteTransformation = relativeTransformation;
            }

        }

    }


    private RandomAccessibleInterval getTransformableDimensionsRAI( long s )
    {

        long[] min = new long[ numDimensions ];
        long[] max = new long[ numDimensions ];

        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            switch ( dimensionTypes[d] )
            {
                case SEQUENCE_DIM:
                    min[ d ] = s;
                    max[ d ] = s;
                    break;
                case FIXED_DIM:
                    min[ d ] = interval.min( d );
                    max[ d ] = interval.max( d );
                    break;
                case TRANSFORMABLE_DIM:
                    min[ d ] = image.min( d );
                    max[ d ] = image.max( d );
                    break;
            }
        }


        FinalInterval interval = new FinalInterval( min, max );

        RandomAccessibleInterval rai =
                Views.dropSingletonDimensions(
                    Views.interval( image, interval )
                );

        return rai;

    }

    private RandomAccessibleInterval getFixedRAI(
            long s,
            InvertibleRealTransform transform )
    {
        RandomAccessibleInterval rai;

        if ( s == sRef || referenceType.equals( FIXED ) )
        {
            rai = getTransformableDimensionsRAI( s );
            rai = Views.interval( rai, transformableDimensionsInterval );
        }
        else if ( s != sRef && referenceType.equals( MOVING ) )
        {
            rai = getTransformableDimensionsRAI( s );
            RandomAccessible ra = getTransformedRA( rai, transform );
            rai = Views.interval( ra, transformableDimensionsInterval );
        }
        else
        {
            return null; // should not occur
        }

        // crop out reference region


        return rai;
    }

    public void setTransformableDimensionsInterval()
    {
        int n = getNumTransformableDimensions();

        long[] min = new long[n];
        long[] max = new long[n];

        int i = 0;
        for ( int d = 0; d < dimensionTypes.length; ++d )
        {
            if ( dimensionTypes[d] == TRANSFORMABLE_DIM )
            {
                min[ i ] = interval.min( d );
                max[ i ] = interval.max( d );
                i++;
            }
        }

        transformableDimensionsInterval = new FinalInterval(  min, max );

    }

    private RandomAccessible getMovingRA(
            long s,
            InvertibleRealTransform transform )
    {

        RandomAccessibleInterval rai = getTransformableDimensionsRAI( s + ds );
        RandomAccessible ra;

        if ( s == sRef  )
        {
            ra = Views.extendMirrorSingle( rai );
        }
        else
        {
            ra = getTransformedRA( rai, transform );
        }

        return ra;

    }



    private RandomAccessible getTransformedRA(
            RandomAccessibleInterval rai,
            InvertibleRealTransform transform )
    {

        RealRandomAccessible rra
                = RealViews.transform(
                    Views.interpolate( Views.extendMirrorSingle( rai ),
                            new NLinearInterpolatorFactory() ),
                                transform );

        RandomAccessible ra = Views.raster( rra );

        return ra;
    }

    private int getNumTransformableDimensions( )
    {
        int n = 0;
        for ( int dimensionType : dimensionTypes )
        {
            if ( dimensionType == TRANSFORMABLE_DIM )
            {
               n++;
            }
        }

        return n;
    }

}

