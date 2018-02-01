package de.embl.cba.registration;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.img.ImgView;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import java.util.function.Function;

/**
 * Wrapper for ImagePlus using imglib2-caches.
 * It loads the planes lazily, which is especially useful when wrapping a virtual stack.
 *
 * @author Matthias Arzt
 */

public class VirtualStackAdapter {

    public static Img<UnsignedByteType > wrapByte( ImagePlus image ) {
        return internWrap(image, ImagePlus.GRAY8, new UnsignedByteType(), array -> new ByteArray((byte[]) array));
    }

    public static Img<UnsignedShortType > wrapShort( ImagePlus image ) {
        return internWrap(image, ImagePlus.GRAY16, new UnsignedShortType(), array -> new ShortArray((short[]) array));
    }

    public static Img<FloatType> wrapFloat(ImagePlus image) {
        return internWrap(image, ImagePlus.GRAY32, new FloatType(), array -> new FloatArray((float[]) array));
    }

    public static Img<ARGBType> wrapRGBA(ImagePlus image) {
        return internWrap(image, ImagePlus.COLOR_RGB, new ARGBType(), array -> new IntArray((int[]) array));
    }

    public static Img< ? > wrap( ImagePlus image )
    {
        switch ( image.getType() )
        {
            case ImagePlus.GRAY8:
                return wrapByte( image );
            case ImagePlus.GRAY16:
                return wrapShort( image );
            case ImagePlus.GRAY32:
                return wrapFloat( image );
            case ImagePlus.COLOR_RGB:
                return wrapRGBA( image );
        }
        throw new RuntimeException( "Only 8, 16, 32-bit and RGB supported!" );
    }

    private static <T extends NativeType<T>, A extends ArrayDataAccess<A>> Img<T> internWrap(ImagePlus image, int expectedType, T type, Function<Object, A> createArrayAccess) {
        if(image.getType() != expectedType)
            throw new IllegalArgumentException();
        ImagePlusLoader<A> loader = new ImagePlusLoader<>(image, createArrayAccess);
        CachedCellImg<T, A> cached = createCachedImage(type, loader);
        return ImgView.wrap(dropSingletonDimensionsButNotFirstOrSecond(cached), new PlanarImgFactory<>());
    }

    public static < T > RandomAccessibleInterval< T > dropSingletonDimensionsButNotFirstOrSecond( final RandomAccessibleInterval< T > source )
    {
        RandomAccessibleInterval< T > res = source;
        for ( int d = source.numDimensions() - 1; d >= 2; --d )
            if ( source.dimension( d ) == 1 )
                res = Views.hyperSlice( res, d, source.min( d ) );
        return res;
    }

    private static <T extends NativeType<T>, A extends ArrayDataAccess<A>> CachedCellImg<T, A> createCachedImage(T type, ImagePlusLoader<A> loader) {
        ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory();
        return factory.createWithCacheLoader(loader.grid().getImgDimensions(), type, loader,
                new ReadOnlyCachedCellImgOptions().cellDimensions(loader.cellDimensions()));
    }

    private static class ImagePlusLoader<A extends ArrayDataAccess<A>> implements CacheLoader< Long, Cell<A>>
    {
        private final ImagePlus image;
        private final CellGrid grid;
        private final Function<Object, A> arrayFactory;

        public ImagePlusLoader(final ImagePlus image, Function<Object, A> arrayFactory)
        {
            this.arrayFactory = arrayFactory;
            final long[] dimensions = { image.getWidth(), image.getHeight(), image.getNChannels(), image.getNSlices(), image.getNFrames()};
            final int[] cellDimensions = { image.getWidth(), image.getHeight(), 1, 1, 1 };
            this.grid = new CellGrid( dimensions, cellDimensions);
            this.image = image;
        }

        @Override
        public Cell<A> get( final Long key ) throws Exception
        {
            final long[] cellMin = new long[ 5 ];
            final int[] cellDims = new int[ 5 ];
            grid.getCellDimensions(key, cellMin, cellDims );

            int channel = (int) (cellMin[2] + 1);
            int slice = (int) (cellMin[3] + 1);
            int frame = (int) (cellMin[4] + 1);
            int stackIndex = image.getStackIndex(channel, slice, frame);
            ImageProcessor processor = image.getStack().getProcessor(stackIndex);
            final A array = arrayFactory.apply(processor.getPixels());
            return new Cell<>( cellDims, cellMin, array );
        }

        public CellGrid grid() {
            return grid;
        }

        public int[] cellDimensions() {
            final int[] cellDims = new int[ grid.numDimensions() ];
            grid.cellDimensions(cellDims);
            return cellDims;
        }
    }
}