package de.embl.cba.registration.transformationfinders;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.concurrent.ExecutorService;

public interface TransformationFinder {

    < R extends RealType< R > & NativeType< R > > RealTransform findTransform(
            RandomAccessibleInterval fixedRAI,
            RandomAccessible movingRA,
            ExecutorService service);
}
