package io.github.egg101.BlockMarket;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.MySQL;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockMarket extends JavaPlugin{
	Logger log;
	private Database mysql;
    public static Economy econ = null;
    String validCompanies = "lumberjacks carpenters miners masons diggers";
    String validSymbols = "LMBR CARP MINE MASN DIGR";
    @Override
    public void onEnable(){
		log = this.getLogger();
		log.info("[BlockMarket] Enabled");
    	
		// Set up config
	    File file = new File(this.getDataFolder(), "config.yml");
	    if (!file.exists()) {
			log.info("[BlockMarket] Creating config.yml...");
	        this.saveDefaultConfig();
			log.info("[BlockMarket] Successfully created config.yml!");
	    }
	    
	    // Set up SQL
        sqlConnection();
        
        // Set up economy through Vault
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
    }
    @Override
    public void onDisable() {
    	
    }
    
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		Player player = (Player) sender;
		String playername = player.getName();
		boolean error = false;
		
    	if(cmd.getName().equalsIgnoreCase("bm")){
			//-------- /bm buy [company] [amount]
    		if (args.length == 3 && args[0].equalsIgnoreCase("buy")){
    			String companyName = args[1].toLowerCase();
    			int amount = Integer.parseInt(args[2]);
    			
    			// Preliminary error check before SQL happens
    			companyName = company_name_exists(player,companyName);
    			if (companyName == "error") { return false; } // Giving error message is already handled in method. Exits onCommand
    			
    			int sql_shares_unsold = get_int("companies","shares_unsold","name",companyName);
    			int sql_value = get_int("companies","value","name",companyName);
    			int sql_share_amt = get_int("companies","share_amt","name",companyName);
    			int sql_playershares = get_int("shareholders",companyName,"playername",playername);
    			
    			int shares_left = sql_shares_unsold - amount;
    			double cost = amount * (sql_value / sql_share_amt);
    			cost = round(cost, 2);
    			int newshareamount = sql_playershares + amount;
    			
    			// Error checking
    			
    			if (sql_shares_unsold == 0) {
    				player.sendMessage("[BlockMarket] All shares in that company are sold out.");
    				error = true;
    			}
    			if (amount == 0) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.RED + "You can't buy 0 shares.");
    				error = true;
    			}
    			if (sql_shares_unsold >= amount) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.RED + "You can't buy that many shares. There are only " + sql_shares_unsold + " shares for sale in that company.");
    				error = true;
    			}
    			
    			// If no errors:
    			if (error == false) {
    				// Remove unsold shares from company
    				try {
						mysql.query("UPDATE companies SET shares_unsold="+shares_left+" WHERE name='"+companyName+"';");
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
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.GREEN + "Successfully bought "+amount+" shares for $"+cost+".");
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
    			int sql_value = get_int("companies","value","name",companyName);
    			int sql_share_amt = get_int("companies","share_amt","name",companyName);
    			int sql_playershares = get_int("shareholders",companyName,"playername",playername);
    			int sql_returntax = get_int("companies","returntax","name",companyName);
    			
    			int shares_updated = sql_playershares + sql_shares_unsold; // New share amt for company
    			int newshareamount = sql_playershares - amount; // New share amt for player
    			
    			double payment = amount * (sql_value / sql_share_amt) * sql_returntax;
    			payment = round(payment, 2);
    			
    			// Error checking
    			if (amount == 0) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.RED + "You can't sell 0 shares.");
    				error = true;
    			}
    			if (sql_playershares < amount) {
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.RED + "You only have " +sql_playershares+" shares to sell.");
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
						mysql.query("UPDATE companies SET shares_unsold="+shares_updated+" WHERE name='"+companyName+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
    				
    				// Gives money to player
					econ.depositPlayer(playername, payment);
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.GREEN + "Successfully sold "+amount+" shares for $"+payment+". (Return tax was "+sql_returntax*100+"%)");
    				
    			}
			}

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
    			int sql_returntax = 	get_int("companies","returntax","name",companyName);
    			
    			String lastchangePrefix = "";
    			if (sql_lastchange > 0) { lastchangePrefix = "+"; }
    			else if (sql_lastchange < 0) { lastchangePrefix = "-"; }

				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Info for "+showCompanyName+ ChatColor.DARK_PURPLE +"----");
				player.sendMessage(ChatColor.DARK_BLUE + "Ticker: $" + ChatColor.AQUA + sql_symbol + ChatColor.DARK_RED + " " + lastchangePrefix + sql_lastchange);
				player.sendMessage(ChatColor.DARK_BLUE + "Company value: $" + ChatColor.AQUA + sql_value);
				player.sendMessage(ChatColor.DARK_BLUE + "Total shares in company: " + ChatColor.AQUA + sql_share_amt);
				player.sendMessage(ChatColor.DARK_BLUE + "Share price: $" + ChatColor.AQUA + price);
				player.sendMessage(ChatColor.DARK_BLUE + "Shares for sale: " + ChatColor.AQUA + sql_shares_unsold);
				player.sendMessage(ChatColor.DARK_BLUE + "Return tax: " + ChatColor.AQUA + sql_returntax + "%");
				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------");
    			
			}
    		
			//-------- /bm ticker
    		if (args.length == 1 && args[0].equalsIgnoreCase("ticker")){
    			String[] companyNames = {"lumberjacks", "carpenters", "miners", "masons", "diggers"};
    			String[] tickerSymbols = new String[5];
    			int[] lastChanges = new int[5];
    			
    			for (int i = 0; i<5; i++) {
    				tickerSymbols[i] = get_str("companies","symbol","name",companyNames[i]);
        			lastChanges[i] =	get_int("companies","lastchange","name",companyNames[i]);
    			}
    			

    			String combinedMessage = "";
    			for (int i = 0; i<5; i++) {
    				combinedMessage.concat("�7"+companyNames[i]);
    				
    				if (lastChanges[i] > 0) {
    					combinedMessage.concat("�a +");
    				} else if (lastChanges[i] < 0) {
    					combinedMessage.concat("�c -");
    				} else {
    					combinedMessage.concat("�f");
    				}
    				
    				combinedMessage.concat(lastChanges[i] + "   ");
    			}
    			
			}
			//-------- /bm portfolio (username)
    		if (args[0].equalsIgnoreCase("portfolio")){
    			if (args.length == 1) { // if no player specified
    				int sharesLMBR = get_int("shareholders","lumberjacks","playername",playername);
    				int sharesCARP = get_int("shareholders","carpenters","playername",playername);
    				int sharesMINE = get_int("shareholders","miners","playername",playername);
    				int sharesMASN = get_int("shareholders","masons","playername",playername);
    				int sharesDIGR = get_int("shareholders","diggers","playername",playername);
    				
    				int playerValueLMBR = sharesLMBR*(get_int("companies","value","name","lumberjacks"))/(get_int("companies","share_amt","name","lumberjacks"));
    				int playerValueCARP = sharesCARP*(get_int("companies","value","name","carpenters"))/(get_int("companies","share_amt","name","carpenters"));
    				int playerValueMINE = sharesLMBR*(get_int("companies","value","name","miners"))/(get_int("companies","share_amt","name","miners"));
    				int playerValueMASN = sharesLMBR*(get_int("companies","value","name","masons"))/(get_int("companies","share_amt","name","masons"));
    				int playerValueDIGR = sharesLMBR*(get_int("companies","value","name","diggers"))/(get_int("companies","share_amt","name","diggers"));
    				
    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Your Stock Portfolio" + ChatColor.DARK_PURPLE +"----");
    				player.sendMessage(ChatColor.DARK_BLUE + "Lumberjacks (LMBR): " + ChatColor.AQUA + sharesLMBR + " shares, total value $"+playerValueLMBR);
    				player.sendMessage(ChatColor.DARK_BLUE + "Carpenters (CARP): " + ChatColor.AQUA + sharesCARP + " shares, total value $"+playerValueCARP);
    				player.sendMessage(ChatColor.DARK_BLUE + "Miners (MINE): " + ChatColor.AQUA + sharesMINE + " shares, total value $"+playerValueMINE);
    				player.sendMessage(ChatColor.DARK_BLUE + "Masons (MASN): " + ChatColor.AQUA + sharesMASN + " shares, total value $"+playerValueMASN);
    				player.sendMessage(ChatColor.DARK_BLUE + "Diggers (DIGR): " + ChatColor.AQUA + sharesDIGR + " shares, total value $"+playerValueDIGR);
    				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------");
    			}
    		}

			//-------- /bm list
    		if (args.length > 0 && args[0].equalsIgnoreCase("list")){
    			//---- /bm list companies
    			if (args.length == 1 && args[1].equalsIgnoreCase("companies")) {
    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Companies" + ChatColor.DARK_PURPLE +"----");
    				player.sendMessage(ChatColor.BLUE + "Lumberjacks (LMBR)");
    				player.sendMessage(ChatColor.BLUE + "Carpenters (CARP)");
    				player.sendMessage(ChatColor.BLUE + "Miners (MINE)");
    				player.sendMessage(ChatColor.BLUE + "Masons (MASN)");
    				player.sendMessage(ChatColor.BLUE + "Diggers (DIGR)");
    				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------");
    				
    			}
    			//---- /bm list shareholders [company]
    			if (args.length == 2 && args[1].equalsIgnoreCase("shareholders")) {
        			String companyName = args[1].toLowerCase();
    				companyName = company_name_exists(player, companyName);
    				if (companyName == "error") { return false; } // Error message already handled in method

    				String[] shareholders = new String[15];
    				int[] shares = new int[15];
    				
    				ResultSet rs = null;
    				
    				// Get players with highest # of shares from that company
    				try {
						rs = mysql.query("SELECT * FROM shareholders ORDER BY "+companyName+" DESC LIMIT 15");
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    				try {
						if (rs.first()) {
							 int i = 0;
							    do {
							    	shareholders[i] = rs.getString("playername");
							    	shares[i] = rs.getInt(companyName);
							        i++;
							    } while(rs.next());
						}
					} catch (SQLException e) {
							e.printStackTrace();
					}

    				player.sendMessage(ChatColor.DARK_PURPLE +"----" + ChatColor.LIGHT_PURPLE + "Shareholders in "+companyName + ChatColor.DARK_PURPLE +"----");
    				for (int i=0; i<15; i++) {
    					player.sendMessage(ChatColor.BLUE + shareholders[i] + ": " + shares[i] + " shares");
    					
    				}
    				player.sendMessage(ChatColor.DARK_PURPLE +"-----------------");
    				
    			}
			}

			//-------- /bm give [playername] [company] [amount]
    		if (args.length == 4 && args[0].equalsIgnoreCase("give")){
    			String companyName = args[2].toLowerCase();
    			int amount = Integer.parseInt(args[2]);
    			
    			// Preliminary error check before SQL happens
    			companyName = company_name_exists(player,companyName);
    			if (companyName == "error") { return false; } // Giving error message is already handled in method. Exits onCommand

    			int sql_playershares = get_int("shareholders",companyName,"playername",playername);

    			int newshares = amount;
				if (entry_exists("shareholders","playername",args[1]) == 1) {
					int sql_currentshares = get_int("shareholders",companyName,"playername",args[1]);
					newshares = sql_currentshares + amount;
				}
    			// Error checking
    			if (sql_playershares < amount) {
    				player.sendMessage(ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.RED + "You only have " +sql_playershares+" shares to transfer.");
    				error = true;
    			}
    			if (!Bukkit.getServer().getPlayer(args[2]).isOnline()) {
    				player.sendMessage(ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.RED + "The recipient is not online. Remember it's case sensitive!");
    				error = true;
    			}
    			
    			// If no errors
    			if (error == false) {
    				// Create player entry with default values if doesn't exist
    				if (entry_exists("shareholders","playername",args[1]) == 0) {
    					try {
							mysql.query("INSERT INTO shareholders VALUES ('"+playername+"',0,0,0,0,0);");
						} catch (SQLException e) {
							e.printStackTrace();
						}
    				}
    				// Add shares bought to player
					try {
						mysql.query("UPDATE shareholders SET "+companyName+"="+newshares+" WHERE playername='"+args[1]+"';");
					} catch (SQLException e) {
						e.printStackTrace();
					}
    				player.sendMessage (ChatColor.DARK_PURPLE +"[BlockMarket]" + ChatColor.GREEN + "Successfully transferred "+amount+" shares to "+args[2]+".");
    			}
			} // end /bm give [playername] [company] [amount]
    		
    		return true;
    	} // end /bm

    	return false; 
    }
    
    
    public void sqlConnection() {
	    log.info("[BlockMarket] Connecting to MySQL...");
		String host = getConfig().getString("mysqlinfo.host");
		String port = getConfig().getString("mysqlinfo.port");
		String dbname = getConfig().getString("mysqlinfo.database");
		String user = getConfig().getString("mysqlinfo.username");
		String password = getConfig().getString("mysqlinfo.password");
		
		mysql = new MySQL(this.getLogger(),"[BlockMarket] ",host,Integer.parseInt(port),dbname,user,password);
		try {
			mysql.open();
		    log.info("[BlockMarket] Successfully connected to MySQL!");
		} catch (Exception e) {
			log.info("[BlockMarket] Could not connect to MySQL:");
			log.info(e.getMessage());
		}

	}

	public void sqlDoesDatabaseExist() {
        if(!(mysql.isTable("companies"))){
    	    log.info("[BlockMarket] Creating MySQL table 'companies'...");
        	try {
    	    	mysql.query("CREATE TABLE rewardtable (playername VARCHAR(50),  rewardtime INT(10), IP VARCHAR(20), rewarded VARCHAR(5), one VARCHAR(5), two VARCHAR(5), three VARCHAR(5));");
        	    log.info("[BlockMarket] Successfully created MySQL table!");
        	} catch (Exception e) {
        		log.info("[BlockMarket] Could not create MySQL table:");
    			log.info(e.getMessage());
        	}
    	    	
        }
        if(!(mysql.isTable("shareholders"))){
    	    log.info("[BlockMarket] Creating MySQL table 'shareholders'...");
        	try {
    	    	mysql.query("CREATE TABLE rewardtable (playername VARCHAR(50),  rewardtime INT(10), IP VARCHAR(20), rewarded VARCHAR(5), one VARCHAR(5), two VARCHAR(5), three VARCHAR(5));");
        	    log.info("[BlockMarket] Successfully created MySQL table!");
        	} catch (Exception e) {
        		log.info("[BlockMarket] Could not create MySQL table:");
    			log.info(e.getMessage());
        	}
    	    	
        }
    }
	
				// ============================//
				//			 Methods	 	   //
				// ============================//
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
		if (!validCompanies.contains(companyName)) { // Not a valid company name
			if (!validSymbols.toLowerCase().contains(companyName)){ // Not a valid symbol name
				player.sendMessage("[BlockMarket] That company doesn't exist.");
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
				case "digr":
					companyName = "diggers";
					break;
				}
				return companyName;
			}
		}
		return "error";
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
