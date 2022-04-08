//on fait sleep pour attendre que les routes se contruisent
import java.util.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
public class PhysicalNode implements VirtualNode{
    public int id;//id du noeud
    public ArrayList<Integer> voisins;//id des voisins
    public ArrayList<String> queuesIn;//nom des queues de voisin -> noeud
    public ArrayList<String> queuesOut;//nom des queues de noeuf -> voisin
    public HashMap<Integer,ArrayList<Integer>> roadTo; //noeud a emprunter pour aller vers
    public Channel channel;
    public boolean visited;

    //Attributs virtuels
    public int virtualId;
    public int voisinGauche;
    public int voisinDroite;

    public PhysicalNode(int i){
        id = i;
        voisins = new ArrayList<Integer>();
        queuesIn = new ArrayList<String>();
        queuesOut = new ArrayList<String>();
        roadTo = new HashMap<>();
    }

    public PhysicalNode(int i,ArrayList<Integer> v,ArrayList<String> q1, ArrayList<String> q2, HashMap<Integer,ArrayList<Integer>>  h,Channel c,int vi,int vg,int vd){
        id = i;
        voisins = v;
        queuesIn = q1;
        queuesOut = q2;
        roadTo  = h;
        channel =c;
        visited = false;
        virtualId=vi;
        voisinGauche=vg;
        voisinDroite=vd;
    }

    //Fonction pour initialiser le tableau de tous les noeuds
    public void init() throws java.io.IOException{
        //On envoie son id a tous les voisins
        String message ="i "+id;
        for(int i=0;i<voisins.size();i++){
            channel.basicPublish("",id+"v"+voisins.get(i),null,message.getBytes());
        }


    }

    void envoyerMessage(int id,int destinataire,String message)throws java.io.IOException{
        int voisin = roadTo.get(destinataire).get(0);
        StringBuilder sb = new StringBuilder(message);
        sb.insert(0,"m "+id+" "+destinataire+" ");
        //System.out.println(sb);
        message = sb.toString();
        channel.basicPublish("",id+"v"+voisin,null,message.getBytes());
    }
    public void sendRight(String message)throws java.io.IOException{
        envoyerMessage(id,voisinDroite,message);
    }

    public void sendLeft(String message)throws java.io.IOException{
        envoyerMessage(id,voisinGauche,message);
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
        int w=1;
        //Boucle pour initialiser noeud physique
        for(w = 1;w<argv.length-3;w++){
            voisins.add(Integer.parseInt(argv[w]));
        }

        //Initialisation du noeud virtuel
        int virtualId = Integer.parseInt(argv[w]);
        w++;
        int voisinGauche =  Integer.parseInt(argv[w]);
        w++;
        int voisinDroite = Integer.parseInt(argv[w]);


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
        noeud = new PhysicalNode(id,voisins,queuesIn, queuesOut,roadTo,channel,virtualId,voisinGauche,voisinDroite);
        //Fonction a effectuer lors de la reception d'un message sur la couche physique
        DeliverCallback reponseInit = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            //System.out.println(message);
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
                        // if(roadTo.size()>0){
                        //     System.out.print("Road to  ");
                        //     for ( Integer key : roadTo.keySet() ) {
                        //         System.out.print( key +" =" );
                        //         for(int j=0;j<roadTo.get(key).size();j++){
                        //             System.out.print(" "+roadTo.get(key).get(j));
                        //         }
                        //         System.out.println();
            
                        //     }
                        // }
            //         }
                }else{//Si on est pas au noeud initiateur
                    // System.out.println("pas arrivé");
                    int j =1;
                    while(Integer.parseInt(arrayTMP[j])!=id){//On se positionne sur le noeud avant le courant pour lui envoyer
                        j++;
                     }
                    // System.out.println("Mon id est "+arrayTMP[j]);
                    //  System.out.println("Je vais envoyé a "+arrayTMP[j-1]);
                    //  j = j-1;
                      channel.basicPublish("",id+"v"+arrayTMP[j-1],null,message.getBytes());
                }
            }else if(arrayTMP[0].equals("i")){//Si on est en cours d initialisation
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
            }else if(arrayTMP[0].equals("m")){
                //On fait rien de l'id du noeud qui envoie le message
                int id2 = Integer.parseInt(arrayTMP[1]);
                int destinataire = Integer.parseInt(arrayTMP[2]);
                String message2 = arrayTMP[3];
                StringBuilder sb = new StringBuilder(arrayTMP[3]);
                for(int i=4;i<arrayTMP.length;i++){
                     sb.append(" "+arrayTMP[i]);
                 }
                 //System.out.println("destinaire ="+ destinataire + " id = "+id);
                 message2 = sb.toString();
                 if(destinataire == id){
                    System.out.println(message2);
                }else{

                    noeud.envoyerMessage(id,destinataire,message2);
                }
            }else if(arrayTMP[0].equals("q")){
                System.exit(1);
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
        Thread.sleep(6000);
        Boolean exit=true;
        while(exit){
            Scanner sc = new Scanner(System.in);
            String messageInput = sc.nextLine();
            if(messageInput.equals("q")){
                exit=false;
                 
                for(int i=0;i<queuesIn.size();i++){
                    channel.basicPublish("",queuesOut.get(i),null,"q".getBytes());
                }
            }else{
                    String[] messagesArray = messageInput.split(" ");
                if(messagesArray[messagesArray.length - 1].equals("R")){
                    StringBuffer bs=new StringBuffer();
                    for(int s=0;s<=messagesArray.length - 2;s++){
                        bs.append(messagesArray[s]);
                        bs.append(" ");
                    }
                    noeud.sendRight(bs.toString());
                }else if(messagesArray[messagesArray.length - 1].equals("L")){
                    StringBuffer bs=new StringBuffer();
                    for(int s=0;s<=messagesArray.length - 2;s++){
                        bs.append(messagesArray[s]);
                        bs.append(" ");
                    }
                    noeud.sendLeft(bs.toString());
                }else{
                    System.out.println("Error: Faut choisir R ou L !");
                }
            }
            
        }
        //System.exit(1);
      //  System.out.println(messageInput);
      
        


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
