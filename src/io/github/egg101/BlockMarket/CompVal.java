package io.github.egg101.BlockMarket;

public class CompVal
{
    private int lmbrQueue = 0;
    private int carpQueue = 0;
    private int mineQueue = 0;
    private int masnQueue = 0;
    private int digrQueue = 0;

    	// LMBR
    public void addlmbrQ(int value)
    {
        lmbrQueue = lmbrQueue + value;
    }
    public void sublmbrQ(int value)
    {
        lmbrQueue = lmbrQueue - value;
    }
    public int getlmbrQ()
    {
        return lmbrQueue;
    }
    

		// CARP
	public void addcarpQ(int value)
	{
	    carpQueue = carpQueue + value;
	}
	public void subcarpQ(int value)
	{
	    carpQueue = carpQueue - value;
	}
	public int getcarpQ()
	{
	    return carpQueue;
	}
	
		// MINE
	public void addmineQ(int value)
	{
	    mineQueue = mineQueue + value;
	}
	public void submineQ(int value)
	{
	    mineQueue = mineQueue - value;
	}
	public int getmineQ()
	{
	    return mineQueue;
	}

		// MASN
	public void addmasnQ(int value)
	{
	    masnQueue = masnQueue + value;
	}
	public void submasnQ(int value)
	{
	    masnQueue = masnQueue - value;
	}
	public int getmasnQ()
	{
	    return masnQueue;
	}
    

		// DIGR
	public void adddigrQ(int value)
	{
	    digrQueue = digrQueue + value;
	}
	public void subdigrQ(int value)
	{
	    digrQueue = digrQueue - value;
	}
	public int getdigrQ()
	{
	    return digrQueue;
	}

}