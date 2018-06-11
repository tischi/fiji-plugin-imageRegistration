package de.embl.cba.registration.utils;

import ij.ImagePlus;

import static de.embl.cba.registration.utils.Constants.X;
import static de.embl.cba.registration.utils.Constants.Y;
import static de.embl.cba.registration.utils.Constants.Z;

public abstract class ImagePlusUtils
{
	public static double[] getCalibration( ImagePlus imagePlus )
	{
		double[] calibration = new double[3];
		calibration[ X ] = imagePlus.getCalibration().pixelWidth;
		calibration[ Y ] = imagePlus.getCalibration().pixelHeight;
		calibration[ Z ] = imagePlus.getCalibration().pixelDepth;
		return calibration;
	}
}
