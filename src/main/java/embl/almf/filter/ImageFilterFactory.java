package embl.almf.filter;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

import static embl.almf.filter.ImageFilterType.*;

public abstract class ImageFilterFactory < R extends RealType< R > & NativeType< R > > {

    public static ImageFilter create(
            ImageFilterType imageFilterType,
            Map< String, Object > parameters )
    {
        if ( imageFilterType.equals( GAUSS ) )
        {
            return new ImageFilterGauss( parameters );
        }
        else if ( imageFilterType.equals ( THRESHOLD ) )
        {
            return new ImageFilterThreshold( parameters );
        }
        else
        {
            // TODO: throw an error
            return null;
        }

    }

}
