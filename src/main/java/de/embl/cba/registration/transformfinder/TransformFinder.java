package de.embl.cba.registration.transformfinder;

import de.embl.cba.registration.filter.FilterSequence;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public interface TransformFinder< R extends RealType< R > & NativeType< R > > {

     RealTransform findTransform( RandomAccessibleInterval< R > fixedRAI, RandomAccessible< R > movingRA, FilterSequence filterSequence);

     String asString();

}
