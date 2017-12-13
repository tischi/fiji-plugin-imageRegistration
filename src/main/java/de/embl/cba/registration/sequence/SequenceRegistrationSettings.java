package de.embl.cba.registration.sequence;

public class SequenceRegistrationSettings implements SettingsInterface{

	public enum RegistrationMethod{
		Elastix_Translation("Elastix Translation"),
		Elastix_Euler("Elastix Euler"),
		PhaseCorrelation_Translation("PhaseCorrelation Translation");
		private final String methodName;
		private RegistrationMethod(String name) {methodName=name;}
		@Override
		public String toString(){return methodName;}
		
	}

	public enum SequenceAlgorithm{
		Recursive("Recursive"),
		Fixed_Point("Fixed Point");
		private final String sAlgorithm;
		private SequenceAlgorithm(String algorithm) {sAlgorithm=algorithm;}
		@Override
		public String toString(){return sAlgorithm;}
	}
	
	RegistrationMethod method;
	SequenceAlgorithm sAlgorithm;
	
}
