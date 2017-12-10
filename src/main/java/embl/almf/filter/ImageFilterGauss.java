package embl.almf.filter;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;

import java.util.Map;

import net.imglib2.algorithm.gauss3.*;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import static embl.almf.filter.ImageFilterConstants.*;

public class ImageFilterGauss< R extends RealType< R > & NativeType < R > > implements ImageFilter< R > {

    private Map< String, Object > parameters;

    public ImageFilterGauss( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    @Override
    public RandomAccessibleInterval< R > filter( RandomAccessibleInterval< R > source )
    {
        double[] sigmas = (double []) parameters.get( FILTER_GAUSS_SIGMAS );

        final ImgFactory< R > factory = new ArrayImgFactory< R >();
        RandomAccessibleInterval< R > target = factory.create( source, Views.iterable( source ).firstElement() );

        RandomAccessible< R > extendedSource = Views.extendMirrorSingle( source );

        try
        {
            Gauss3.gauss( sigmas, extendedSource, target );
        }
        catch ( IncompatibleTypeException e )
        {
            e.printStackTrace();
        }

        return target;
    }
}
