import java.util.concurrent.Semaphore;
import java.util.*;
import java.io.File;  
import java.io.FileNotFoundException;

public class Project2 {
   private static int numCustomers = 50; // Numgber of customers to run
   private static int numBoxOffice = 2; // Variable for number of box office agents

   private static Semaphore boxOfficeMutex = new Semaphore(1); //Protects Queue for boxOffice orders
   private static Semaphore customerAtBox = new Semaphore(0); //Signals box office that cust is in queue
   private static Semaphore ticketResultMutex = new Semaphore(1); //Used for protecting ticket result queue
   private static Semaphore concessionMutex = new Semaphore(1); //Protects queue for concession orders 
   private static Semaphore concession = new Semaphore(0); //Signals concession worker that customer is in line
   private static Semaphore receiveFoodMutex = new Semaphore(1); //Protects queue to signal that food has been given to customers
   private static Semaphore ticketTakeQMutex = new Semaphore(1); //Protects queue of customers in line for ticket taker
   private static Semaphore ticketTaker = new Semaphore(0); //Signals ticket taker that customer is in line
   private static Semaphore movieListMutex = new Semaphore(1); //Protects lists of movies and their seat count 
   private static Semaphore custGetTicketResult = new Semaphore(0); //Signals to customers that their ticket order has been processed
   private static Semaphore custGetFoodResult = new Semaphore(0); //Signals to customers their food is ready
   private static Semaphore doneWithFood = new Semaphore(0); //Signals concession worker that Customer has received food. Helps maintain concurrency
   private static Semaphore doneWithBoxOffice = new Semaphore(0); //Signals to customers that their transaction has finished. Also helps maintain concurrency
   private static Semaphore doneWithTicketTaker = new Semaphore(0); //Signals to customers that they are done at the ticket taker
   
   static Queue<String> custTicketOrder = new LinkedList<String>(); //Customers in line/their order
   static Queue<Boolean> ticketResult = new LinkedList<Boolean>(); //Results for if movie was sold out
   static Queue<Boolean> foodOrderResult = new LinkedList<Boolean>(); //Results if food was given
   static Queue<String> custFoodOrder = new LinkedList<String>(); //Customer food orders
   static Queue<Integer> ticketTakerQ = new LinkedList<Integer>(); // Line of customers for ticket taker
   public static List<Integer> seats = new ArrayList<Integer>(); //Hold number of seats for each movie
   public static List<String> movies = new ArrayList<String>(); //Holds movies available
   Random rand = new Random(); //Used to randomize customer choices

   Project2(){}

  
	class Customer extends Thread {
		private int custID;
		private String movieChoice, foodOrder;
      private boolean getFood, validTicket, givenFood;

		Customer(int id){  //Initialize customer with id and choose movie and if will get food
			this.custID = id;
         chooseMovie();
         concessionVisitChoice();
		}

		private void chooseMovie(){ // randomized movie choice
			int randChoice = rand.nextInt(movies.size());
			this.movieChoice = movies.get(randChoice);
         System.out.println("Customer " + custID + " created, buying ticket to " + movieChoice);
         
		}

      private boolean concessionVisitChoice(){ //randomized choice for food
         int choice = rand.nextInt(2);
         if(choice == 1){
            this.getFood = true;
         }
         else{
            this.getFood = false;
         }  
         return this.getFood;
      }

      private void chooseOrder(){ //Randomly chooses food order if getting food
         int randChoice = rand.nextInt(3);
         if(randChoice == 0){
            this.foodOrder = "Popcorn";
         }
         else if(randChoice == 1){
            this.foodOrder = "Soda";
         }
         else{
            this.foodOrder = "Popcorn & Soda";
         }
      }

      private void addTicketOrderToQ(){ //adds ticket order to queue for box office
         custTicketOrder.add(movieChoice + "," + this.custID);
      }

      private void getTicket(){ // Gets result of ticket order
         this.validTicket = ticketResult.remove();
      }

