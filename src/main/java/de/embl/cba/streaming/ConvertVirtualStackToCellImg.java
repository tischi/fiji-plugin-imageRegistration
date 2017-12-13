package de.embl.cba.streaming;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public abstract class ConvertVirtualStackToCellImg {

    public final static Img< UnsignedShortType > getCellImgUnsignedShort( ImagePlus imp )
    {
        // assuming we know it is a 3D, 16-bit stack...
        final long[] dimensions = new long[]{
                imp.getStack().getWidth(),
                imp.getStack().getHeight(),
                imp.getStack().getSize()
        };

        // set up cell size such that one cell is one plane
        final int[] cellDimensions = new int[]{
                imp.getStack().getWidth(),
                imp.getStack().getHeight(),
                1
        };

        // make a CellLoader that copies one plane of data from the virtual stack
        final CellLoader< UnsignedShortType > loader = new CellLoader< UnsignedShortType >() {
            @Override
            public void load( final SingleCellArrayImg< UnsignedShortType, ? > cell ) throws Exception
            {
                final int z = ( int ) cell.min( 2 );
                final short[] impData = ( short[] ) imp.getStack().getProcessor( 1 + z ).getPixels();
                final short[] cellData = ( short[] ) cell.getStorageArray();
                System.arraycopy( impData, 0, cellData, 0, cellData.length );
            }
        };

        // create a CellImg with that CellLoader
        final Img< UnsignedShortType > img = new ReadOnlyCachedCellImgFactory().create(
                dimensions,
                new UnsignedShortType(),
                loader,
                ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions ) );

        return img;

    }
}
