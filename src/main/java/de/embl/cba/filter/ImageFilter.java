package de.embl.cba.filter;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface ImageFilter < R extends RealType< R > & NativeType < R >, U extends RealType< U > >  {

    RandomAccessibleInterval< U > filter( RandomAccessibleInterval< R > rai );

}
