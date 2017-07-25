package com.bloomberg.emsx.samples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.bloomberg.emsx.samples.Log.LogLevels;
import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;

public class EasyMSX implements EventHandler, NotificationHandler {

	// EVENTS
    private static final Name 	ORDER_ROUTE_FIELDS = new Name("OrderRouteFields");

	// ADMIN
	private static final Name 	SLOW_CONSUMER_WARNING	= new Name("SlowConsumerWarning");
	private static final Name 	SLOW_CONSUMER_WARNING_CLEARED	= new Name("SlowConsumerWarningCleared");

	// SESSION_STATUS
	private static final Name 	SESSION_STARTED 		= new Name("SessionStarted");
	private static final Name 	SESSION_TERMINATED 		= new Name("SessionTerminated");
	private static final Name 	SESSION_STARTUP_FAILURE = new Name("SessionStartupFailure");
	private static final Name 	SESSION_CONNECTION_UP 	= new Name("SessionConnectionUp");
	private static final Name 	SESSION_CONNECTION_DOWN	= new Name("SessionConnectionDown");

	// SERVICE_STATUS
	private static final Name 	SERVICE_OPENED 			= new Name("ServiceOpened");
	private static final Name 	SERVICE_OPEN_FAILURE 	= new Name("ServiceOpenFailure");

	// SUBSCRIPTION_STATUS + SUBSCRIPTION_DATA
	private static final Name	SUBSCRIPTION_FAILURE 	= new Name("SubscriptionFailure");
	private static final Name	SUBSCRIPTION_STARTED	= new Name("SubscriptionStarted");
	private static final Name	SUBSCRIPTION_TERMINATED	= new Name("SubscriptionTerminated");

	private static final Name AUTHORIZATION_SUCCESS = new Name("AuthorizationSuccess");

	private static final String DEFAULT_HOST = "localhost";
	private static final int DEFAULT_PORT = 8194;
	
	public enum Environment {
		PRODUCTION,
		BETA;
	}

	private Environment environment;
	private String host;
	private String user;
	private int port;
	private String userIP;
	
	private boolean serverConnection=false;
	
	public Orders orders;
	public Routes routes;
	public Teams teams;
	public Brokers brokers;

	private Session session;
	private SessionOptions sessionOptions;
	
	String emsxServiceName;
	String authServiceName = "//blp/apiauth";
	String brokerSpecServiceName;
	Service emsxService;
	Service brokerSpecService;
	
	ArrayList<SchemaFieldDefinition> orderFields = new ArrayList<SchemaFieldDefinition>();
	ArrayList<SchemaFieldDefinition> routeFields = new ArrayList<SchemaFieldDefinition>();

	ConcurrentHashMap<CorrelationID, MessageHandler> requestMessageHandlers = new ConcurrentHashMap<CorrelationID, MessageHandler>();
	ConcurrentHashMap<CorrelationID, MessageHandler> subscriptionMessageHandlers = new ConcurrentHashMap<CorrelationID, MessageHandler>();
	
	Team team;
	String selectedTeam="";
	
	Identity userIdentity;
	CorrelationID authRequestID;
	
	NotificationHandler globalNotificationHandler = null;

	boolean orderBlotterInitialized = false;
	boolean routeBlotterInitialized = false;
	boolean authorized = false;
	boolean authorizationFailed = false;

	public EasyMSX() throws Exception {
		
		this.environment = Environment.BETA;
		this.host = DEFAULT_HOST;
		this.port = DEFAULT_PORT;
		this.authorized = true;
		
		initialize();
	}
	
	public EasyMSX(String team) throws Exception {
		
		this.environment = Environment.BETA;
		this.host = DEFAULT_HOST;
		this.port = DEFAULT_PORT;
		this.selectedTeam = team;
		this.authorized = true;
		
		initialize();
	}

	public EasyMSX(Environment env) throws Exception {
		this.environment = env;
		this.host = DEFAULT_HOST;
		this.port = DEFAULT_PORT;
		this.authorized = true;
		
		initialize();
	}

	public EasyMSX(Environment env, String team) throws Exception {
		this.environment = env;
		this.host = DEFAULT_HOST;
		this.port = DEFAULT_PORT;
		this.selectedTeam = team;
		this.authorized = true;
		
		initialize();
	}

	public EasyMSX(Environment env, String host, int port, String user, String userIP) throws Exception {
		this.environment = env;
		this.host = host;
		this.port = port;
		this.user = user;
		this.userIP = userIP;
		this.serverConnection = true;
		
		initialize();
	}
	
