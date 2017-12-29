package de.embl.cba.registration.ui;

import de.embl.cba.registration.OutputIntervalType;
import de.embl.cba.registration.RegistrationAxisType;
import de.embl.cba.registration.filter.ImageFilterParameters;
import de.embl.cba.registration.filter.ImageFilterType;
import de.embl.cba.registration.transformfinder.TransformFinderSettings;
import de.embl.cba.registration.transformfinder.TransformFinderType;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;
import org.scijava.module.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class RegistrationParameters
{
    private final RegistrationPlugin plugin;
    private final Module module; // TODO: what is the difference between plugin and module?

    public RegistrationAxisType[] registrationAxisTypes;
    public Map< String, Object > filterParameters;
    public Map< String, Object > transformParameters;
    public TransformFinderSettings transformSettings;
    public OutputIntervalType outputIntervalType;
    public FinalInterval interval;
    public ExecutorService executorService;

    public RegistrationParameters( RegistrationPlugin plugin )
    {
        this.plugin = plugin;
        this.module = plugin;
        updateParameters();
    }

    public void updateParameters()
    {
        setRegistrationAxesTypes();
        setImageFilterParameters();
        setTransformSettings();
        setRegistrationAxesInterval();
        setOutputInterval();
        setNumThreads();
    }

    private void setNumThreads()
    {
        executorService = plugin.threadService.getExecutorService(); // TODO
    }

    private void setRegistrationAxesTypes()
    {
        registrationAxisTypes = new RegistrationAxisType[ plugin.dataset.numDimensions() ];

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            String axisTypeName = ( String ) plugin.typeInput( d ).getValue( module );
            registrationAxisTypes[ d ] = RegistrationAxisType.valueOf( axisTypeName );
        }

    }

    private void setRegistrationAxesInterval()
    {
        long[] min = Intervals.minAsLongArray( plugin.dataset );
        long[] max = Intervals.maxAsLongArray( plugin.dataset );

        for ( int d = 0; d < plugin.dataset.numDimensions(); ++d )
        {
            min[ d ] = (long) plugin.varInput( d, "min" ).getValue( module );
            max[ d ] = (long) plugin.varInput( d, "max" ).getValue( module );
        }

        interval = new FinalInterval( min, max );
    }

    private void setImageFilterParameters()
    {
        filterParameters = new HashMap<>();
        setFilters();
        setSubSampling();
        setThreshold();
        setGauss();
    }

    private void setFilters()
    {
        ArrayList< ImageFilterType > imageFilterTypes = new ArrayList<>(  );
        imageFilterTypes.add( ImageFilterType.valueOf( plugin.imageFilterType ) );
        imageFilterTypes.add( ImageFilterType.SubSample );
        filterParameters.put( ImageFilterParameters.SEQUENCE, imageFilterTypes );
    }

    private void setSubSampling()
    {
        String[] tmp;
        tmp = plugin.imageFilterSubSampling.split( "," );
        long[] subSampling = Arrays.stream( tmp ).mapToLong( i -> Long.parseLong( i ) ).toArray();
        filterParameters.put(
                ImageFilterParameters.SUB_SAMPLING, subSampling );
    }

    private void setGauss()
    {
        filterParameters.put(
                ImageFilterParameters.DOG_SIGMA_SMALLER, new double[]{ 2.0D, 2.0D } );
        filterParameters.put(
                ImageFilterParameters.DOG_SIGMA_LARGER, new double[]{ 5.0D, 5.0D } );
        filterParameters.put(
                ImageFilterParameters.GAUSS_SIGMA, new double[]{ 10.0D, 1.0D } );
    }

    private void setThreshold()
    {
        String[] tmp;
        tmp = plugin.imageFilterThreshold.split( "," );
        double[] thresholdMinMax = Arrays.stream( tmp ).mapToDouble( i -> Double.parseDouble( i ) ).toArray();

        filterParameters.put(
                ImageFilterParameters.THRESHOLD_MIN_VALUE, thresholdMinMax[0] );
        filterParameters.put(
                ImageFilterParameters.THRESHOLD_MAX_VALUE, thresholdMinMax[1] );
    }

    private void setTransformSettings()
    {
        transformSettings = new TransformFinderSettings();
        transformSettings.transformFinderType = TransformFinderType.valueOf( plugin.transformationTypeInput );
        setTranslationRange();
        setRotationRange();
    }

    private void setRotationRange()
    {
        String[] tmp;
        tmp = plugin.transformationParameterMaximalRotationsInput.split( "," );
        double[] transformationParametersMaximalRotations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            transformationParametersMaximalRotations[ i ] = Double.parseDouble( tmp[ i ] );
        }
        transformSettings.maximalRotations = transformationParametersMaximalRotations;
    }

    private void setTranslationRange()
    {
        String[] tmp;
        tmp = plugin.transformationParametersMaximalTranslationsInput.split( "," );
        double[] maximalTranslations = new double[ tmp.length ];
        for ( int i = 0; i < tmp.length; ++i )
        {
            maximalTranslations[ i ] = Double.parseDouble( tmp[ i ].trim() );
        }
        transformSettings.maximalTranslations = maximalTranslations;
    }

    private void setOutputInterval()
    {
        outputIntervalType = OutputIntervalType.valueOf( plugin.outputViewIntervalSizeTypeInput );
    }
}