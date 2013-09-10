package io.github.egg101.BlockMarket;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
//import java.text.SimpleDateFormat;
import java.util.Calendar;
//import java.util.Date;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.MySQL;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockMarket extends JavaPlugin{
	Logger log;
	private Database mysql;
    public static Economy econ = null;
    String validCompanies = "lumberjacks carpenters miners masons diggers";
    String validSymbols = "LMBR CARP MINE MASN DIGR";
    Plugin plugin;
	   
        		
    @Override
    public void onEnable(){
		log = this.getLogger();
		log.info("Enabled");
		plugin = getServer().getPluginManager().getPlugin("BlockMarket");

		// Set up config
	    File file = new File(this.getDataFolder(), "config.yml");
	    if (!file.exists()) {
			log.info("Creating config.yml...");
	        this.saveDefaultConfig();
			log.info("Successfully created config.yml!");
	    }
	    
	    // Set up SQL
        sqlConnection();
        
        // Set up economy through Vault
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Set up time when stocks update
        setupChangeTime(plugin);

        // Set up repeating task for adding up value every 30 minutes
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
            	log.info("queueing 1");
            	int lmbrVal = get_int("companies","queue_val","name","lumber");
        		int carpVal = get_int("companies","queue_val","name","carpenters");
        		int mineVal = get_int("companies","queue_val","name","miners");
        		int masnVal = get_int("companies","queue_val","name","masons");
        		int digrVal = get_int("companies","queue_val","name","diggers");

            	log.info("queueing 2");
            	Calendar now = Calendar.getInstance();
            	now.add(Calendar.MINUTE, -30);
            	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            	String dfStr = df.format(now.getTime());

            	log.info("queueing 3");
            	ResultSet rs;
        		try {
        			rs = mysql.query("SELECT * FROM `lb-"+getConfig().getString("world")+"` WHERE date >= '"+dfStr+"';");
        		} catch (SQLException e) {
                    rs = null;
                	e.printStackTrace();
                }

            	log.info("queueing 4");
            	try {
            		rs.first();
	        	    do {
	                	log.info("queueing INSIDE 1");
	                	log.info(String.valueOf(rs.getInt("type")));
	        	        switch (rs.getInt("type")) {
	        	        	  // CARP
	        	        	case 5:
	        	        		if (rs.getInt("data") == 1) { //spruce plank
	        	        			carpVal = carpVal + getConfig().getInt("carpenters.value.placespruceplank");
	        	        		} else if (rs.getInt("data") == 2) { //birch plank
	            	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placebirchplank");
	            	        	} else if (rs.getInt("data") == 3) { //jungle plank
	        	        			carpVal = carpVal + getConfig().getInt("carpenters.value.placejungleplank");
	            	        	} else { // oak plank
	            	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placeoakplank");
	            	        	}
	        	        		break;
	        	        	case 53: //oak stairs
	        	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placeoakstairs");
	        	        		break;
	        	        	case 134: //spruce stairs
	        	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placesprucestairs");
	        	        		break;
	        	        	case 135: //birch stairs
	        	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placebirchstairs");
	        	        		break;
	        	        	case 136: //jungle stairs
	        	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placejunglestairs");
	        	        		break;
	        	        	case 44: //oak slab
	        	        		if (rs.getInt("data") == 2) { //oak slab
	        	        			carpVal = carpVal + getConfig().getInt("carpenters.value.placeoakslab");
	        	        		} else if (rs.getInt("data") == 3) { //cobble slab
	        	        			masnVal = masnVal + getConfig().getInt("masons.value.placecobbleslab");
	        	        		} else if (rs.getInt("data") == 4) { //brick slab
	        	        			masnVal = masnVal + getConfig().getInt("masons.value.placebrickslab");
	        	        		} else if (rs.getInt("data") == 5) { //sb slab
	        	        			masnVal = masnVal + getConfig().getInt("masons.value.placestonebrickslab");
	        	        		} else { // stone slab
	        	        			masnVal = masnVal + getConfig().getInt("masons.value.placestoneslab");
	        	        		}
	        	        		break;
	        	        	case 126: //slabs
	        	        		if (rs.getInt("data") == 1) { //spruce slab
	        	        			carpVal = carpVal + getConfig().getInt("carpenters.value.placespruceslab");
	        	        		} else if (rs.getInt("data") == 2) { //birch slab
	            	        		carpVal = carpVal + getConfig().getInt("carpenters.value.placebirchslab");
	            	        	} else if (rs.getInt("data") == 3) { //jungle slab
	        	        			carpVal = carpVal + getConfig().getInt("carpenters.value.placejungleslab");
	            	        	}
	        	        		break;
	        	        		
	        	        	  // MASN
	        	        	case 1: //stone
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placestone");
	        	        		break;
	        	        	case 4: //cobble
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placecobble");
	        	        		break;
	        	        	case 45: //brick
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placebrick");
	        	        		break;
	        	        	case 98: //stonebrick
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placestonebrick");
	        	        		break;
	        	        	case 67: //cobblestairs
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placecobblestairs");
	        	        		break;
	        	        	case 108: //brickstairs
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placebrickstairs");
	        	        		break;
	        	        	case 109: //stonebrickstairs
	        	        		masnVal = masnVal + getConfig().getInt("masons.value.placestonebrickstairs");
	        	        		break;
	        	        	// case 44: SEE CARP ABOVE
	        	        } // end creation switch
	
	        	        	// Destruction
	                	log.info("queueing INSIDE 2");
	        	        switch (rs.getInt("replaced")) {
	        	        	  // MINE
	        	        	case 1: // stone
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breakstone");
	        	        		break;
	        	        	case 16: // coal
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breakcoal");
	        	        		break;
	        	        	case 15: // iron
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breakiron");
	        	        		break;
	        	        	case 14: // gold
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breakgold");
	        	        		break;
	        	        	case 73: // redstone
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breakredstone");
	        	        		break;
	        	        	case 21: // lapis
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breaklapis");
	        	        		break;
	        	        	case 56: // diamond
	        	        		mineVal = mineVal + getConfig().getInt("miners.value.breakdiamond");
	        	        		break;
	        	        		
	          	        	  // DIGR
	          	        	case 2: // grass
	          	        		digrVal = digrVal + getConfig().getInt("diggers.value.breakgrass");
	          	        		break;
	          	        	case 3: // dirt
	          	        		digrVal = digrVal + getConfig().getInt("diggers.value.breakdirt");
	          	        		break;
	          	        	case 12: // sand
	          	        		digrVal = digrVal + getConfig().getInt("diggers.value.breaksand");
	          	        		break;
	          	        	case 13: // gravel
	          	        		digrVal = digrVal + getConfig().getInt("diggers.value.breakgravel");
	          	        		break;
	          	        	case 82: // clay
	          	        		digrVal = digrVal + getConfig().getInt("diggers.value.breakclay");
	          	        		break;
		        	        	  // LMBR
	        	        	case 17:
	        	        		if (rs.getInt("data") == 1) { //spruce log
	        	        			lmbrVal = lmbrVal + getConfig().getInt("lumberjacks.value.breaksprucelog");
	        	        		} else if (rs.getInt("data") == 2) { //birch log
	            	        		lmbrVal = lmbrVal + getConfig().getInt("lumberjacks.value.breakbirchlog");
	            	        	} else if (rs.getInt("data") == 3) { //jungle log
	        	        			lmbrVal = lmbrVal + getConfig().getInt("lumberjacks.value.breakjunglelog");
	            	        	} else { // oak log
	            	        		lmbrVal = lmbrVal + getConfig().getInt("lumberjacks.value.breakoaklog");
	            	        	}
	        	        		break;
	        	        		
	        	        } // end destruction switch
	        	    } while (rs.next());
        	    } catch (SQLException e) {
            		e.printStackTrace();
            	}
            	log.info("queueing 5");
            	try {
					mysql.query("UPDATE companies SET queue_val="+lmbrVal+" WHERE name='lumberjacks'");
					mysql.query("UPDATE companies SET queue_val="+carpVal+" WHERE name='carpenters'");
					mysql.query("UPDATE companies SET queue_val="+mineVal+" WHERE name='miners'");
					mysql.query("UPDATE companies SET queue_val="+masnVal+" WHERE name='masons'");
					mysql.query("UPDATE companies SET queue_val="+digrVal+" WHERE name='diggers'");
				} catch (SQLException e) {
					e.printStackTrace();
				}
            }
        }, 800L, 800L); // 36000
    }
    @Override
    public void onDisable() {
		log.info("Disabled");
    	mysql.close();
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		Player player = (Player) sender;
		String playername = player.getName();
		boolean error = false;

    	if(cmd.getName().equalsIgnoreCase("bm")){
    		// /bm help
    		if (args.length == 1 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?"))) {
    			player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "BlockMarket Commands" + ChatColor.DARK_PURPLE +"----");
				player.sendMessage(ChatColor.GREEN + "/bm buy [company] [#]" + ChatColor.AQUA + "  Buys stocks from a company.");
				player.sendMessage(ChatColor.GREEN + "/bm sell [company] [#]" + ChatColor.AQUA + "  Sells stocks back to a company. There is tax for returning stocks!");
				player.sendMessage(ChatColor.GREEN + "/bm info [company]" + ChatColor.AQUA + "  Shows info about the company.");
				player.sendMessage(ChatColor.GREEN + "/bm ticker" + ChatColor.AQUA + "  Shows how much the value of 1 share changed since yesterday.");
				player.sendMessage(ChatColor.GREEN + "/bm portfolio" + ChatColor.AQUA + "  Shows what stocks you have in what companies and their values.");
				player.sendMessage(ChatColor.GREEN + "/bm list companies" + ChatColor.AQUA + "  Lists the available companies and their ticker symbols.");
				player.sendMessage(ChatColor.GREEN + "/bm list shareholders [company]" + ChatColor.AQUA + "  Lists the top shareholders in a company.");
				player.sendMessage(ChatColor.GREEN + "/bm give [player] [company] [#]" + ChatColor.AQUA + "  Transfers a stock to another player. There is no tax.");
				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------------");
    		} // end /bm help
    		
			//-------- /bm buy [company] [amount]
    		if (args.length == 3 && args[0].equalsIgnoreCase("buy")){
    			String companyName = args[1].toLowerCase();
    			int amount = Integer.parseInt(args[2]);
    			
    			// Preliminary error check before SQL happens
    			companyName = company_name_exists(player,companyName);
    			if (companyName == "error") { return false; } // Giving error message is already handled in method. Exits onCommand
    			
    			int sql_shares_unsold = get_int("companies","shares_unsold","name",companyName);
    			int sql_shares_sold = get_int("companies","shares_sold","name",companyName);
    			int sql_value = get_int("companies","value","name",companyName);
    			int sql_share_amt = get_int("companies","share_amt","name",companyName);
    			int sql_playershares = get_int("shareholders",companyName,"playername",playername);
    			
    			int shares_left = sql_shares_unsold - amount;
    			int shares_sold_upd = sql_shares_sold + amount;
    			double cost = amount * (sql_value / sql_share_amt);
    			cost = round(cost, 2);
    			int newshareamount = sql_playershares + amount;

    			// Error checking
    			if (getConfig().getBoolean(companyName+".open") == false) {
    				player.sendMessage("[BlockMarket] Buying and selling shares in that company is closed.");
    				error = true;
    			}
    			if (sql_shares_unsold == 0) {
    				player.sendMessage("[BlockMarket] All shares in that company are sold out.");
    				error = true;
    			}
    			if (amount == 0) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "You can't buy 0 shares.");
    				error = true;
    			}
    			if (amount > sql_shares_unsold ) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "You can't buy that many shares. There are only " + sql_shares_unsold + " shares for sale in that company.");
    				error = true;
    			}
    			if (econ.getBalance(playername) < cost) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "You don't have enough money.");
    				error = true;
    			}
    			
    			// If no errors:
    			if (error == false) {
    				// Remove unsold shares from company
    				try {
						mysql.query("UPDATE companies SET shares_unsold="+shares_left+",shares_sold="+shares_sold_upd+" WHERE name='"+companyName+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
    				// Create player entry with default values if doesn't exist
    				if (entry_exists("shareholders","playername",playername) == 0) {
    					try {
							mysql.query("INSERT INTO shareholders VALUES ('"+playername+"',0,0,0,0,0);");
						} catch (SQLException e) {
							e.printStackTrace();
						}
    				}
    				// Add shares bought to player
					try {
						mysql.query("UPDATE shareholders SET "+companyName+"="+newshareamount+" WHERE playername='"+playername+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
					
					// Take due money from player
					econ.withdrawPlayer(playername, cost);
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.GREEN + "Successfully bought "+amount+" shares for $"+cost+".");

    			}
			} // END /bm buy [company] [amount]

			//-------- /bm sell [company] [amount]
    		if (args.length == 3 && args[0].equalsIgnoreCase("sell")){
    			String companyName = args[1].toLowerCase();
    			int amount = Integer.parseInt(args[2]);
    			
    			// Preliminary error check before SQL happens
    			companyName = company_name_exists(player,companyName);
    			if (companyName == "error") { return false; } // Giving error message is already handled in method. Exits onCommand

    			int sql_shares_unsold = get_int("companies","shares_unsold","name",companyName);
    			int sql_shares_sold = get_int("companies","shares_sold","name",companyName);
    			int sql_value = get_int("companies","value","name",companyName);
    			int sql_share_amt = get_int("companies","share_amt","name",companyName);
    			int sql_playershares = get_int("shareholders",companyName,"playername",playername);
    			double cnf_returntax = getConfig().getDouble(companyName+".returntax");

    			int shares_sold_upd = sql_shares_sold - amount;
    			int shares_updated = amount + sql_shares_unsold; // New share amt for company
    			int newshareamount = sql_playershares - amount; // New share amt for player
    			
    			double payment = amount * (sql_value / sql_share_amt) * (1-cnf_returntax);
    			payment = round(payment, 2);
    			
    			// Error checking
    			if (getConfig().getBoolean(companyName+".open") == false) {
    				player.sendMessage("[BlockMarket] Buying and selling shares in that company is closed.");
    				error = true;
    			}
    			if (amount == 0) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "You can't sell 0 shares.");
    				error = true;
    			}
    			if (sql_playershares < amount) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "You only have " +sql_playershares+" shares to sell.");
    				error = true;
    			}
    			
    			if (error == false) {
    				// Removes shares sold from player
					try {
						mysql.query("UPDATE shareholders SET "+companyName+"="+newshareamount+" WHERE playername='"+playername+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
					
    				// Give shares back to company
    				try {
						mysql.query("UPDATE companies SET shares_unsold="+shares_updated+",shares_sold="+shares_sold_upd+" WHERE name='"+companyName+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
    				
    				// Gives money to player
					econ.depositPlayer(playername, payment);
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.GREEN + "Successfully sold "+amount+" shares for $"+payment+". (Return tax was "+cnf_returntax*100+"%)");
    				
    			}
			} // end /bm sell [company] [amount]

    		//Gets [company] from companies.
    		//Gets symbol, value, share_amt, price (value/share_amt), returntax, and shares_unsold and lists.
			//-------- /bm info [company]
    		if (args.length == 2 && args[0].equalsIgnoreCase("info")){
    			String companyName = args[1].toLowerCase();
    			String showCompanyName = companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
    			
    			// Preliminary error check before SQL happens
    			companyName = company_name_exists(player,companyName);
    			if (companyName == "error") { return false; } // Giving error message is already handled in method. Exits onCommand
    			
    			String sql_symbol =		get_str("companies","symbol","name",companyName);
    			int sql_lastchange =	get_int("companies","lastchange","name",companyName);
    			int sql_value = 		get_int("companies","value","name",companyName);
    			int sql_share_amt = 	get_int("companies","share_amt","name",companyName);
    			double price = sql_value / sql_share_amt;
    			price = round(price, 2);
    			int sql_shares_unsold = get_int("companies","shares_unsold","name",companyName);
    			double cfg_returntax = 	getConfig().getDouble(companyName + ".returntax");
    			
    			String lastchangePrefix = "";
    			if (sql_lastchange > 0) { lastchangePrefix = "§a +"; }
    			else if (sql_lastchange < 0) { lastchangePrefix = "§c "; } // no - prefix because it is already on the value

				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Info for "+showCompanyName+ ChatColor.DARK_PURPLE +"----");
				player.sendMessage(ChatColor.BLUE + "Ticker: " + ChatColor.AQUA + sql_symbol + " " + lastchangePrefix + sql_lastchange);
				player.sendMessage(ChatColor.BLUE + "Company value: " + ChatColor.AQUA + "$" + sql_value);
				player.sendMessage(ChatColor.BLUE + "Total shares in company: " + ChatColor.AQUA + sql_share_amt);
				player.sendMessage(ChatColor.BLUE + "Share price: " + ChatColor.AQUA + "$" + price);
				player.sendMessage(ChatColor.BLUE + "Shares for sale: " + ChatColor.AQUA + sql_shares_unsold);
				player.sendMessage(ChatColor.BLUE + "Return tax: " + ChatColor.AQUA + cfg_returntax + "%");
				player.sendMessage(ChatColor.DARK_PURPLE +"--------------------");
    			
			} // end /bm info [company]

			//-------- /bm ticker
    		if (args.length == 1 && args[0].equalsIgnoreCase("ticker")){
    			String[] companyNames = {"lumberjacks", "carpenters", "miners", "masons", "diggers"};
    			String[] tickerSymbols = new String[5];
    			double[] lastChanges = new double[5];

    			for (int i = 0; i<5; i++) {
    				tickerSymbols[i] = get_str("companies","symbol","name",companyNames[i]);
        			lastChanges[i] = get_double("companies","lastchange","name",companyNames[i]);
    			}
    			

    			String combinedMessage = "§5Ticker:   ";
    			for (int i = 0; i<5; i++) {
    				combinedMessage = combinedMessage.concat("§7"+tickerSymbols[i]);

    				if (lastChanges[i] > 0) {
    					combinedMessage = combinedMessage.concat("§a +");
    				} else if (lastChanges[i] < 0) {
    					combinedMessage = combinedMessage.concat("§c ");
    				} else {
    					combinedMessage = combinedMessage.concat("§f ");
    				}

					combinedMessage = combinedMessage.concat(String.valueOf(lastChanges[i]) + "   ");
    			}
    			player.sendMessage(combinedMessage);
			} // end /bm ticker

			//-------- /bm portfolio (username)
    		if (args.length > 0 && args[0].equalsIgnoreCase("portfolio")){
    			if (args.length == 1) { // if no player specified
    				int sharesLMBR = get_int("shareholders","lumberjacks","playername",playername);
    				int sharesCARP = get_int("shareholders","carpenters","playername",playername);
    				int sharesMINE = get_int("shareholders","miners","playername",playername);
    				int sharesMASN = get_int("shareholders","masons","playername",playername);
    				int sharesDIGR = get_int("shareholders","diggers","playername",playername);
    				
    				int playerValueLMBR = sharesLMBR*(get_int("companies","value","name","lumberjacks"))/(get_int("companies","share_amt","name","lumberjacks"));
    				int playerValueCARP = sharesCARP*(get_int("companies","value","name","carpenters"))/(get_int("companies","share_amt","name","carpenters"));
    				int playerValueMINE = sharesMINE*(get_int("companies","value","name","miners"))/(get_int("companies","share_amt","name","miners"));
    				int playerValueMASN = sharesMASN*(get_int("companies","value","name","masons"))/(get_int("companies","share_amt","name","masons"));
    				int playerValueDIGR = sharesDIGR*(get_int("companies","value","name","diggers"))/(get_int("companies","share_amt","name","diggers"));

    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Your Stock Portfolio" + ChatColor.DARK_PURPLE +"----");
    				player.sendMessage(ChatColor.BLUE + "Lumberjacks (LMBR): " + ChatColor.AQUA + sharesLMBR + " shares, total value " + ChatColor.GREEN + "$"+playerValueLMBR);
    				player.sendMessage(ChatColor.BLUE + "Carpenters (CARP): " + ChatColor.AQUA + sharesCARP + " shares, total value " + ChatColor.GREEN + "$"+playerValueCARP);
    				player.sendMessage(ChatColor.BLUE + "Miners (MINE): " + ChatColor.AQUA + sharesMINE + " shares, total value " + ChatColor.GREEN + "$"+playerValueMINE);
    				player.sendMessage(ChatColor.BLUE + "Masons (MASN): " + ChatColor.AQUA + sharesMASN + " shares, total value " + ChatColor.GREEN + "$"+playerValueMASN);
    				player.sendMessage(ChatColor.BLUE + "Diggers (DIGR): " + ChatColor.AQUA + sharesDIGR + " shares, total value " + ChatColor.GREEN + "$"+playerValueDIGR);
    				player.sendMessage(ChatColor.DARK_PURPLE +"------------------------");
    			
    			} else if (args.length == 2) { // if a player is specified
    				if (entry_exists("shareholders","playername",args[1]) == 0) {
    					player.sendMessage(ChatColor.DARK_PURPLE + "[BlockMarket] " + ChatColor.RED + "That player doesn't own any stocks! (case sensitive)");
    					return false;
    				}
    				int sharesLMBR = get_int("shareholders","lumberjacks","playername",args[1]);
    				int sharesCARP = get_int("shareholders","carpenters","playername",args[1]);
    				int sharesMINE = get_int("shareholders","miners","playername",args[1]);
    				int sharesMASN = get_int("shareholders","masons","playername",args[1]);
    				int sharesDIGR = get_int("shareholders","diggers","playername",args[1]);
    				
    				int playerValueLMBR = sharesLMBR*(get_int("companies","value","name","lumberjacks"))/(get_int("companies","share_amt","name","lumberjacks"));
    				int playerValueCARP = sharesCARP*(get_int("companies","value","name","carpenters"))/(get_int("companies","share_amt","name","carpenters"));
    				int playerValueMINE = sharesMINE*(get_int("companies","value","name","miners"))/(get_int("companies","share_amt","name","miners"));
    				int playerValueMASN = sharesMASN*(get_int("companies","value","name","masons"))/(get_int("companies","share_amt","name","masons"));
    				int playerValueDIGR = sharesDIGR*(get_int("companies","value","name","diggers"))/(get_int("companies","share_amt","name","diggers"));

    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + args[1] + "'s Stock Portfolio" + ChatColor.DARK_PURPLE +"----");
    				player.sendMessage(ChatColor.BLUE + "Lumberjacks (LMBR): " + ChatColor.AQUA + sharesLMBR + " shares, total value " + ChatColor.GREEN + "$"+playerValueLMBR);
    				player.sendMessage(ChatColor.BLUE + "Carpenters (CARP): " + ChatColor.AQUA + sharesCARP + " shares, total value " + ChatColor.GREEN + "$"+playerValueCARP);
    				player.sendMessage(ChatColor.BLUE + "Miners (MINE): " + ChatColor.AQUA + sharesMINE + " shares, total value " + ChatColor.GREEN + "$"+playerValueMINE);
    				player.sendMessage(ChatColor.BLUE + "Masons (MASN): " + ChatColor.AQUA + sharesMASN + " shares, total value " + ChatColor.GREEN + "$"+playerValueMASN);
    				player.sendMessage(ChatColor.BLUE + "Diggers (DIGR): " + ChatColor.AQUA + sharesDIGR + " shares, total value " + ChatColor.GREEN + "$"+playerValueDIGR);
    				player.sendMessage(ChatColor.DARK_PURPLE +"------------------------");
    			}
    		} // end /bm portfolio

			//-------- /bm list
    		if (args.length > 0 && args[0].equalsIgnoreCase("list")){
    			//---- /bm list companies
    			if (args.length == 2 && args[1].equalsIgnoreCase("companies")) {
    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Companies" + ChatColor.DARK_PURPLE +"----");
    				player.sendMessage(ChatColor.BLUE + "Lumberjacks (LMBR)");
    				player.sendMessage(ChatColor.BLUE + "Carpenters (CARP)");
    				player.sendMessage(ChatColor.BLUE + "Miners (MINE)");
    				player.sendMessage(ChatColor.BLUE + "Masons (MASN)");
    				player.sendMessage(ChatColor.BLUE + "Diggers (DIGR)");
    				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------");
    				
    			}
    			//---- /bm list shareholders [company]
    			else if (args.length == 3 && (args[1].equalsIgnoreCase("shareholders") || args[1].equalsIgnoreCase("sh"))) {
        			String companyName = args[2].toLowerCase();
    				companyName = company_name_exists(player, companyName);
    				if (companyName == "error") { return false; } // Error message already handled in method

    				String[] shareholders = new String[15];
    				int[] shares = new int[15];
    				
    				ResultSet rs = null;

    				// Get players with highest # of shares from that company
    				try {
						rs = mysql.query("SELECT * FROM shareholders ORDER BY "+companyName+" DESC LIMIT 15");
					} catch (SQLException e) {
						player.sendMessage(ChatColor.DARK_PURPLE + "[BlockMarket] " + ChatColor.RED + "Error: There are no shareholders in that company, or it doesn't exist."); 
						e.printStackTrace();
						return false;
					}
					int a = 0;
					
    				try {
						if (rs.first()) {
							    do {
							    	shareholders[a] = rs.getString("playername");
							    	shares[a] = rs.getInt(companyName);
							        a++;
							    } while(rs.next());
						}
					} catch (SQLException e) {
							player.sendMessage(ChatColor.DARK_PURPLE + "[BlockMarket] " + ChatColor.RED + "Error: There are no shareholders in that company."); 
							e.printStackTrace();
							return false;
					}

    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Shareholders in "+companyName + ChatColor.DARK_PURPLE +"----");
    				for (int i=0; i<a; i++) {
    					player.sendMessage(ChatColor.BLUE + shareholders[i] + ": " + String.valueOf(shares[i]) + " shares");
    					
    				}
    				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------------------");

    				
    			} else {
    				player.sendMessage(ChatColor.DARK_PURPLE + "[BlockMarket]" + ChatColor.RED + " Did you mean:");
    				player.sendMessage(ChatColor.GRAY + "/bm list shareholders [company name]");
    				player.sendMessage(ChatColor.GRAY + "/bm list companies");
    			}
			} // end /bm list

			//-------- /bm give [playername] [company] [amount]
    		if (args.length == 4 && args[0].equalsIgnoreCase("give")){
    			String companyName = args[2].toLowerCase();
    			int amount = Integer.parseInt(args[3]);

    			// Preliminary error check before SQL happens
    			companyName = company_name_exists(player,companyName);
    			if (companyName == "error") { return false; } // Giving error message is already handled in method. Exits onCommand

    			int sql_playershares = get_int("shareholders",companyName,"playername",playername);
    			int newshares = amount;
    			int newshares2 = sql_playershares - amount;
    			// Error checking
    			if (sql_playershares < amount) {
    				player.sendMessage(ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "You only have " +sql_playershares+" shares to transfer.");
    				error = true;
    			}
    			if (!Bukkit.getServer().getPlayer(args[1]).isOnline()) {
    				player.sendMessage(ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.RED + "The recipient is not online. Remember it's case sensitive!");
    				error = true;
    			}
    			
				if (entry_exists("shareholders","playername",args[1]) == 1) {
					int sql_currentshares = get_int("shareholders",companyName,"playername",args[1]);
					newshares = sql_currentshares + amount;
				}

    			// If no errors
    			if (error == false) {
    				// Create player entry with default values if doesn't exist
    				if (entry_exists("shareholders","playername",args[1]) == 0) {
    					try {
							mysql.query("INSERT INTO shareholders VALUES ('"+args[1]+"',0,0,0,0,0);");
						} catch (SQLException e) {
							e.printStackTrace();
						}
    				}
    				// Add shares bought to player
					try {
						mysql.query("UPDATE shareholders SET "+companyName+"="+newshares+" WHERE playername='"+args[1]+"';");
						mysql.query("UPDATE shareholders SET "+companyName+"="+newshares2+" WHERE playername='"+playername+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.GREEN + "Successfully transferred "+amount+" shares to "+args[1]+".");
    			}
			} // end /bm give [playername] [company] [amount]

    		  // /bm connect
    		if (args.length == 1 && (args[0].equalsIgnoreCase("connect") || args[0].equalsIgnoreCase("open"))){
    			if (!mysql.isOpen()) {
    				sqlConnection();
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.GREEN + "Successfully opened connection to MySQL.");
    			} else {
    				mysql.close();
    				sqlConnection();
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket] " + ChatColor.GREEN + "Successfully reopened connection to MySQL.");
    			}
    		} // end /bm connect
    		
    		return true;
    	} // end /bm

    	return false; 
    } // end onCommand()
    
    
    public void sqlConnection() {
	    log.info("Connecting to MySQL...");
		String host = getConfig().getString("mysqlinfo.host");
		String port = getConfig().getString("mysqlinfo.port");
		String dbname = getConfig().getString("mysqlinfo.database");
		String user = getConfig().getString("mysqlinfo.username");
		String password = getConfig().getString("mysqlinfo.password");
		
		mysql = new MySQL(this.getLogger(),"[BlockMarket] ",host,Integer.parseInt(port),dbname,user,password);
		try {
			mysql.open();
		} catch (Exception e) {
			log.info(e.getMessage());
		}

		if (mysql.isOpen()) { log.info("Successfully connected to MySQL!"); }
		else { log.info("Could not connect to MySQL:"); }

	}

	public void sqlDoesDatabaseExist() {
        if(!(mysql.isTable("companies"))){
    	    log.info("Creating MySQL table 'companies'...");
        	try {
    	    	mysql.query("CREATE TABLE rewardtable (playername VARCHAR(50),  rewardtime INT(10), IP VARCHAR(20), rewarded VARCHAR(5), one VARCHAR(5), two VARCHAR(5), three VARCHAR(5));");
        	    log.info("Successfully created MySQL table!");
        	} catch (Exception e) {
        		log.info("Could not create MySQL table!");
    			log.info(e.getMessage());
        	}
    	    	
        }
        if(!(mysql.isTable("shareholders"))){
    	    log.info("Creating MySQL table 'shareholders'...");
        	try {
    	    	mysql.query("CREATE TABLE rewardtable (playername VARCHAR(50),  rewardtime INT(10), IP VARCHAR(20), rewarded VARCHAR(5), one VARCHAR(5), two VARCHAR(5), three VARCHAR(5));");
        	    log.info("Successfully created MySQL table!");
        	} catch (Exception e) {
        		log.info("Could not create MySQL table:");
    			log.info(e.getMessage());
        	}
    	    	
        }
    }
	
				// ============================//
				//			 Methods	 	   //
				// ============================//
	public void setupChangeTime(Plugin plugin) {
		Calendar cal = Calendar.getInstance();
		long now = cal.getTimeInMillis();
		
		log.info("HOUR_OF_DAY " + cal.get(Calendar.HOUR_OF_DAY));
		log.info("MINUTE " + cal.get(Calendar.MINUTE));
		
		if(cal.get(Calendar.HOUR_OF_DAY) >= 1)
		    cal.add(Calendar.DATE, 1);  // do it tomorrow if now is after 1 AM
		cal.set(Calendar.HOUR_OF_DAY, 1); // 1 AM
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		long offset = cal.getTimeInMillis() - now;
		long ticks = offset / 50;  //there are 50 milliseconds in a tick

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,new Runnable()
	    {
	        public void run()
	        {
	        	if (!mysql.isOpen()) { sqlConnection(); }
	        	// LMBR
	        	int sql_current = get_int("companies","value","name","lumberjacks");
	        	int sql_queue = get_int("companies","queue_val","name","lumberjacks");
	        	int sql_share_amt = get_int("companies","share_amt","name","lumberjacks");
	        	double keepPercent = 1 - getConfig().getDouble("lumberjacks.bufferpercent");
	        	int change = 0;
	        	double lastchange = 0;
	        	int finalVal = sql_current;

        		change = sql_queue - sql_current;
        		change = (int)Math.round(change * keepPercent);
        		lastchange = round((change/sql_share_amt),2);
        		finalVal = finalVal + change;
        		try {
					mysql.query("UPDATE companies SET value="+finalVal+",lastchange="+lastchange+" WHERE name='lumberjacks';");
				} catch (SQLException e) {
					e.printStackTrace();
				}

	        	// CARP
	        	sql_current = get_int("companies","value","name","carpenters");
	        	sql_queue = get_int("companies","queue_val","name","carpenters");
	        	sql_share_amt = get_int("companies","share_amt","name","carpenters");
	        	keepPercent = 1 - getConfig().getDouble("carpenters.bufferpercent");
	        	change = 0;
	        	lastchange = 0;
	        	finalVal = sql_current;

	        	change = sql_queue - sql_current;
        		change = (int)Math.round(change * keepPercent);
        		lastchange = round((change/sql_share_amt),2);
        		finalVal = finalVal + change;
        		try {
					mysql.query("UPDATE companies SET value="+finalVal+",lastchange="+lastchange+",queue_val='0' WHERE name='carpenters';");
				} catch (SQLException e) {
					e.printStackTrace();
				}

	        	// MINE
	        	sql_current = get_int("companies","value","name","miners");
	        	sql_queue = get_int("companies","queue_val","name","miners");
	        	sql_share_amt = get_int("companies","share_amt","name","miners");
	        	keepPercent = 1 - getConfig().getDouble("miners.bufferpercent");
	        	change = 0;
	        	lastchange = 0;
	        	finalVal = sql_current;

	        	change = sql_queue - sql_current;
        		change = (int)Math.round(change * keepPercent);
        		lastchange = round((change/sql_share_amt),2);
        		finalVal = finalVal + change;
        		try {
					mysql.query("UPDATE companies SET value="+finalVal+",lastchange="+lastchange+",queue_val='0' WHERE name='miners';");
				} catch (SQLException e) {
					e.printStackTrace();
				}
        		
	        	// MASN
	        	sql_current = get_int("companies","value","name","masons");
	        	sql_queue = get_int("companies","queue_val","name","masons");
	        	sql_share_amt = get_int("companies","share_amt","name","masons");
	        	keepPercent = 1 - getConfig().getDouble("masons.bufferpercent");
	        	change = 0;
	        	lastchange = 0;
	        	finalVal = sql_current;

	        	change = sql_queue - sql_current;
        		change = (int)Math.round(change * keepPercent);
        		lastchange = round((change/sql_share_amt),2);
        		finalVal = finalVal + change;
        		try {
					mysql.query("UPDATE companies SET value="+finalVal+",lastchange="+lastchange+",queue_val='0' WHERE name='masons';");
				} catch (SQLException e) {
					e.printStackTrace();
				}

	        	// DIGR
	        	sql_current = get_int("companies","value","name","diggers");
	        	sql_queue = get_int("companies","queue_val","name","diggers");
	        	sql_share_amt = get_int("companies","share_amt","name","diggers");
	        	keepPercent = 1 - getConfig().getDouble("diggers.bufferpercent");
	        	change = 0;
	        	lastchange = 0;
	        	finalVal = sql_current;

	        	change = sql_queue - sql_current;
        		change = (int)Math.round(change * keepPercent);
        		lastchange = round((change/sql_share_amt),2);
        		finalVal = finalVal + change;
        		try {
					mysql.query("UPDATE companies SET value="+finalVal+",lastchange="+lastchange+",queue_val='0' WHERE name='diggers';");
				} catch (SQLException e) {
					e.printStackTrace();
				}
	        }
	    }, ticks);
	}
	
	public String get_str(String table, String varSelect, String varWhere, String valWhere) {
		// Get string from MySQL
		ResultSet rs;
		try {
			rs = mysql.query("Select "+varSelect+" FROM "+table+" WHERE "+varWhere+"='"+valWhere+"';");
		} catch (SQLException e) {
            rs = null;
        	e.printStackTrace();
        }
		
		// Return string
        try {
            if (rs.first()) {
                try {
                    return rs.getString(varSelect);
                } catch (SQLException e) {
                    rs = null;
                	e.printStackTrace();
                }
            }
        } catch (SQLException e) {
       
            e.printStackTrace();
        }
        
        // If failed
        return null;
	}
	public int get_int(String table, String varSelect, String varWhere, String valWhere) {
		// Get string from MySQL
		ResultSet rs;
		try {
			rs = mysql.query("Select "+varSelect+" FROM "+table+" WHERE "+varWhere+"='"+valWhere+"';");
		} catch (SQLException e) {
            rs = null;
        	e.printStackTrace();
        }
		
		// Return string
        try {
            if (rs.first()) {
                try {
                    return rs.getInt(varSelect);
                } catch (SQLException e) {
                    rs = null;
                	e.printStackTrace();
                }
            }
        } catch (SQLException e) {
       
            e.printStackTrace();
        }
        
        // If failed
        return 0;
	}
	public double get_double(String table, String varSelect, String varWhere, String valWhere) {
		// Get string from MySQL
		ResultSet rs;
		try {
			rs = mysql.query("Select "+varSelect+" FROM "+table+" WHERE "+varWhere+"='"+valWhere+"';");
		} catch (SQLException e) {
            rs = null;
        	e.printStackTrace();
        }
		
		// Return string
        try {
            if (rs.first()) {
                try {
                    return rs.getDouble(varSelect);
                } catch (SQLException e) {
                    rs = null;
                	e.printStackTrace();
                }
            }
        } catch (SQLException e) {
       
            e.printStackTrace();
        }
        
        // If failed
        return 0;
	}
	public Date get_date(String table, String varSelect, String varWhere, String valWhere) {
		// Get string from MySQL
		ResultSet rs;
		try {
			rs = mysql.query("Select "+varSelect+" FROM "+table+" WHERE "+varWhere+"='"+valWhere+"';");
		} catch (SQLException e) {
            rs = null;
        	e.printStackTrace();
        }
		
		// Return string
        try {
            if (rs.first()) {
                try {
                    return rs.getDate(varSelect);
                } catch (SQLException e) {
                    rs = null;
                	e.printStackTrace();
                }
            }
        } catch (SQLException e) {
       
            e.printStackTrace();
        }
        
        // If failed
        return null;
	}
	public int entry_exists(String table, String varWhere, String valWhere) {
		// Get string from MySQL
		ResultSet rs;
		try {//SELECT EXISTS(SELECT 1 FROM table1 WHERE ...)
			rs = mysql.query("Select EXISTS(SELECT 1 FROM "+table+" WHERE "+varWhere+"='"+valWhere+"');");
		} catch (SQLException e) {
            rs = null;
        	e.printStackTrace();
        }
		
		// Return string
        try {
            if (rs.first()) {
                try {
                    return rs.getInt("EXISTS(SELECT 1 FROM "+table+" WHERE "+varWhere+"='"+valWhere+"')");
                } catch (SQLException e) {
                    rs = null;
                	e.printStackTrace();
                }
            }
        } catch (SQLException e) {
       
            e.printStackTrace();
        }
        
        // If failed
        return 0;
	}
	
	public String company_name_exists(Player player, String companyName) {
		if ((!companyName.equalsIgnoreCase("lumberjacks")) && 
			(!companyName.equalsIgnoreCase("carpenters")) &&
			(!companyName.equalsIgnoreCase("miners")) &&
			(!companyName.equalsIgnoreCase("masons")) &&
			(!companyName.equalsIgnoreCase("diggers"))) { // Not a valid company name
			if (!validSymbols.toLowerCase().contains(companyName)){ // Not a valid symbol name
				player.sendMessage(ChatColor.DARK_PURPLE + "[BlockMarket] " + ChatColor.RED + "Error: That company doesn't exist.");
				return "error";
			} else { // is a valid symbol name
				// Convert symbol to the actual company name
				switch (companyName) {
				case "lmbr":
					companyName = "lumberjacks";
					break;
				case "carp":
					companyName = "carpenters";
					break;
				case "mine":
					companyName = "miners";
					break;
				case "masn":
					companyName = "masons";
					break;
				case "digr":
					companyName = "diggers";
					break;
				}
				return companyName;
			}
		} else { // is a valid company name
			return companyName;
		}
	}
	// === ROUND DOUBLES ===
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
	    return bd.doubleValue();
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
		    return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
		    return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	 }
	
}
