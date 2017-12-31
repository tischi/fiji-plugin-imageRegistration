package de.embl.cba.registration.util;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public abstract class Duplicator
{

    public static < R extends RealType< R > & NativeType< R > > RandomAccessibleInterval< R > toArrayImg( RandomAccessibleInterval< R > source )
    {
        ImgFactory< R > imgFactory = new ArrayImgFactory< R >();

        Img< R > output = imgFactory.create( source, Views.iterable( source ).firstElement() );

        for ( Pair< R, R > p : Views.interval( Views.pair( Views.zeroMin( source ), output ), output ) )
            p.getB().set( p.getA() );

        return Views.translate( output, Intervals.minAsLongArray( source ) );
    }

}