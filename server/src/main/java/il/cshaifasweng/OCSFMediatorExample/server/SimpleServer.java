package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.ArrayList;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

//new import of random
import java.util.Random;

public class SimpleServer extends AbstractServer
{
	private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

	//we create x and o players. Later will assign them to 2 clients randomly
	private ConnectionToClient xPlayer = null;
	private ConnectionToClient oPlayer = null;

	// we will use random to give X and O to players
	private final Random rnd = new Random();

	public SimpleServer(int port)
	{
		super(port);
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client)
	{
		String s = msg.toString().trim();//was before we just added trim

		if (s.equals("join"))
		{
			handleJoin(client); //when join is clicked
			return;
		}

		if (s.startsWith("move"))
		{
			handleMove(s, client); //when button on a game board is clicked
			return;
		}

		if (s.equals("remove client"))
		{
			handleRemove(client); //when client app closes
		}
	}

	private void handleJoin(ConnectionToClient client)
	{
		try
		{
			if (isSubscribed(client))
			{
				client.sendToClient("info You already joined"); //we disable join button after joining but is is just user experience so we check it also in the server
				return;
			}
			if (SubscribersList.size() >= 2)
			{
				client.sendToClient("error Game is full"); //not more then 2 player can play xo
				return;
			}

			SubscribersList.add(new SubscribedClient(client)); //adding a player

			if (SubscribersList.size() == 1)
			{
				client.sendToClient("info Waiting for opponent...");// one player cannot play so we need second
				return;
			}

			startGame(); // if we passed all restrictions we can start a game!

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void startGame() throws IOException
	{
		// server only assigns symbols and tells clients who they are.

		ConnectionToClient p1 = SubscribersList.get(0).getClient(); //so here we get players from a list
		ConnectionToClient p2 = SubscribersList.get(1).getClient();

		boolean firstIsX = rnd.nextBoolean(); // here we create some random true/false
		xPlayer = firstIsX ? p1 : p2; //if true - player 1 is x
		oPlayer = firstIsX ? p2 : p1; // the opposite of previous line

		xPlayer.sendToClient("start X"); // tells first player his symbol
		oPlayer.sendToClient("start O"); // tells second player his symbol

		// (extra) server can also send info messages if you want
		xPlayer.sendToClient("info Game started");
		oPlayer.sendToClient("info Game started");
	}

	private void handleMove(String s, ConnectionToClient client)
	{
		try
		{
			if (xPlayer == null || oPlayer == null)
			{
				client.sendToClient("error Game not started yet"); //just in case
				return;
			}

			char me = symbolOf(client);
			if (me == '?')
			{
				client.sendToClient("error You are not a player");
				return;
			}

			String[] parts = s.split("\\s+"); //our move is "move row col" so we split it to strings
			if (parts.length != 3)
				return;

			int row = Integer.parseInt(parts[1]); // player move on row
			int col = Integer.parseInt(parts[2]); // player move on col

			if (row < 0 || row > 2 || col < 0 || col > 2) //just to be sure we are not out of grid!
				return;


			// We forward the move to BOTH clients with the symbol of the sender:
			// "move X row col" or "move O row col"
			String forwarded = "move " + me + " " + row + " " + col;

			xPlayer.sendToClient(forwarded);
			oPlayer.sendToClient(forwarded);
		}
		catch (Exception e)
		{
			try
			{
				client.sendToClient("error Bad move");
			}
			catch (IOException ignored){}
		}
	}

	private boolean isSubscribed(ConnectionToClient client)
	{
		for (SubscribedClient sc : SubscribersList) //just check if already in the game
		{
			if (sc.getClient().equals(client))
				return true;
		}
		return false;
	}

	private char symbolOf(ConnectionToClient client)
	{
		if (client.equals(xPlayer)) return 'X';
		if (client.equals(oPlayer)) return 'O';
		return '?'; //this is not suppose to happen but if we have some kind of a problem we will sent ? to signal it
	}

	private void handleRemove(ConnectionToClient client)
	{
		SubscribersList.removeIf(sc -> sc.getClient().equals(client));

		// if someone leaves - reset players so new game can start again
		if (client.equals(xPlayer)) xPlayer = null;
		if (client.equals(oPlayer)) oPlayer = null;

		try
		{
			if (xPlayer != null) xPlayer.sendToClient("info Opponent disconnected");
			if (oPlayer != null) oPlayer.sendToClient("info Opponent disconnected");
		}
		catch (IOException ignored) {}
	}

    /*public void sendToAllClients(String message) {
        try {
            for (SubscribedClient subscribedClient : SubscribersList) {
                subscribedClient.getClient().sendToClient(message);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }*/ //this was here so i dont delete it for now
}
