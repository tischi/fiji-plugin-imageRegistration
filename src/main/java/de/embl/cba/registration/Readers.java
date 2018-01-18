package de.embl.cba.registration;

import de.embl.cba.registration.util.MetaImage;
import ij.IJ;
import ij.io.Opener;
import io.scif.config.SCIFIOConfig;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Readers
{

    public static MetaImage openUsingDefaultSCIFIO( String path, ImageJ ij ) throws IOException
    {
        long start = Logger.start("# Open image using default SCIFIO...");

        final File file = new File( path );
        Dataset dataset = ij.scifio().datasetIO().open( file.getPath() );

        MetaImage metaImage = new MetaImage();
        metaImage.imgPlus = dataset.getImgPlus();
        metaImage.rai = metaImage.imgPlus;
        metaImage.axisTypes = Axes.axisTypesList( dataset );
        metaImage.axisOrder = Axes.axisOrder( metaImage.axisTypes );

        Logger.doneIn( start );
        return metaImage;
    }

    public static RandomAccessibleInterval<RealType<?>> openUsingPlanarSCIFIO( String path, ImageJ ij ) throws IOException
    {
        long start = Logger.start("# Open image using planar SCIFIO...");

        SCIFIOConfig scifioConfig = new SCIFIOConfig(  );
        scifioConfig.imgOpenerSetImgModes(  SCIFIOConfig.ImgMode.PLANAR );
        final File file = new File( path );
        Dataset dataset = ij.scifio().datasetIO().open( file.getPath(), scifioConfig );

        Logger.doneIn( start );
        return dataset;
    }

    public static MetaImage openUsingImageJ1( String path ) throws IOException
    {
        return internOpenUsingImageJ1( path, false );
    }

    public static MetaImage openVirtualUsingImageJ1( String path ) throws IOException
    {
        return internOpenUsingImageJ1( path, true );
    }

    public static MetaImage internOpenUsingImageJ1( String path, boolean openVirtual ) throws IOException
    {
        long start = Logger.start("# Open image using ImageJ1 Opener...");

        MetaImage metaImage = new MetaImage();

        Opener opener = new Opener();

        if ( openVirtual )
        {
            metaImage.imagePlus = IJ.openVirtual( path );
            metaImage.img = VirtualStackAdapter.wrap( metaImage.imagePlus );
        }
        else
        {
            metaImage.imagePlus = IJ.openImage( path );
            metaImage.img = ImageJFunctions.wrap( metaImage.imagePlus );
        }

        metaImage.rai = metaImage.img;
        metaImage.title = metaImage.imagePlus.getTitle();
        metaImage.axisTypes = Axes.getAxisTypes( metaImage.imagePlus );


        Logger.doneIn( start );

        return metaImage;
    }

    public static  ArrayList< AxisType > readAxisTypesUsingDefaultSCIFIO( String path, ImageJ ij ) throws IOException
    {
        long start = Logger.start("# Reading axis types using default SCIFIO...");

        final File file = new File( path );
        Dataset dataset = ij.scifio().datasetIO().open( file.getPath() );
        ArrayList< AxisType > axisTypes = Axes.axisTypesList( dataset );

        Logger.doneIn( start );
        return axisTypes;
    }




}
