package de.embl.cba.registration.filter;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public abstract class ImageFilterFactory < R extends RealType< R > & NativeType< R > > {

    public static ImageFilter create( Map< String, Object > parameters )
    {
        ImageFilterType imageFilterType = (ImageFilterType) parameters.get( ImageFilterParameters.FILTER_TYPE );

        if ( imageFilterType.equals( ImageFilterType.Gauss ) )
        {
            return new ImageFilterGauss( parameters );
        }
        else if ( imageFilterType.equals ( ImageFilterType.Threshold ) )
        {
            return new ImageFilterThreshold( parameters );
        }
        else if ( imageFilterType.equals ( ImageFilterType.DifferenceOfGaussian ) )
        {
            return new ImageFilterDog( parameters );
        }
        else if ( imageFilterType.equals ( ImageFilterType.EnhanceEdgesAndThreshold ) )
        {
            return new ImageFilterDogThreshold( parameters );
        }
        else
        {
            return null;
        }

    }

}
