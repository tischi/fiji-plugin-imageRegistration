package de.embl.cba.registration.filter;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public abstract class ImageFilterFactory < R extends RealType< R > & NativeType< R > > {

    public static ImageFilter create( ImageFilterType imageFilterType, Map< String, Object > parameters )
    {
        if ( imageFilterType.equals( ImageFilterType.Gauss ) )
        {
            return new ImageFilterGauss( parameters );
        }

        if ( imageFilterType.equals ( ImageFilterType.None ) )
        {
            return new ImageFilterNone( parameters );
        }

        if ( imageFilterType.equals ( ImageFilterType.Threshold ) )
        {
            return new ImageFilterThreshold( parameters );
        }

        if ( imageFilterType.equals ( ImageFilterType.DifferenceOfGaussian ) )
        {
            return new ImageFilterDog( parameters );
        }

        if ( imageFilterType.equals ( ImageFilterType.DifferenceOfGaussianAndThreshold ) )
        {
            return new ImageFilterDogThreshold( parameters );
        }

        if ( imageFilterType.equals ( ImageFilterType.SubSample ) )
        {
            return new ImageFilterSubSample( parameters );
        }

        return new ImageFilterNone( parameters );

    }

}