	public EasyMSX(Environment env, String team, String host, int port, String user, String userIP) throws Exception {
		this.environment = env;
		this.host = host;
		this.port = port;
		this.user = user;
		this.userIP = userIP;
		this.selectedTeam = team;
		this.serverConnection = true;

		initialize();
	}

	
	private void initialize() throws SessionException, ServiceException, IOException, InterruptedException {
		
		initializeSession();
		initializeService();
		initializeFieldData();
		initializeTeams();
		initializeBrokerData();
		
        Log.LogMessage(LogLevels.BASIC, "Message Handlers count: " + requestMessageHandlers.size());

        while(!requestMessageHandlers.isEmpty())
        {
        	 // wait until all outstanding requests (static loads) have been completed.
        	Thread.sleep(0);
        }
		
        if(selectedTeam!="") {
        	this.team = teams.get(selectedTeam);
        	if(this.team==null) {
                Log.LogMessage(LogLevels.BASIC, "Invalid team specified.");
        	}
        }
        
        initializeOrders();
		initializeRoutes();
		
        Log.LogMessage(LogLevels.BASIC, "EMSXAPI initialization complete");
	}

	private void initializeSession() throws SessionException, IOException, InterruptedException {

		if(this.environment == Environment.BETA) emsxServiceName = "//blp/emapisvc_beta";
		else if(this.environment == Environment.PRODUCTION) emsxServiceName = "//blp/emapisvc";

		sessionOptions = new SessionOptions();
		
		sessionOptions.setServerHost(this.host);
		sessionOptions.setServerPort(this.port);
		Log.LogMessage(LogLevels.BASIC, "Creating Session for " + this.host + ":" + this.port + " environment:" + environment.toString());
        this.session = new Session(sessionOptions, this);

        if(!this.session.start()) {
        	throw new SessionException("Unable to start session.");
        }
        
        while(!authorized) {
        	if(authorizationFailed) {
        		throw new SessionException("User authorization failed.");
        	}
        	Thread.sleep(0);
        }

        Log.LogMessage(LogLevels.BASIC, "Called for session start...");

	}

	private void initializeService() throws InterruptedException, IOException, ServiceException {

        if(!session.openService(emsxServiceName)) {
        	session.stop();
        	throw new ServiceException("Unable to open EMSX service");
        }
        
        emsxService = session.getService(emsxServiceName);

	}

