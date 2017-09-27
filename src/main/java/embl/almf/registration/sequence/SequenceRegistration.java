package embl.almf.registration.sequence;

import java.io.File;

import embl.almf.image.ImageInterface;

public class SequenceRegistration implements SequenceRegistrationInterface {
	
	public void setInputData(ImageInterface inputImage);
	
	public void setSettings(SettingsInterface newSettings);
	
	public SettingsInterface getSettings();
	
	public void saveSettings(SettingsInterface settings,File registrationLog);
	
	public SettingsInterface loadSettings(File registrationLog);
	
	
	public ImageInterface getOutputData();
	
	public void runSequenceRegistration(){
		
	}
	
	public void saveRegistration(File registrationLog);
	
	public void loadRegistration(File registrationLog);

}
