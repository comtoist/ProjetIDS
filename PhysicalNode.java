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
    public HashMap<Integer,Integer> roadTo; //noeud a emprunter pour aller vers
    public Channel channel;
    public boolean visited;

    public PhysicalNode(int i){
        id = i;
        voisins = new ArrayList<Integer>();
        queuesIn = new ArrayList<String>();
        queuesOut = new ArrayList<String>();
        roadTo = new HashMap<>();
    }

    public PhysicalNode(int i,ArrayList<Integer> v,ArrayList<String> q1, ArrayList<String> q2, HashMap<Integer,Integer>  h,Channel c){
        id = i;
        voisins = v;
        queuesIn = q1;
        queuesOut = q2;
        roadTo  = h;
        channel =c;
        visited = false;
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

    // public void getRoute(ArrayList h){
    //     // for(int i=0; i<voisins.size();i++){

    //     // }
    //     if(id==1){
    //         String temp = "2 "+id;//Est ce que dans tes voisins il y a 4 et on veut envoyer un message vers 4
    //         channel.basicPublish("","1v3",null,temp.getBytes());
    //     }
    //     channel.basicConsume("1v3", true, deliverCallback2, consumerTag -> { });
        
    // }
    
    // DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
    //     String message = new String(delivery.getBody(), "UTF-8");
    //     String[] arrayTMP = message.split(" ");
    //     int node = Integer.parseInt(arrayTMP[arrayTMP.length-1]);//Noeud vers lequel envoyer si trouvé

    //     int objectif = Integer.parseInt(message);
        

    //     if(voisins.contains(objectif)){
    //         channel.basicPublish("",id+"v"+node,null,message.getBytes());
    //         channel.basicPublish("",id+"v"+objectif+"msg",null,message.getBytes());
    //     }
    // };

    //Fonction pour initialiser le tableau de tous les noeuds
    public void init(){
        //On envoie son id a tous les voisins
        String message ="i "+id;
        for(int i=0;i<voisins.size();i++){
            channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
        }


    }


    
    DeliverCallback reponseInit = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), "UTF-8");
        String[] arrayTMP = message.split(" ");
        if(arrayTMP[0].equals("r")){//On verifie le type (r ou i)
            if(Integer.parseInt(arrayTMP[1]) == id){//On regarder si on est arrivé au noeud initiateur
                int dernier = arrayTMP.length;
                for(int j=2;j<arrayTMP.length-1;j++){//Si oui alors on rempli roadTo avec le bon chemin
                    if(roadTo.get(j).size() > arrayTMP.size() - 2 ){
                        roadTo.put(arrayTMP[dernier],arrayTMP[j]);
                    }
                }
            }else{//Si on est pas au noeud initiateur
                int j =0;
                while(j!=id){//On se positionne sur le noeud avant le courant pour lui envoyer
                    j++;
                }
                j = j-1;
                channel.basicPublish("",id+"v"+j,null,message.getBytes());
            }
        }else{//Si on est en cours d initialisation
            //On s ajoute au chemin
            if(visited == false){//A deplacer
                visited = true;
                message = message.concat(" "+id);
                for(int i=0;i<voisins.size();i++){
                    if(voisins.get(i)!= arrayTMP[arrayTMP.length-1]){
                        channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
                    }
                }
            }
        }
            // int node = Integer.parseInt(arrayTMP[arrayTMP.length-1]);//Noeud vers lequel envoyer si trouvé

        // int objectif = Integer.parseInt(message);
        

        // if(voisins.contains(objectif)){
        //     channel.basicPublish("",id+"v"+node,null,message.getBytes());
        //     channel.basicPublish("",id+"v"+objectif+"msg",null,message.getBytes());
        // }
    };



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

            String queueNameMsg = monID+"v"+argv[i]+"msg";
            String queueName2Msg = argv[i]+"v"+monID+"msg";

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