      private void addFoodOrderToQ(){ //Adds food order to concession queue
         System.out.println("Customer " + this.custID + " in line to buy " + this.foodOrder);
         custFoodOrder.add(foodOrder + "," + this.custID);
      }

      private void getInTicketTakerLine(){ //Adds customer to ticket taker queue
         ticketTakerQ.add(this.custID);
         System.out.println("Customer " + this.custID + " in line to see ticket taker");
      }

      private void getFoodResult(){ //Gets result of getting food. True if food recieved
         this.givenFood = foodOrderResult.remove();
         System.out.println("Customer " + this.custID + " receives " + this.foodOrder);
      }

      private void goToMovie(){ //Prints customer entering theater for desired movie
         System.out.println("Customer " + this.custID + " enters theater to see " + this.movieChoice);
      }

      private void movieFull(){ //Prints if the movie was sold out
         System.out.println("Customer " + custID + " leaves theater because " + this.movieChoice + " is sold out");
      }


		public void run(){  //Customers actions along with the semaphores being used
			try{
               boxOfficeMutex.acquire();
               addTicketOrderToQ();
               customerAtBox.release();
               boxOfficeMutex.release();
               custGetTicketResult.acquire();
               ticketResultMutex.acquire();
               getTicket();
               ticketResultMutex.release();
               doneWithBoxOffice.acquire();

               if(this.validTicket == true){
                  ticketTakeQMutex.acquire();
                  getInTicketTakerLine();
                  ticketTaker.release();
                  ticketTakeQMutex.release();
                  doneWithTicketTaker.acquire();

                  if(this.getFood == true){
                     chooseOrder();
                     concessionMutex.acquire();
                     addFoodOrderToQ();
                     concession.release();
                     concessionMutex.release();
                     custGetFoodResult.acquire();
                     receiveFoodMutex.acquire();
                     getFoodResult();
                     receiveFoodMutex.release();
                     doneWithFood.release();
                  }

                  goToMovie();
               }
               else{
                  movieFull(); 
               }
         }
         catch(Exception e){
            System.out.println(e + "oops" + this.custID);
         }

		}

    }

    class BoxOfficeAgent extends Thread{
      private int agentId, numTickets, custID, movieAndTicketIndex;
      private String movie;
      private boolean validChoice = false;

      BoxOfficeAgent(int id){ //Initializes agent and prints it was created
         this.agentId = id;
         System.out.println("Box office agent " + agentId + " was created");
      }

      private void getCustChoice(){ //Gets the customers order
         String order = custTicketOrder.remove();
         String[] orderSplit = order.split(",", 0);
         this.movie = orderSplit[0];
         this.custID = Integer.parseInt(orderSplit[1]);
         this.movieAndTicketIndex = movies.indexOf(this.movie);
         this.numTickets = seats.get(this.movieAndTicketIndex);
         System.out.println("Box office agent " + agentId + " serving customer " + custID);
      }

      private void isValidChoice(){ //Checks if the movie is sold out or not
         if(this.numTickets >= 1){
            int temp = numTickets -1;
            seats.set(this.movieAndTicketIndex, temp);
            this.validChoice = true;
         }
         else{
            this.validChoice = false;
            noSeats();
       
         }
      }

      private void tellCustomerResult(){ //Enqueues result of ticket order
         ticketResult.add(this.validChoice);
      }

      private void sellTicket(){ //Prints and sleeps if ticket was sold
         try{
         Thread.sleep(1500);
         System.out.println("Box office agent " + this.agentId + " sold ticket for " + this.movie + " to customer " + this.custID);
         }
         catch(Exception e){
            System.out.println(e);
         }
      }

      private void noSeats(){ //Prints if movie is full
         System.out.println("Box office agent " + this.agentId + " informs customer " + this.custID + " that " + this.movie + " is sold out");
      }

