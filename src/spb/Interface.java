package spb;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

@SuppressWarnings("serial")
public class Interface extends Agent {
		
	private int raio, totalEntregue, ofertas, escolhidas, totalSaidas;
	private double capacidade, numBikesAtual, totalPedidos, totalPedidosAceites, sucesso, ocupacao, distOriginal, endDist;
	private String startAID, endAID, chosenAID, position;
	
		public void setup() {
			super.setup();
			
			addBehaviour(new CyclicBehaviour(this) {

				public void action() {
					
					MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
							MessageTemplate.MatchPerformative(ACLMessage.INFORM_REF));							
					ACLMessage msg = receive(mt);
					
					if (msg != null) {
						
						//receber estatísticas dos utilizadores
						if (msg.getPerformative() == 7) {   //INFORM
							String[] info = msg.getContent().split(" ");
							startAID = info[2] + " " + info[3];
							endAID = info[7] + " " + info[8];
							chosenAID = info[12] + " " + info[13];
							distOriginal = Double.parseDouble(info[16]);
							endDist = Double.parseDouble(info[19]);								
							ofertas = Integer.parseInt(info[21]);
							escolhidas = Integer.parseInt(info[24]);
							
							System.out.println("\n-------------" + msg.getSender().getLocalName().toUpperCase() + " DEVOLVEU BICICLETA-------------");
							System.out.println("O " + msg.getSender().getLocalName() + " saiu da " + startAID + " e queria ir para a " + endAID +
									". Durante a viagem recebeu " + ofertas + " ofertas e aceitou " + escolhidas + ". Acabou por chegar à "+
									chosenAID + " percorrendo uma distância de " + endDist + " e a distância calculada originalmente era de " + distOriginal);
						}
				
						//receber estatísticas das estações
						if (msg.getPerformative() == 9)  {   //INFORM_REF
							String[] cont = msg.getContent().split(" ");
							position = cont[1] + " " + cont[2];
							raio = Integer.parseInt(cont[6]);
							capacidade = Double.parseDouble(cont[10]);
							numBikesAtual = Double.parseDouble(cont[12]);
							totalEntregue = Integer.parseInt(cont[15]);
							totalSaidas = Integer.parseInt(cont[18]);
							totalPedidos = Double.parseDouble(cont[21]);
							totalPedidosAceites =  Double.parseDouble(cont[23]);
							
							if (totalPedidos == 0) {
								sucesso = 0;
							}
							else {
								sucesso = (double) Math.round(totalPedidosAceites/totalPedidos * 100 * 100D) / 100D; 
							}
							ocupacao = (double) Math.round(numBikesAtual/capacidade * 100 * 100D) / 100D;							
							
							System.out.println("\n-------------" + msg.getSender().getLocalName().toUpperCase() + "-------------");
							System.out.println("A " + msg.getSender().getLocalName() + " situada nas coordenadas: " + position + 
									" com um raio de proximidade de " + raio + " tem uma taxa de ocupação de " + ocupacao + "%, estando de momento com " + (int)numBikesAtual + 
									" bicicletas em " + (int)capacidade + " possíveis");
							System.out.println("A sua taxa de sucesso a atrair utilizadores é de " + sucesso + "%" + 
									" tendo sido aceites " + (int)totalPedidosAceites + " em " + (int)totalPedidos + " pedidos");
							System.out.println("Foram alugadas " + totalSaidas + 
									" bicicletas e entraram " + totalEntregue);
						}					
						
				
					} block();
					
				}			
			});
		
		}
		protected void takeDown() {                   
			super.takeDown();
			System.out.println("SPB encontra-se fechado");
		}	  
}

