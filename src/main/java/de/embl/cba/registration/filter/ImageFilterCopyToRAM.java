package de.embl.cba.registration.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

import java.util.Map;

public class ImageFilterCopyToRAM
        < R extends RealType< R > & NativeType< R >, U extends RealType< U > >
        implements ImageFilter< R, R > {

    private Map< String, Object > parameters;

    public ImageFilterCopyToRAM( Map< String, Object > parameters )
    {
        this.parameters = parameters;
    }

    @Override
    public RandomAccessibleInterval< R > filter( RandomAccessibleInterval< R > source )
    {

        ImgFactory< R > imgFactory = new ArrayImgFactory< R >();
        Img< R > output = imgFactory.create( source, Views.iterable( source ).firstElement() );

        for ( Pair< R, R > p : Views.interval( Views.pair( Views.zeroMin( source ), output ), output ) )
            p.getB().set( p.getA() );

        /*
        ArrayCursor< BitType > target = output.cursor();
        Cursor< BitType > src = Views.flatIterable( converted ).cursor();
        while( target.hasNext() )
            target.next().set( src.next() );
        */

        return Views.translate( output, Intervals.minAsLongArray( source ) );

    }
}