package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public class ImageFilterDogThreshold
        < R extends RealType< R > & NativeType< R > >
        implements ImageFilter< R, BitType > {

    ImageFilter imageFilterDog;
    ImageFilter imageFilterThreshold;

    public ImageFilterDogThreshold( Map< String, Object > parameters )
    {
        parameters.put( ImageFilterParameters.FILTER_TYPE, ImageFilterType.DifferenceOfGaussian );
        this.imageFilterDog = ImageFilterFactory.create( ImageFilterParameters.FILTER_TYPE, parameters );

        parameters.put( ImageFilterParameters.FILTER_TYPE, ImageFilterType.Threshold );
        this.imageFilterThreshold = ImageFilterFactory.create(  parameters );
    }

    @Override
    public RandomAccessibleInterval< BitType > apply( RandomAccessibleInterval< R > source )
    {

        RandomAccessibleInterval dog = imageFilterDog.apply( source );
        RandomAccessibleInterval threshold = imageFilterThreshold.apply( dog );

        return threshold;
    }

}
