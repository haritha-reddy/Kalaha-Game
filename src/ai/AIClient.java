package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {
       int bestScore = 0; /* best value is assigned  */
       int bestPossibleMove= 0; /* best possible move made by player */
       int currentScore;
       int depth = 0; /* depth is assigned as zero */
       GameState node=new GameState();
       
       /* Creating a origin node and its clone*/
       GameState originNode = new GameState();
       originNode = currentBoard.clone();
       
       /* Checking for all the six ambos*/
       for(int i=1; i<7; i++)
       {
         if(originNode.moveIsPossible(i))
         {
           /* Choose the best possible move using minimax alpha beta pruning */
           currentScore = minMaxAlphaBeta(node, depth, i);
           if(currentScore > bestScore)
           {
             bestScore= currentScore;  /* Update the bestScore in case of a high current score*/
             bestPossibleMove=i;
           }
         }
       }
      return bestPossibleMove;
    }
    
 /* Returns the  utility value for the terminal nodes*/
    public int minMaxAlphaBeta(GameState node, int depth, int ambo)
    {
     int bestScore = Integer.MIN_VALUE; 
     int INFINITY = 0;
     int alpha = INFINITY; 
     int beta = -INFINITY;  
     int player = 1;
     int HOUSE_S = 7; /* the house of the AI client (player1) is in the 6th ambo*/
     
     /*Creation of a child node and its clone*/
     GameState childNode = new GameState();
     childNode = node.clone();
     int utility = node.board[HOUSE_S];
     
     
     /* The utility value is assigned to the nodes after a depth of six*/
     if(depth>= 6 || node.gameEnded())
     {
       return childNode.board[HOUSE_S];
     }
     else 
     {
               if(player ==1)
               {

      childNode.makeMove(ambo); /* The child node makes the move */
      
      
      
       for(int a=1;a<=6;a++) /* Check for all the six ambos */
       {
         alpha=Math.max(alpha, minMaxAlphaBeta(childNode, depth+1, a+1));
         
         if(alpha < bestScore)
         {
           alpha = bestScore;  /* Better move than before*/
         }   
         if(beta <= alpha)
         {
            //alpha is pruned in this case 
            break;
         }    
       }     
       return alpha; /*Our Best move*/
     } 
      else
          for(int a=1;a<=6;a++)
          {
              beta=Math.min(beta, minMaxAlphaBeta(childNode,depth+1,a+1));
              if(beta> bestScore)
              {
                  beta = bestScore; /* Better move than previous */
              }
              if(beta<=alpha)
              {
                  break; // beta is pruned here
              }
          }
      return beta; /* Opponent's best move */
     }
    }
}