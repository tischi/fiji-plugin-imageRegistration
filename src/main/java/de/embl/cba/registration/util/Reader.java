package de.embl.cba.registration.util;

import de.embl.cba.registration.Axes;
import de.embl.cba.registration.Logger;
import io.scif.config.SCIFIOConfig;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Reader
{

    public static RandomAccessibleInterval<RealType<?>> openRAIUsingDefaultSCIFIO( String path, ImageJ ij ) throws IOException
    {
        long start = Logger.start("# Open image using default SCIFIO...");

        final File file = new File( path );
        Dataset dataset = ij.scifio().datasetIO().open( file.getPath() );

        Logger.doneIn( start );
        return dataset;
    }

    public static RandomAccessibleInterval<RealType<?>> openRAIUsingPlanarSCIFIO( String path, ImageJ ij ) throws IOException
    {
        long start = Logger.start("# Open image using planar SCIFIO...");

        SCIFIOConfig scifioConfig = new SCIFIOConfig(  );
        scifioConfig.imgOpenerSetImgModes(  SCIFIOConfig.ImgMode.PLANAR );
        final File file = new File( path );
        Dataset dataset = ij.scifio().datasetIO().open( file.getPath(), scifioConfig );

        Logger.doneIn( start );
        return dataset;
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
