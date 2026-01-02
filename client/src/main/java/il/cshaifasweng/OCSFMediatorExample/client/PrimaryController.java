package il.cshaifasweng.OCSFMediatorExample.client;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class PrimaryController {

	@FXML private Button b00,b01,b02,b10,b11,b12,b20,b21,b22;
	@FXML private Label statusLabel;
	@FXML private Button joinButton;

	private char mySymbol = '?';

	//Client-side board because lab wants game on client side
	private final char[] board = new char[9]; //This will store moves as row*3+col instead of 2d array
	private char turn = 'X'; // X starts (client-side rule)
	private boolean gameActive = false; //signals if there is a game right now. No game at start

	@FXML
	void initialize() {
		EventBus.getDefault().register(this);

		statusLabel.setText("Press Join Game");
		setBoardDisabled(true);

		resetGame(); //initialize board to '-'
	}

	@FXML
	private void onJoinGame(ActionEvent event) {
		statusLabel.setText("Waiting for second player");
		joinButton.setDisable(true);

		try {
			SimpleClient.getClient().sendToServer("join");
		} catch (IOException e) {
			statusLabel.setText("Failed to join");
			joinButton.setDisable(false);
		}
	}

	@FXML
	private void onButtClicked(ActionEvent event)
	{

		if (!gameActive)
			return; //if game not started ignore clicks

		if (mySymbol != turn)
		{
			statusLabel.setText("Not your turn"); //to avoid double moves by any player
			return;
		}

		Button clicked = (Button) event.getSource();
		String id = clicked.getId(); //We can get a move from the button name

		int row = id.charAt(1) - '0';
		int col = id.charAt(2) - '0';

		int idx = row * 3 + col; //our moves are stored in 1d array instead of 2d
		if (board[idx] != '-')
		{
			return; // "-" signals that cell is empty and we can move there. We disable it visually but just to be sure on code level
		}

		try
		{
			SimpleClient.getClient().sendToServer("move " + row + " " + col);//here we are sending our move as a string to server to handle (server forwards it)
		}
		catch (IOException e)
		{
			statusLabel.setText("Houston we have a problem"); //if it disconnects
		}
	}

	@Subscribe
	public void onGameEvent(GameEvent e) {
		Platform.runLater(() -> handleServerMsg(e.getMsg()));
	}

	private void handleServerMsg(String msg) {

		// server sends start X / start O
		if (msg.startsWith("start "))
		{
			mySymbol = msg.charAt(6); // X or O
			resetGame(); //clean board for new game
			gameActive = true; //signals if there is a game right now. No game at start

			// X starts
			turn = 'X';

			statusLabel.setText("You are " + mySymbol);
			updateTurnUI(); //show "Your turn" / "Opponent turn"
			return;
		}

		// server forwards: move X row col  OR  move O row col
		if (msg.startsWith("move "))
		{
			String[] parts = msg.split("\\s+"); //our move is "move symbol row col" so we split it so strings to get "number"
			if (parts.length != 4)
				return;

			char symbol = parts[1].charAt(0);

			int row = Integer.parseInt(parts[2]); // player move on row
			int col = Integer.parseInt(parts[3]); // player move on col

			if (row < 0 || row > 2 || col < 0 || col > 2) //just to be sure we are not out of grid!
				return;

			int idx = row * 3 + col; //our moves are stored in 1d array instead of 2d
			if (board[idx] != '-')
				return;

			board[idx] = symbol; //apply move locally

			applyBoardToButtons(); //we are updating play board after each move!

			// Client checks winner/draw (lab requirement)
			Character winner = checkWinner(board);
			if (winner != null) {
				gameActive = false;
				setBoardDisabled(true);
				showResultPopup("Winner is: " + winner); //Telling players who won
				return;
			}

			if (isDraw(board)) {
				gameActive = false;
				setBoardDisabled(true);
				showResultPopup("Its a DRAW"); //both loosers
				return;
			}

			//turn switches
			turn = (turn == 'X') ? 'O' : 'X';
			updateTurnUI(); //telling whos turn it is

			return;
		}

		// info/error messages
		if (msg.startsWith("info "))
		{
			statusLabel.setText(msg.substring(5));
			return;
		}

		if (msg.startsWith("error "))
		{
			statusLabel.setText(msg.substring(6));
			return;
		}
		statusLabel.setText(msg);
	}

	private void updateTurnUI()
	{
		//label on top of board will show whos turn it is
		if (!gameActive)
		{
			setBoardDisabled(true);
			return;
		}

		if (turn == mySymbol)
		{
			statusLabel.setText("Your turn");
			setBoardDisabled(false);
			disableFilledCells();
		}
		else
		{
			statusLabel.setText("Opponent turn");
			setBoardDisabled(true);
		}
	}

	private void applyBoardToButtons()
	{
		setBtn(b00, board[0]);
		setBtn(b01, board[1]);
		setBtn(b02, board[2]);
		setBtn(b10, board[3]);
		setBtn(b11, board[4]);
		setBtn(b12, board[5]);
		setBtn(b20, board[6]);
		setBtn(b21, board[7]);
		setBtn(b22, board[8]);

		disableFilledCells(); //here we disable already clicked buttons
	}

	private void setBtn(Button btn, char ch)
	{
		btn.setText(ch == '-' ? "" : String.valueOf(ch));
	}

	private void disableFilledCells()
	{
		disableIfFilled(b00, board[0]); //here we disable already clicked buttons
		disableIfFilled(b01, board[1]);
		disableIfFilled(b02, board[2]);
		disableIfFilled(b10, board[3]);
		disableIfFilled(b11, board[4]);
		disableIfFilled(b12, board[5]);
		disableIfFilled(b20, board[6]);
		disableIfFilled(b21, board[7]);
		disableIfFilled(b22, board[8]);
	}

	private void disableIfFilled(Button btn, char ch)
	{
		if (ch != '-') btn.setDisable(true);
	}

	private void setBoardDisabled(boolean disabled)
	{
		b00.setDisable(disabled);
		b01.setDisable(disabled);
		b02.setDisable(disabled);
		b10.setDisable(disabled);
		b11.setDisable(disabled);
		b12.setDisable(disabled);
		b20.setDisable(disabled);
		b21.setDisable(disabled);
		b22.setDisable(disabled);
	}

	private void showResultPopup(String text) {
		try
		{
			ResultPopup.show(text);
		}
		catch (Exception ex)
		{
			statusLabel.setText(text);
		}
	}

	private void resetGame()
	{
		for (int i = 0; i < 9; i++)
			board[i] = '-'; //All moves are "-"
		applyBoardToButtons();
	}

	private Character checkWinner(char[] b)
	{
		int[][] lines =
				{
				{0,1,2},{3,4,5},{6,7,8}, //if one of those lines is full - we have a winner
				{0,3,6},{1,4,7},{2,5,8},
				{0,4,8},{2,4,6}
		};
		for (int[] L : lines)
		{
			char a = b[L[0]];
			if (a != '-' && a == b[L[1]] && a == b[L[2]])
				return a;
		}
		return null;
	}

	private boolean isDraw(char[] b) //simple draw check
	{
		for (char ch : b)
			if (ch == '-')
				return false;
		return true;
	}
}
