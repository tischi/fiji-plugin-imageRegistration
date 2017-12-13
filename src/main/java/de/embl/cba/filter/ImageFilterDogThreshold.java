package de.embl.cba.filter;

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
        this.imageFilterDog = ImageFilterFactory.create( ImageFilterType.DOG, parameters );
        this.imageFilterThreshold = ImageFilterFactory.create( ImageFilterType.THRESHOLD, parameters );
    }

    @Override
    public RandomAccessibleInterval< BitType > filter( RandomAccessibleInterval< R > source )
    {

        RandomAccessibleInterval dog = imageFilterDog.filter( source );
        RandomAccessibleInterval threshold = imageFilterThreshold.filter( dog );

        return threshold;
    }

}
