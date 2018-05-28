package de.embl.cba.registration.ui;

import bdv.util.BdvSource;
import mpicbg.spim.data.SpimData;
import net.imglib2.type.numeric.ARGBType;

import java.awt.*;
import java.io.File;

public class ProSPrDataSource
{
    public SpimData spimData;
    public BdvSource bdvSource;
    public File file;
    public Integer maxLutValue;
    public boolean isActive;
    public Color color;
    public String name;

}