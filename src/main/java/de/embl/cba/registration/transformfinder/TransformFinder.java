package de.embl.cba.registration.transformfinder;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface TransformFinder< R extends RealType< R > & NativeType< R > > {

     RealTransform findTransform( RandomAccessibleInterval< R > fixedRAI, RandomAccessible< R > movingRA );

}
