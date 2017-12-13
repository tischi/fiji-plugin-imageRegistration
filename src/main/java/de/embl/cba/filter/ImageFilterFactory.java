package de.embl.cba.filter;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public abstract class ImageFilterFactory < R extends RealType< R > & NativeType< R > > {

    public static ImageFilter create(
            ImageFilterType imageFilterType,
            Map< String, Object > parameters )
    {
        if ( imageFilterType.equals( ImageFilterType.GAUSS ) )
        {
            return new ImageFilterGauss( parameters );
        }
        else if ( imageFilterType.equals ( ImageFilterType.THRESHOLD ) )
        {
            return new ImageFilterThreshold( parameters );
        }
        else if ( imageFilterType.equals ( ImageFilterType.DOG ) )
        {
            return new ImageFilterDog( parameters );
        }
        else if ( imageFilterType.equals ( ImageFilterType.DOG_THRESHOLD ) )
        {
            return new ImageFilterDogThreshold( parameters );
        }
        else
        {
            // TODO: throw an error
            return null;
        }

    }

}
