package de.embl.cba.registration.utils;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.axis.AxisType;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;

import java.util.ArrayList;

public class MetaImage
{

    public String title;

    public ImagePlus imagePlus;
    public Img img;
    public ImgPlus imgPlus;

    public RandomAccessibleInterval rai;
    public String axisOrder;
    public ArrayList< AxisType > axisTypes;

    public long numSpatialDimensions;

}
