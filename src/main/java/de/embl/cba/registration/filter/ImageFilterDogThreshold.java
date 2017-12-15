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
        this.imageFilterDog = ImageFilterFactory.create( parameters );

        parameters.put( ImageFilterParameters.FILTER_TYPE, ImageFilterType.Threshold );
        this.imageFilterThreshold = ImageFilterFactory.create(  parameters );
    }

    @Override
    public RandomAccessibleInterval< BitType > filter( RandomAccessibleInterval< R > source )
    {

        RandomAccessibleInterval dog = imageFilterDog.filter( source );
        RandomAccessibleInterval threshold = imageFilterThreshold.filter( dog );

        return threshold;
    }

}
