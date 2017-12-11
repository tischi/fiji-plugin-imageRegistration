package embl.almf.filter;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.converter.Converters;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
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
        ArrayImg< BitType, LongArray > output = ArrayImgs
                .bits( Intervals.dimensionsAsLongArray( source ) );

        RandomAccessibleInterval< BitType > converted = Converters.convert( source, ( s, t ) -> {
            t.set( s.getRealDouble() > threshold );
        }, new BitType() );
        for ( Pair< BitType, BitType > p : Views.interval( Views.pair( Views.zeroMin( converted ), output ), output ) )
            p.getB().set( p.getA() );

        /*
        ArrayCursor< BitType > target = output.cursor();
        Cursor< BitType > src = Views.flatIterable( converted ).cursor();
        while( target.hasNext() )
            target.next().set( src.next() );
        */

        return Views.translate( output, Intervals.minAsLongArray( source ) );

        // return Converters.convert( source, (s,t) -> { t.set( s.getRealDouble() > threshold );}, new BitType(  ) );
    }




}
