package il.cshaifasweng.OCSFMediatorExample.client;

public class GameEvent
{
    private final String msg;

    public GameEvent(String msg)
    {
        this.msg = msg;
    }

    public String getMsg()
    {
        return msg;
    }
}