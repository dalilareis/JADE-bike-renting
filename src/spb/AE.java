package spb;

import java.util.Random;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class AE extends Agent {
	private Position posicao;
	private int raio;
	private int capacidade;
	private int numBikesAtual;
	private int totalEntregue;
	private int totalPedidos;
	private int totalPedidosAceites;
	private int percentagemDesconto;
	private int totalSaidas;

	protected void setup() {
		//adicionar estação ao DF
		DFAgentDescription template = new DFAgentDescription();
		template.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Estacao");
		sd.setName("Estacao disponivel");
		template.addServices(sd);
		try {
			DFService.register(this, template);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		
		//inicializações
		posicao = new Position();
		Random r = new Random();
		raio = r.nextInt(26-10) + 10;  //raio da área de proximidade da estação entre 10 e 25
		capacidade = r.nextInt(31-15) + 15;  //capacidade máxima da estação entre 15 e 30 bicicletas
		numBikesAtual = r.nextInt(capacidade+1-1) + 1;  //número de bicicletas começa entre 1 e a capacidade máxima
		totalEntregue = 0;
		totalPedidos = 0;
		totalPedidosAceites = 0;
		totalSaidas = 0;
		percentagemDesconto = calculaDesconto(capacidade, numBikesAtual);
		
		//receber e responder a mensagens dos utilizadores
		addBehaviour(new CyclicBehaviour(this) {
			public void action() {
				ACLMessage msg = receive();
				if (msg!=null) {
					//indicar ao utilizador a posicao da estacao 
					if (msg.getPerformative() == 16 && msg.getContent().equals("What is your position?")) {  //REQUEST
						ACLMessage resp = msg.createReply();
						resp.setContent(posicao.toString());
						resp.setPerformative(ACLMessage.INFORM_REF);
						send(resp);
					}
					//incrementar saídas e atualizar número de bicicletas quando um utilizador sai desta estação
					if (msg.getPerformative() == 1) {  //AGREE
						numBikesAtual--;
						totalSaidas++;
					}
					//receber posição do utilizador, verificar se está na área e se há espaço e enviar incentivos
					if (msg.getPerformative() == 7) {  //INFORM
						
						percentagemDesconto = calculaDesconto(capacidade, numBikesAtual);
						ACLMessage resp = msg.createReply();
						String[] tokens = msg.getContent().split(" ");
						Position posUtilizador = new Position(Double.parseDouble(tokens[tokens.length-2]), Double.parseDouble(tokens[tokens.length-1]));
						double distancia = posUtilizador.calcDist(posicao);
						if(numBikesAtual < 0.8 * capacidade && distancia <= raio) {
							resp.setContent("Oferecemos desconto de " + percentagemDesconto + " para uma distancia de " + distancia);
							resp.setPerformative(ACLMessage.PROPOSE);
							totalPedidos++;
							send(resp);
						}
					}
					//receber respostas às propostas
					if (msg.getPerformative() == 0) {  //ACCEPT_PROPOSAL
						totalPedidosAceites++;
					}
				
					//receber bicicletas
					if (msg.getPerformative() == 4) {  //CONFIRM
						totalEntregue++;
						numBikesAtual++;
						percentagemDesconto = calculaDesconto(capacidade, numBikesAtual);
					}
				}
				block();
			}
		});
		
		//enviar estatísticas à interface a cada 30 segundos
		addBehaviour(new TickerBehaviour(this, 30000) {						
			@Override
			protected void onTick() {
				
				ACLMessage info = new ACLMessage(ACLMessage.INFORM_REF);
				info.addReceiver(new AID ("Interface", AID.ISLOCALNAME));
				String cont = "Posição " + posicao.toString() + " Raio de proximidade " + raio;
				String cont1 = " Capacidade da estação " + capacidade + " bicicletas " + numBikesAtual + " bikes recebidas " + totalEntregue + 
						" bikes alugadas " + totalSaidas;
				String cont2 = " Incentivos oferecidos " + totalPedidos + " aceites " + totalPedidosAceites;
				info.setContent(cont+cont1+cont2);
				send(info);
				
			}
		});
	}

	protected void takeDown() {
		super.takeDown();
		try {
			DFService.deregister(this);
			System.out.println(this.getAID().getLocalName() + " encerrada");
		} catch (FIPAException e) {
			e.printStackTrace();
		}
	}
	
	//calcular desconto oferecido pela estação tendo em conta o número de bicicletas que nela se encontram
	public static int calculaDesconto(int capacidade, int numBikesAtual) {
		int percentagemDesconto = 0;
		
		if(numBikesAtual <= 0.10 * capacidade) percentagemDesconto = 50;
		if(numBikesAtual > 0.10 * capacidade && numBikesAtual <= 0.25 * capacidade) percentagemDesconto = 40;
		if(numBikesAtual > 0.25 * capacidade && numBikesAtual <= 0.50 * capacidade) percentagemDesconto = 30;
		if(numBikesAtual > 0.50 * capacidade && numBikesAtual <= 0.70 * capacidade) percentagemDesconto = 15;
		if(numBikesAtual > 0.70 * capacidade && numBikesAtual < 0.80 * capacidade) percentagemDesconto = 5;

		return percentagemDesconto;
	}
}