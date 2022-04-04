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
    public HashMap<Integer,ArrayList<Integer>> roadTo; //noeud a emprunter pour aller vers
    public Channel channel;
    public boolean visited;

    public PhysicalNode(int i){
        id = i;
        voisins = new ArrayList<Integer>();
        queuesIn = new ArrayList<String>();
        queuesOut = new ArrayList<String>();
        roadTo = new HashMap<>();
    }

    //Fonction pour initialiser le tableau de tous les noeuds
    public void init() throws java.io.IOException{
        //On envoie son id a tous les voisins
        String message ="i "+id;
        for(int i=0;i<voisins.size();i++){
            channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
        }


    }



    public static void main(String[] argv) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        PhysicalNode noeud;
        //Commande pour ajouter un noeud au noeud physique
        //PhysicalNode id [ids voisins]
        int id = Integer.parseInt(argv[0]);
        ArrayList<Integer> voisins = new ArrayList<>();
        for(int i = 1;i<argv.length;i++){
            voisins.add(Integer.parseInt(argv[i]));
        }

        //On creer le bon nombre de queues
        //Le premier creer, creer les queues dans les 2 sens (lui -> autre noeud) (autre noeud -> lui)
        ArrayList<String> queuesIn = new ArrayList<String>();
        ArrayList<String> queuesOut = new ArrayList<String>();
        for (int i=1;i<argv.length;i++){ //argv.length-1 -> on enleve l'id du noeud
            String queueName = id+"v"+argv[i];
            String queueName2 = argv[i]+"v"+id;

            String queueNameMsg = id+"v"+argv[i]+"msg";
            String queueName2Msg = argv[i]+"v"+id+"msg";

            channel.queueDeclare(queueName,false,false,false,null);
            channel.queueDeclare(queueName2,false,false,false,null);

            queuesIn.add(queueName2);
            queuesOut.add(queueName);
        }
        ArrayList<Integer> alreadyInit = new ArrayList<Integer>();
        HashMap<Integer,ArrayList<Integer>> roadTo = new HashMap<Integer,ArrayList<Integer>>(); //noeud a emprunter pour aller vers
        
        //Fonction a effectuer lors de la reception d'un message sur la couche physique
        DeliverCallback reponseInit = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(message);
            String[] arrayTMP = message.split(" ");
            if(arrayTMP[0].equals("r")){//On verifie le type (r ou i)
                //System.out.print(arrayTMP[0]);
                if(Integer.parseInt(arrayTMP[1]) == id){//On regarder si on est arrivé au noeud initiateur
                    //System.out.println("Arrivé");
                    int dernier = arrayTMP.length-1;
                    //System.out.println("dernier = "+arrayTMP[dernier]);
            //         if(roadTo.get(arrayTMP[dernier]).size() > arrayTMP.length - 2 ){
                        ArrayList<Integer> temp = new ArrayList<Integer>();
                        for(int j=2;j<arrayTMP.length;j++){//rempli roadTo avec le bon chemin
                            temp.add(Integer.parseInt(arrayTMP[j]));                            
                        }
                        roadTo.put(Integer.parseInt(arrayTMP[dernier]),temp);
                        if(roadTo.size()>0){
                            System.out.print("Road to  ");
                            for ( Integer key : roadTo.keySet() ) {
                                System.out.print( key +" =" );
                                for(int j=0;j<roadTo.get(key).size();j++){
                                    System.out.print(" "+roadTo.get(key).get(j));
                                }
                                System.out.println();
            
                            }
                        }
            //         }
                }else{//Si on est pas au noeud initiateur
                    // System.out.println("pas arrivé");
                    int j =1;
                    while(Integer.parseInt(arrayTMP[j])!=id){//On se positionne sur le noeud avant le courant pour lui envoyer
                        j++;
                     }
                    System.out.println("Mon id est "+arrayTMP[j]);
                     System.out.println("Je vais envoyé a "+arrayTMP[j-1]);
                    //  j = j-1;
                      channel.basicPublish("",id+"v"+arrayTMP[j-1],null,message.getBytes());
                }
            }else{//Si on est en cours d initialisation
                //System.out.print(arrayTMP[0]);
            //     //On s ajoute au chemin
                if(!alreadyInit.contains(Integer.parseInt(arrayTMP[1]))){
                    //System.out.println("hop");
                     alreadyInit.add(Integer.parseInt(arrayTMP[1]));
                     message = message.concat(" "+id);
                     for(int i=0;i<voisins.size();i++){
                         if(voisins.get(i)!= Integer.parseInt(arrayTMP[arrayTMP.length-1])){
                             channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
                         }else{
                             message = message.replaceFirst("i","r");
                             channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
                             message = message.replaceFirst("r","i");
                         }
                    }

            
                 }
            }

            
        };
      

        for(int i=0;i<queuesIn.size();i++){
            String queueName = queuesIn.get(i);
             channel.basicConsume(queueName, true, reponseInit, consumerTag -> { });
        }

        
        String message ="i "+id;
        for(int i=0;i<voisins.size();i++){
            channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
        }


        // for(int i=0;i<roadTo.size();i++){
        //     ArrayList temp = roadTo.get(i);
        //     for(int j=0;j<roadTo.get(i).size();j++){
        //         // System.out.println(" "+temp.get(j));

        //     }
        // }

        //System.out.println("Bonjor");

        //channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        //faut consommer toutes les queues entrantes donc faire boucle
      
       // int i=0;
        // while(true){
        //     String queueName = queuesIn.get(i%queuesIn.size());
        //     channel.basicConsume(queueName, true, reponseInit, consumerTag -> { });
        // }
        
        // System.out.println("Mon id est :"+id);
        // System.out.println("Mes voisins sont");
        // for(int i=0;i<voisins.size();i++){
        //     System.out.print(voisins.get(i)+" ");
        // }
        // System.out.println();
        // System.out.println("Nom de mes queues in");
        // for(int i=0;i<queuesIn.size();i++){
        //     System.out.println(queuesIn.get(i));
        // }
        // System.out.println("Nom de mes queues out");
        // for(int i=0;i<queuesOut.size();i++){
        //     System.out.println(queuesOut.get(i));
        // }

        //On envoye un message a un noeud et il l affiche
        //Si on envoye a plusieurs noeud ca doit marcher aussi
        
    }
}