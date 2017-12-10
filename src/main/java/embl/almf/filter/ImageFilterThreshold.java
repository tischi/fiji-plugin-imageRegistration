package embl.almf.filter;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.converter.Converters;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import java.util.Map;

import static embl.almf.filter.ImageFilterParameters.NUM_THREADS;
import static embl.almf.filter.ImageFilterParameters.THRESHOLD_VALUE;

public class ImageFilterThreshold
        < R extends RealType< R > & NativeType < R > >
        implements ImageFilter< R, BitType > {

    private Map< String, Object > parameters;

    public ImageFilterThreshold( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    @Override
    public RandomAccessibleInterval< BitType > filter( RandomAccessibleInterval< R > source )
    {
        double threshold = (double) parameters.get( THRESHOLD_VALUE );
        return Converters.convert( source, (s,t) -> { t.set( s.getRealDouble() > threshold );}, new BitType(  ) );
    }

}