      public void run(){ //Actions of box office agents
         while(true){
            try{
               customerAtBox.acquire();
               boxOfficeMutex.acquire();
               getCustChoice();
               boxOfficeMutex.release();
               movieListMutex.acquire();
               ticketResultMutex.acquire();
               isValidChoice();
               movieListMutex.release();
               tellCustomerResult();
               custGetTicketResult.release();
               ticketResultMutex.release();
               if(this.validChoice == true){
                  sellTicket();
                  doneWithBoxOffice.release();
               }
               else{
                  doneWithBoxOffice.release();
               }
            }
            catch(Exception e){
               System.out.println(e);
            }
         }

      }


    }

    class TicketTaker extends Thread{
      private int custID;

      TicketTaker(){ //Intialized ticket taker
         System.out.println("Ticker taker created");
      }
      private void serveCustomer(){ //Gets customer from ticket taker line queue
          try{
         Thread.sleep(250);
         this.custID = ticketTakerQ.remove();
         System.out.println("Ticket taken from customer " + this.custID);
         }
         catch(Exception e){
            System.out.println(e);
         }
      }
      public void run(){ //Actions followed by ticket taker
         while(true){
            try{
               ticketTaker.acquire();
               ticketTakeQMutex.acquire();
               serveCustomer();
               ticketTakeQMutex.release();
               doneWithTicketTaker.release();

            }
            catch(Exception e){
               System.out.println(e);
            }
         }


      }
    }

    class ConcessionWorker extends Thread{
      private int workID, custID;
      private String foodChoice;

      ConcessionWorker(){ //Initializes concession worker
         System.out.println("Concessions stand worker created");
      }

      private void getOrder(){ //gets customer food order from queue
         String input = custFoodOrder.remove();
         String[] orderSplit = input.split(",", 0);
         this.foodChoice = orderSplit[0];
         this.custID = Integer.parseInt(orderSplit[1]);
         System.out.println("Order for " + this.foodChoice + " taken from customer " + this.custID);   
      }

      private void giveOrder(){  //gives customer food and sleeps
          try{
         Thread.sleep(3000);
         foodOrderResult.add(true);
         System.out.println(this.foodChoice + " given to customer " + this.custID);
         }
         catch(Exception e){
            System.out.println(e);
         }
      }

       public void run(){ //action sequence of concession worker
         while(true){
            try{
               concession.acquire();
               concessionMutex.acquire();
               getOrder();
               concessionMutex.release();
               receiveFoodMutex.acquire();
               giveOrder();
               custGetFoodResult.release();
               receiveFoodMutex.release();
               doneWithFood.acquire();
               

            }
            catch(Exception e){
               System.out.println(e);
            }
         }


      }


    }


   public static void main(String args[])
   {
      Project2 ts = new Project2(); //Initialize theater

      try {    //used to read file
         File movieFile = new File("movies.txt");
         // File movieFile = new File(args[0]); 
         Scanner myReader = new Scanner(movieFile);
         while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            String[] inputSplit = data.split("\t", -1);
            movies.add(inputSplit[0]);
            seats.add(Integer.parseInt(inputSplit[1]));
            }
         myReader.close();
      } 
      catch (FileNotFoundException e) {
         System.out.println("An error occurred.");
         e.printStackTrace();
      }

      for(int i=0; i < numBoxOffice;i++){ //start box office threads
         BoxOfficeAgent agent = ts.new BoxOfficeAgent(i);
         agent.start();
      }

      TicketTaker t = ts.new TicketTaker(); //starts ticket taker
      t.start();

      ConcessionWorker cWorker = ts.new ConcessionWorker(); //starts concession worker thread
      cWorker.start();

      Thread[] customerThread = new Thread[numCustomers]; //starting customer threads
       for(int i = 0; i < numCustomers; i++){
         customerThread[i] = ts.new Customer(i);
         customerThread[i].start();
      }

      for(int i = 0; i < numCustomers ; ++i){ //joining all customer threads
         try{
            customerThread[i].join();
            System.out.println("Joined customer " + i);
         }
         catch(Exception e){
            System.out.println(e);
         }
      }
      System.exit(0);


   }
}




