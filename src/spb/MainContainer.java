package spb;

import jade.core.Runtime;

import java.util.concurrent.TimeUnit;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class MainContainer {

	Runtime rt;
	ContainerController container;
	protected static int numCust = 1;

	public void initMainContainerInPlatform(String host, String port, String containerName) {

		//Get the JADE runtime interface (singleton)
		this.rt = Runtime.instance();
		
		//Create a Profile, where the launch arguments are stored
		Profile prof = new ProfileImpl();
		prof.setParameter(Profile.CONTAINER_NAME, containerName);
		prof.setParameter(Profile.MAIN_HOST, host);
		prof.setParameter(Profile.MAIN_PORT, port);
		prof.setParameter(Profile.MAIN, "true");
		prof.setParameter(Profile.GUI, "true");
		
		//create a main agent container
		this.container = rt.createMainContainer(prof);
		rt.setCloseVM(true);
	}
	
	public void startAgentInPlatform(String name, String classpath){
	    try {
	       AgentController ac = container.createNewAgent(
	  name, 
	  classpath, 
	  new Object[0]);
	       ac.start();
	    } catch (Exception e) {
	       e.printStackTrace();
	    }
	 }
	
	
	public static void main(String[] args) {
		MainContainer a = new MainContainer();
		
		a.initMainContainerInPlatform("localhost","9888","MainContainer");
		
		//criar a interface
		a.startAgentInPlatform("Interface", "spb.Interface");
		
		//criar as 10 estações
		for(int nae = 1; nae<11; nae++) {
			a.startAgentInPlatform("Estacao " + nae, "spb.AE");
		}
		
		//criar um utilizador a cada segundo
		int nau = 1;
		while(true) {
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			a.startAgentInPlatform("Utilizador "+ nau, "spb.AU");
			nau++;
		}
	}
}