	private void initializeFieldData() {
		
		orderFields.add(new SchemaFieldDefinition("API_SEQ_NUM", "ACTIVE", "INT64",1,1,"Special field"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ACCOUNT", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_AMOUNT", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ARRIVAL_PRICE", "ACTIVE", "FLOAT64",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ASSET_CLASS", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ASSIGNED_TRADER", "ACTIVE", "STRING",0,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_AVG_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BASKET_NAME", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BASKET_NUM", "ACTIVE", "INT32",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BLOCK_ID", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BROKER", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BROKER_COMM", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BSE_AVG_PRICE", "ACTIVE", "FLOAT64",1,1,"Static:O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_BSE_FILLED", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_CFD_FLAG", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_COMM_DIFF_FLAG", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_COMM_RATE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_CURRENCY_PAIR", "ACTIVE", "STRING",1,1,"Static:O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_DATE", "ACTIVE", "INT32",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_DAY_AVG_PRICE", "ACTIVE", "FLOAT64",0,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_DAY_FILL", "ACTIVE", "INT32",0,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_DIR_BROKER_FLAG", "ACTIVE", "STRING",0,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_EXCHANGE", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_EXCHANGE_DESTINATION", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_EXEC_INSTRUCTION", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_FILL_ID", "ACTIVE", "INT32",0,1,"Static:O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_FILLED", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_GTD_DATE", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_HAND_INSTRUCTION", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_IDLE_AMOUNT", "ACTIVE", "INT32",0,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_INVESTOR_ID", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ISIN", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_LIMIT_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_NOTES", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_NSE_AVG_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_NSE_FILLED", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ORDER_REF_ID", "ACTIVE", "STRING",0,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ORDER_TYPE", "ACTIVE", "STRING",1,1,"O,R")); // compensate for name difference in schema
		orderFields.add(new SchemaFieldDefinition("EMSX_ORIGINATE_TRADER", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ORIGINATE_TRADER_FIRM", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PERCENT_REMAIN", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PM_UUID", "ACTIVE", "INT32",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PORT_MGR", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PORT_NAME", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PORT_NUM", "ACTIVE", "INT32",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_POSITION", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PRINCIPAL", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_PRODUCT", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_QUEUED_DATE", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_QUEUED_TIME", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_REASON_CODE", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_REASON_DESC", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_REMAIN_BALANCE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ROUTE_ID", "ACTIVE", "INT32",0,1,"Static:O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_ROUTE_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_SEC_NAME", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_SEDOL", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_SEQUENCE", "ACTIVE", "INT32",1,1,"Static:O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_SETTLE_AMOUNT", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_SETTLE_DATE", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_SIDE", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_START_AMOUNT", "ACTIVE", "INT32",0,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STATUS", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STEP_OUT_BROKER", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STOP_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_END_TIME", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_PART_RATE1", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_PART_RATE2", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_START_TIME", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_STYLE", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TICKER", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TIF", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TIME_STAMP", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TRAD_UUID", "ACTIVE", "INT32",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TRADE_DESK", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TRADER", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TRADER_NOTES", "ACTIVE", "STRING",1,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TS_ORDNUM", "ACTIVE", "INT32",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_UNDERLYING_TICKER", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_USER_COMM_AMOUNT", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_USER_COMM_RATE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_USER_FEES", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_USER_NET_MONEY", "ACTIVE", "FLOAT64",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_WORK_PRICE", "ACTIVE", "FLOAT64",0,1,"Order"));
		orderFields.add(new SchemaFieldDefinition("EMSX_WORKING", "ACTIVE", "INT32",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("EMSX_YELLOW_KEY", "ACTIVE", "STRING",1,1,"Static:Order"));
		orderFields.add(new SchemaFieldDefinition("EVENT_STATUS", "ACTIVE", "INT32",1,1,"Special field"));
		orderFields.add(new SchemaFieldDefinition("MSG_SUB_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		orderFields.add(new SchemaFieldDefinition("MSG_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		
		
		routeFields.add(new SchemaFieldDefinition("API_SEQ_NUM", "ACTIVE", "INT64",1,1,"Special field"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ACCOUNT", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_AMOUNT", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_AVG_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_BROKER", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_BROKER_STATUS", "ACTIVE", "STRING",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_BSE_AVG_PRICE", "ACTIVE", "FLOAT64",1,1,"Static:O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_BSE_FILLED", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_CLEARING_ACCOUNT", "ACTIVE", "STRING",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_CLEARING_FIRM", "ACTIVE", "STRING",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_COMM_DIFF_FLAG", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_COMM_RATE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_CURRENCY_PAIR", "ACTIVE", "STRING",1,1,"Static:O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_CUSTOM_ACCOUNT", "ACTIVE", "STRING",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_DAY_AVG_PRICE", "ACTIVE", "FLOAT64",0,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_DAY_FILL", "ACTIVE", "INT32",0,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_EXCHANGE_DESTINATION", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_EXEC_INSTRUCTION", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_EXECUTE_BROKER", "ACTIVE", "STRING",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_FILL_ID", "ACTIVE", "INT32",0,1,"Static:O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_FILLED", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_GTD_DATE", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_HAND_INSTRUCTION", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_IS_MANUAL_ROUTE", "ACTIVE", "INT32",1,1,"Static:Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_LAST_FILL_DATE", "ACTIVE", "INT32",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_LAST_FILL_TIME", "ACTIVE", "INT32",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_LAST_MARKET", "ACTIVE", "STRING",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_LAST_PRICE", "ACTIVE", "FLOAT64",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_LAST_SHARES", "ACTIVE", "INT32",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_LIMIT_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_MISC_FEES", "ACTIVE", "FLOAT64",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_ID", "ACTIVE", "STRING",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_LEG_QUANTITY", "ACTIVE", "INT32",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_NUM_LEGS", "ACTIVE", "INT32",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_PERCENT_FILLED", "ACTIVE", "FLOAT64",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_RATIO", "ACTIVE", "FLOAT64",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_REMAIN_BALANCE", "ACTIVE", "FLOAT64",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_STRATEGY", "ACTIVE", "STRING",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ML_TOTAL_QUANTITY", "ACTIVE", "INT32",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_NOTES", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_NSE_AVG_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_NSE_FILLED", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ORDER_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_P_A", "ACTIVE", "STRING",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_PERCENT_REMAIN", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_PRINCIPAL", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_QUEUED_DATE", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_QUEUED_TIME", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_REASON_CODE", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_REASON_DESC", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_REMAIN_BALANCE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ROUTE_CREATE_DATE", "ACTIVE", "INT32",0,1,"Static:Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ROUTE_CREATE_TIME", "ACTIVE", "INT32",0,1,"Static:Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ROUTE_ID", "ACTIVE", "INT32",0,1,"Static:O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ROUTE_LAST_UPDATE_TIME", "ACTIVE", "INT32",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ROUTE_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_ROUTE_REF_ID", "ACTIVE", "STRING",1,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_SEQUENCE", "ACTIVE", "INT32",1,1,"Static:O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_SETTLE_AMOUNT", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_SETTLE_DATE", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STATUS", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STOP_PRICE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_END_TIME", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_PART_RATE1", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_PART_RATE2", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_START_TIME", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_STYLE", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_STRATEGY_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_TIF", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_TIME_STAMP", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_URGENCY_LEVEL", "ACTIVE", "INT32",0,1,"Route"));
		routeFields.add(new SchemaFieldDefinition("EMSX_USER_COMM_AMOUNT", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_USER_COMM_RATE", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_USER_FEES", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_USER_NET_MONEY", "ACTIVE", "FLOAT64",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EMSX_WORKING", "ACTIVE", "INT32",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("EVENT_STATUS", "ACTIVE", "INT32",1,1,"Special field"));
		routeFields.add(new SchemaFieldDefinition("MSG_SUB_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		routeFields.add(new SchemaFieldDefinition("MSG_TYPE", "ACTIVE", "STRING",1,1,"O,R"));
		
	}

	private void initializeTeams() {
		teams = new Teams(this);
	}

	void setTeam(Team selectedTeam) {
		this.team = selectedTeam;
	}

	private void initializeBrokerData() {
		brokers = new Brokers(this);
	}
	
	private void initializeOrders() {
		orders = new Orders(this);
	}
	
	private void initializeRoutes() {
		routes = new Routes(this);
	}

	@Override
	public void processEvent(Event event, Session session) {
        switch (event.eventType().intValue())
        {                
        case Event.EventType.Constants.ADMIN:
            processAdminEvent(event, session);
            break;
        case Event.EventType.Constants.SESSION_STATUS:
            processSessionEvent(event, session);
            break;
        case Event.EventType.Constants.SERVICE_STATUS:
            processServiceEvent(event, session);
            break;
        case Event.EventType.Constants.SUBSCRIPTION_DATA:
            processSubscriptionDataEvent(event, session);
            break;
        case Event.EventType.Constants.SUBSCRIPTION_STATUS:
            processSubscriptionStatus(event, session);
            break;
        case Event.EventType.Constants.RESPONSE:
            processResponse(event, session);
            break;
        default:
            processMiscEvents(event, session);
            break;
        }
	}
	
	private void processAdminEvent(Event event, Session session)
	{
    	MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
            Message msg = msgIter.next();
            if(msg.messageType().equals(SLOW_CONSUMER_WARNING)) {
            	Log.LogMessage(LogLevels.BASIC, "Slow Consumer Warning");
            } else if(msg.messageType().equals(SLOW_CONSUMER_WARNING_CLEARED)) {
            	Log.LogMessage(LogLevels.BASIC, "Slow Consumer Warning cleared");
            }
        }
	}

	private void processSessionEvent(Event event, Session session) 
	{
    	MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
            Message msg = msgIter.next();
            if(msg.messageType().equals(SESSION_STARTED)) {
            	Log.LogMessage(LogLevels.BASIC, "Session started");
            	if(serverConnection) {
            		try {
            			session.openService(authServiceName);
            		} catch (Exception e) {
                    	Log.LogMessage(LogLevels.BASIC, "Failed to open service");
					}
            	}
            } else if(msg.messageType().equals(SESSION_STARTUP_FAILURE)) {
            	Log.LogMessage(LogLevels.BASIC, "Session startup failure");
            } else if(msg.messageType().equals(SESSION_TERMINATED)) {
            	Log.LogMessage(LogLevels.BASIC, "Session terminated");
            } else if(msg.messageType().equals(SESSION_CONNECTION_UP)) {
            	Log.LogMessage(LogLevels.BASIC, "Session connection up");
            } else if(msg.messageType().equals(SESSION_CONNECTION_DOWN)) {
            	Log.LogMessage(LogLevels.BASIC, "Session connection down");
            }
        }
	}

    private void processServiceEvent(Event event, Session session) 
    {
    	MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
            Message msg = msgIter.next();
            if(msg.messageType().equals(SERVICE_OPENED)) {
     		   String serviceName = msg.getElementAsString("serviceName");
     		   if(serviceName == authServiceName) {
     			   Log.LogMessage(LogLevels.BASIC, "Auth Service opened");
                   sendAuthRequest(session);
     		   } else {
     			   Log.LogMessage(LogLevels.BASIC, "EMSX Service opened");
     		   }
            } else if(msg.messageType().equals(SERVICE_OPEN_FAILURE)) {
            	Log.LogMessage(LogLevels.BASIC, "Service open failed");
            }
        }
	}

	private void processSubscriptionStatus(Event event, Session session) 
	{
    	Log.LogMessage(LogLevels.DETAILED, "Processing SUBSCRIPTION_STATUS event");
    	
        MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
        	Message msg = msgIter.next();
        	CorrelationID cID = msg.correlationID();
        	if(subscriptionMessageHandlers.containsKey(cID)) {
        		MessageHandler mh = subscriptionMessageHandlers.get(cID);
        		mh.processMessage(msg);
        	} else {
        		Log.LogMessage(LogLevels.BASIC, "Unexpected SUBSCRIPTION_STATUS event recieved (CID=" + cID.toString() + "): " + msg.toString());
        	}
        }
    }

    private void processSubscriptionDataEvent(Event event, Session session)
    {
    	Log.LogMessage(LogLevels.DETAILED, "Processing SUBSCRIPTION_DATA event");
    	
        MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
        	Message msg = msgIter.next();
        	CorrelationID cID = msg.correlationID();
        	if(subscriptionMessageHandlers.containsKey(cID)) {
        		MessageHandler mh = subscriptionMessageHandlers.get(cID);
        		mh.processMessage(msg);
        	} else {
        		Log.LogMessage(LogLevels.BASIC, "Unexpected SUBSCRIPTION_DATA event recieved (CID=" + cID.toString() + "): " + msg.toString());
        	}
        }
    }
    
	private void processResponse(Event event, Session session)
    {
    	Log.LogMessage(LogLevels.DETAILED, "Processing RESPONSE event");
    	
        MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
        	Message msg = msgIter.next();
        	CorrelationID cID = msg.correlationID();
        	
        	if(cID==authRequestID) {
        		if(msg.messageType().equals(AUTHORIZATION_SUCCESS))
        		{
        			Log.LogMessage(LogLevels.DETAILED, "User authorised.");
        			authorized = true;
        		} else {
        			Log.LogMessage(LogLevels.DETAILED, "User not authorized.");
        			authorizationFailed=true;
        		}
        		return;
        	}

        	if(requestMessageHandlers.containsKey(cID)) {
        		MessageHandler mh = requestMessageHandlers.get(cID);
        		mh.processMessage(msg);
        		requestMessageHandlers.remove(cID);
            	Log.LogMessage(LogLevels.BASIC, "EMSXAPI: MessageHandler removed [" + cID + "]");
        	} else {
        		Log.LogMessage(LogLevels.BASIC, "Unexpected RESPONSE event recieved: " + msg.toString());
        	}
        }
    }

	private void processMiscEvents(Event event, Session session) 
    {
        MessageIterator msgIter = event.messageIterator();
        while (msgIter.hasNext()) {
            Log.LogMessage(LogLevels.BASIC, "Event: processing misc event");
        }
    }

   private void sendAuthRequest(Session session)
   {
       Service authService = session.getService(authServiceName);
       Request authReq = authService.createAuthorizationRequest();

       authReq.set("authId", user);
       authReq.set("ipAddress", userIP);

       Log.LogMessage(LogLevels.BASIC, "Sending authentication request: " + authReq.toString());

       userIdentity = session.createIdentity();

       authRequestID = new CorrelationID();

       try
       {
           session.sendAuthorizationRequest(authReq, userIdentity, authRequestID);
       }
       catch (Exception e)
       {
           Log.LogMessage(LogLevels.BASIC, "Failed to send authentication request: " + e.getMessage());
       }

   }

	void submitRequest(Request request, MessageHandler handler) {

		CorrelationID newCID = new CorrelationID();

		Log.LogMessage(LogLevels.BASIC, "EMSXAPI: Submitting request...adding MessageHandler [" + newCID + "]");

		requestMessageHandlers.put(newCID, handler);
		
		try {
			if(serverConnection) {
				session.sendRequest(request,userIdentity, newCID);
			} else {
				session.sendRequest(request,newCID);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void subscribe(String topic, MessageHandler handler) {

		CorrelationID newCID = new CorrelationID();
		subscriptionMessageHandlers.put(newCID, handler);
		
		Log.LogMessage(LogLevels.BASIC, "Added Subscription message handler: " + newCID.toString());
		
		try {
	        Subscription sub = new Subscription(topic,newCID);
	        
	        SubscriptionList subs = new SubscriptionList();
	        subs.add(sub);
	        
	        if(serverConnection) {
	        	session.subscribe(subs,userIdentity);
	        } else {
	        	session.subscribe(subs);
	        }

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

	@Override
	public void processNotification(Notification notification) {
		if(globalNotificationHandler!=null && !notification.consume) globalNotificationHandler.processNotification(notification);
	}
}
