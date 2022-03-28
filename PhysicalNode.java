import java.util.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
public class PhysicalNode{
    public int id;//id du noeud
    public ArrayList<Integer> voisins;//id des voisins
    public ArrayList<String> queuesIn;//nom des queues de voisin -> noeud
    public ArrayList<String> queuesOut;//nom des queues de noeuf -> voisin
    
    public PhysicalNode(int i){
        id = i;
        voisins = new ArrayList<Integer>();
        queuesIn = new ArrayList<String>();
        queuesOut = new ArrayList<String>();
    }

    public PhysicalNode(int i,ArrayList<Integer> v,ArrayList<String> q1, ArrayList<String> q2){
        id = i;
        voisins = v;
        queuesIn = q1;
        queuesOut = q2;
    }

    //Normalement pas besoin de ca car on creer le Noeud en une seule fois avec le constructeur du dessus
    public void ajouterVoisin(int i){
        voisins.add(i);
    }

    public void ajouterQueuesIn(String nomQueue){
        queuesIn.add(nomQueue);
    }

    public void ajouterQueuesOut(String nomQueue){
        queuesOut.add(nomQueue);
    }


    public static void main(String[] argv) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        PhysicalNode noeud;
        //Commande pour ajouter un noeud au noeud physique
        //PhysicalNode id [ids voisins]
        int monID = Integer.parseInt(argv[0]);
        //Pour l'instant on essaye de faire communiquer 2 noeuds physique entre eux
        //don chaque noeud a un seul voisin //Faire une boucle sur argv pour avoir la liste
        ArrayList<Integer> mesVoisins = new ArrayList<>();
        for(int i = 1;i<argv.length;i++){
            mesVoisins.add(Integer.parseInt(argv[i]));
        }

        //On creer le bon nombre de queues
        //Le premier creer, creer les queues dans les 2 sens (lui -> autre noeud) (autre noeud -> lui)
        ArrayList<String> queuesIn = new ArrayList<String>();
        ArrayList<String> queuesOut = new ArrayList<String>();
        for (int i=1;i<argv.length;i++){ //argv.length-1 -> on enleve l'id du noeud
            String queueName = monID+"v"+argv[i];
            String queueName2 = argv[i]+"v"+monID;

            channel.queueDeclare(queueName,false,false,false,null);
            channel.queueDeclare(queueName2,false,false,false,null);

            queuesIn.add(queueName2);
            queuesOut.add(queueName);
        }



        //Fonction a effectuer lors de la reception d'un message sur la couche physique
        //Pour l'instant on affiche le message
        //Faudra peut etre envoyer au voisin si id != idDestination (virtuel ou physique ?)
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        };

        if(monID == 1){
            String messageAEnvoyer = "Bonjour";
            for(int i=0;i<queuesOut.size();i++){
                String queueName = queuesOut.get(i);
                channel.basicPublish("",queueName,null,messageAEnvoyer.getBytes());
            }
        }



        //channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //faut consommer toutes les queues entrantes donc faire boucle
        for(int i=0;i<queuesIn.size();i++){
            String queueName = queuesIn.get(i);
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });

        }
        System.out.println("Mon id est :"+monID);
        System.out.println("Mes voisins sont");
        for(int i=0;i<mesVoisins.size();i++){
            System.out.print(mesVoisins.get(i)+" ");
        }
        System.out.println();
        System.out.println("Nom de mes queues in");
        for(int i=0;i<queuesIn.size();i++){
            System.out.println(queuesIn.get(i));
        }
        System.out.println("Nom de mes queues out");
        for(int i=0;i<queuesOut.size();i++){
            System.out.println(queuesOut.get(i));
        }

        //On envoye un message a un noeud et il l affiche
        //Si on envoye a plusieurs noeud ca doit marcher aussi
        
    }
}