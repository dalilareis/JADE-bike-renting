package spb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class AU extends Agent {
	
	protected static AID[] stationAgents;
	private AID startAID, endAID, chosenAID;
	private Random rand = new Random();
	private Position start, end, moving, pos, end1;
	private int timer, offer, scoreMin;
	private int ofertas = 0;
	private int escolhidas = 0;
	private double bestScore = 0;
	private double distanciaPercorrida = 0;
	private double totalDist, move, inc, dist, score1, distOriginal, peso;
	private HashMap<Double, AID> propostas = new HashMap<Double, AID>();
	private HashMap<AID, Position> mapStations = new HashMap<AID, Position>();
	private List<AID> listStations;
	private boolean simpleOver, offersOver;

	protected void setup() {
		super.setup();

//--------------------------------------Procurar no DF pelas Estações----------------------------------------
		
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Estacao");
		template.addServices(sd);
		DFAgentDescription[] result;
		try {
			result = DFService.search(this, template);
			AU.stationAgents = new AID[result.length];
			for (int i=0; i < result.length; i++) {
				AU.stationAgents[i] = result[i].getName();
			}
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
		scoreMin = rand.nextInt(31-1)+1;  //score mínimo aleatório entre 1 e 30, utilizador só aceitará propostas com scores mais altos que este	
		double peso = Math.random();  //Pesos aleatórios (0 a 1)
		simpleOver = false;
			
		SequentialBehaviour sb = new SequentialBehaviour();	
			
//-----------------------------------Pedir as Posições das Estações-------------------------------------------
			
		sb.addSubBehaviour(new OneShotBehaviour(this) {
		    public void action() {
		    
			    ACLMessage ask = new ACLMessage(ACLMessage.REQUEST);
			    ask.setContent("What is your position?");
			    for (int i=0; i < stationAgents.length; i++) {
			    	ask.addReceiver(new AID (stationAgents[i].getLocalName(), AID.ISLOCALNAME));					
			    }
				send(ask);
		    }
		});
		
//---------------------------Receber as Posições de todas as AE registadas----------------------------------
			
		sb.addSubBehaviour(new SimpleBehaviour(this) {
			public void action() {
				for (int i=0; i < stationAgents.length; i++) {
					MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF);
					ACLMessage response = receive(mt);
					if (response != null) {
				    					    			
					    String[] posAE = response.getContent().split(" "); 
					    double x = Double.parseDouble(posAE[0]);
					    double y = Double.parseDouble(posAE[1]);
					    pos = new Position (x,y);
					    mapStations.put(response.getSender(), pos);
					    if(mapStations.size()==stationAgents.length) simpleOver = true;
					}
				} 
			}
			public boolean done() {
				if (simpleOver)
					return true;
				return false;
			}
		});
			
//---------------------Seleccionar 2 estações (à sorte) e estabelecer as Posições de início e fim da viagem--------------------
		
		sb.addSubBehaviour(new OneShotBehaviour(this) {
			public void action() {
					
				listStations = new ArrayList<AID>(mapStations.keySet());
				startAID = listStations.get(rand.nextInt(listStations.size()));
				start = mapStations.get(startAID);
				end = mapStations.get(listStations.get(rand.nextInt(listStations.size())));
				if (end.equals(start)) {
					while (end.equals(start)) end = mapStations.get(listStations.get(rand.nextInt(listStations.size())));
				}
				//System.out.println("Aluguei uma bicicleta na " + startAID.getLocalName() + "\n");
				//System.out.println("Origem: " + start.toString() + " Destino: " + end.toString() + "\n");
				
				endAID = getKeyFromMap(mapStations, end);		
					
				ACLMessage selected = new ACLMessage(ACLMessage.AGREE);
				selected.setContent("I am leaving your station");
				selected.addReceiver(startAID);
				send(selected);
			}
		});
				
//---------------------Simular o movimento do AU (viagem)----------------------------------------

		sb.addSubBehaviour(new SimpleBehaviour(this) {
				
			public void action() {
					
				totalDist = start.calcDist(end);	
				inc = 0.05;
				move = 0;
				timer = ThreadLocalRandom.current().nextInt(500, 3000);
				
				//System.out.println("Deslocação a cada " + (timer/1000) + "s" + "\n");
					
				addBehaviour (new TickerBehaviour(myAgent, timer) {								
					protected void onTick() {
										
						move += inc * totalDist;						
						moving = start.getPoint(end, move);
						//System.out.println(moving.toString());
						
//---------------------Informar às estações a posição actual do AU----------------------------------------
							
						if (move >= 0.75*totalDist && move <= totalDist) {
							ACLMessage givePosition = new ACLMessage(ACLMessage.INFORM);
							givePosition.setContent("Current position is " + moving);
							for (int i = 0; i < stationAgents.length; i++) {
								if(!mapStations.get(stationAgents[i]).equals(end)) {
									givePosition.addReceiver(new AID (stationAgents[i].getLocalName(), AID.ISLOCALNAME));								
								}
							}	
							send(givePosition);
						}
								
//---------------------Verificar se chegou ao destino----------------------------------------
								
						if (move >= totalDist || moving.calcDist(end) < 0.5 ) {
							
							distanciaPercorrida += move;
							chosenAID = getKeyFromMap (mapStations, end);
							end1 = mapStations.get(endAID);
							distOriginal = start.calcDist(end1);
							ACLMessage arrived = new ACLMessage(ACLMessage.CONFIRM);
							ACLMessage stats = new ACLMessage(ACLMessage.INFORM);
							arrived.setContent("I have arrived at your station!");
							stats.addReceiver(new AID ("Interface", AID.ISLOCALNAME));
								
							if (escolhidas > 0) {								
								
								arrived.addReceiver(chosenAID);
								stats.setContent("Parti de " + startAID.getLocalName() + " queria ir para " + endAID.getLocalName() + " e cheguei a " + chosenAID.getLocalName() +
									" Distância original " + distOriginal + " Distância percorrida " + distanciaPercorrida + 
									" Recebi " + ofertas + " ofertas aceitei " + escolhidas);
							}
							else if (escolhidas == 0 || ofertas == 0) {
								
								arrived.addReceiver(endAID);
								stats.setContent("Parti de " + startAID.getLocalName() + " queria ir para " + endAID.getLocalName() + " e cheguei a " + endAID.getLocalName() +
									" Distância original " + distOriginal + " Distância percorrida " + distOriginal + 
									" Recebi " + ofertas + " ofertas aceitei " + escolhidas);
							}								
							send(arrived);
							send(stats);								
							//System.out.println(moving.toString() + " Cheguei ao destino"); 								
							myAgent.doDelete();
							}
						}
				}); 	
				offersOver = true;	
				}				
			public boolean done() {
				if (offersOver)
					return true;
				return false;
			}
		});
						
//----------------------------Receber as ofertas das Estações-----------------------------------------------
								
		sb.addSubBehaviour(new CyclicBehaviour(this) {													
			public void action() {	
					
				MessageTemplate temp = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
				ACLMessage offers = receive(temp);
				if (offers != null) {
																		
					String[] dados = offers.getContent().split(" ");
					offer = Integer.parseInt(dados[3]);
					dist = Double.parseDouble(dados[8]);
																						
//-------------------Estabelecer a melhor oferta (atribuição de 1 score)-------------------------------------
											
					score1 = score(offer, dist, peso);
					propostas.put(score1, offers.getSender());
					if (score1 > bestScore) {								
						bestScore = score1;
						propostas.replace(bestScore, offers.getSender());
					}							
					ofertas++;
					//System.out.println("\n Recebi uma oferta");
											
//---------------------------Aceitar/recusar ofertas-----------------------------------
											
					if (bestScore > scoreMin) { 
						ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						AID chosen = propostas.get(bestScore);
						accept.addReceiver(chosen);
						accept.setContent("I will deliver my bike at your station");
						send(accept);
						if(!mapStations.get(chosen).equals(end)) {
							end = mapStations.get(chosen);
							start = moving;
							totalDist = start.calcDist(end);
							distanciaPercorrida += move;
							move = 0;
							inc = 0.1;
						}
						escolhidas++;						
						//System.out.println("Aceitei uma proposta. Novo destino: " + end.toString());
					
					}
					else {
						ACLMessage refuse = offers.createReply();
						refuse.setPerformative(ACLMessage.REJECT_PROPOSAL);
						refuse.setContent("Not interested!");
						send(refuse);							
						//System.out.println("Recusei uma proposta");
					}
				}
			} 
		});
						
		addBehaviour(sb);	
	}

	protected void takeDown() {                   
		super.takeDown();
		System.out.println(this.getAID().getLocalName() + " saiu do sistema");
	}	  

//-----------Cálculo de um score para as ofertas recebidas, segundo desconto e distância da estação à posição atual do AU-----------
	
	public static double score (int o, double d, double peso) {  //score máximo possível é 45.01 para um desconto de 50% e uma distância de 10, mínimo de 0.536 para desconto 5% e distância 25
													
		double score = o*peso + (1-peso)/d;	 //Diretamente proporcional ao desconto e inversamente proporcional à distância	
		return score;		
	}
	
//------------------Conseguir a key do HashMap a partir do seu valor (o AID para uma posição)--------------------------
	
	public static AID getKeyFromMap (HashMap<AID, Position> map, Position value) {
		for (AID a : map.keySet()) {
			if (map.get(a).equals(value)) {
		        return a;
		    }		      
		}
		return null;
	}						
}			