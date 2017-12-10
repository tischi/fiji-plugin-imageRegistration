package embl.almf.filter;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.Map;

public abstract class ImageFilterFactory < R extends RealType< R > & NativeType< R > > {



    public static ImageFilter create( String imageFilterType,
                                           Map< String, Object > parameters )
    {
        switch ( imageFilterType )
        {
            case ImageFilterConstants.FILTER_GAUSS:
                return new ImageFilterGauss<>( parameters);

        }

        return null;

    }

}
