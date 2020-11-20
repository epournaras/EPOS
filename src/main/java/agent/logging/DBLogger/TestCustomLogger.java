package agent.logging.DBLogger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import org.github.jamm.MemoryMeter;
import org.zeromq.ZMQ;

import loggers.EventLog;
import loggers.MemLog;
import loggers.RawLog;
import pgpersist.PersistenceClient;
import pgpersist.SqlDataItem;
import pgpersist.SqlInsertTemplate;


public class TestCustomLogger
{

    public static void main(String[] args)
    {

        System.out.printf("TestBasicLog (2019-04-26)\n" );


        // MockClient
        // requires 3 arguments:
        // 1. port for sending messages to daemon

        if (args.length < 1)
        {
            System.err.printf("usage: daemon.port sleep.time.milliseconds\n" );
            return;
        }

        // parse arguments
        // parse listen port
        final int			daemonPort = Integer.parseInt(args [0]),
                sleepTimeMilliSeconds = Integer.parseInt(args [1]),
                output_queue_size = 1000;

        System.out.printf( "daemonPort : %d\n", daemonPort );

        String				daemonConnectString = "tcp://localhost:" + daemonPort;

        System.out.printf( "daemonConnectString : %s\n", daemonConnectString );

        // ZeroMQ Context
        ZMQ.Context 			zmqContext = ZMQ.context(1);
        System.out.printf( "ZeroMQ Context created\n" );

        // PersistenceClient, for sending data items to be persisted to the daemon
        PersistenceClient persistenceClient = new PersistenceClient(zmqContext, daemonConnectString, output_queue_size );
        System.out.printf( "PersistenceClient created\n" );

        // ------------
        // -- RawLog --
        // ------------

        // set loggng level
        // log all (1); only log warnings (2); errors (3)
        final int				rawLogLevel = 1;
        RawLog.setPeristenceClient(persistenceClient);
        RawLog.setErrorLevelThreshold(rawLogLevel);

        // set Protopeer peer
        // if this program is running a Protopeer peer, then pass a reference to the instantiated Protopeer peer
        // this will add the peer id and the epoch number for the log
        // e.g you would obtain the reference to the peer after the following Protopeer call
        //Peer 						newPeer = new Peer(peerIndex);

        // if you don't have a Protopeer peer, simply set this to null (the log will write -1 for the peer and also for the epoch number)
        RawLog.setPeerId(null);

        // set DIAS Network Id
        // in the case where you have multiple DIAS networks that are persisting data to the same daemon
        // the DIAS networks can be differentiated by specifiying a diasNetworkId
        final 	int				diasNetworkId = 0;
        RawLog.setDIASNetworkId(diasNetworkId);

        System.out.printf( "RawLog setup with rawLogLevel %d\n", rawLogLevel );

        // --------------
        // -- EventLog --
        // --------------

        // setup EventLog
        EventLog.setPeristenceClient(persistenceClient);
        EventLog.setPeerId(null);
        EventLog.setDIASNetworkId(diasNetworkId);


        // ------------
        // -- MemLog --
        // ------------

        // memory logging is more complicated to setup because it requires a memory meter agent, that runs inside the JVM
        // the loading of the agent is handled inside the launch script (e.g. start.test.sh)
        // the loading of the agent can fail for a number of reasons; thus we must check with hasInstrumentation() if the agent was correctly loaded

        final ArrayList<String>				someList = new ArrayList<String>();

        // initialise MemLog
        final MemoryMeter 	instrument = new MemoryMeter();

        if( !MemoryMeter.hasInstrumentation() )
            System.out.printf( "MemLog: No instrumentation\n" );
        else
        {
            MemLog.setPersistenceClient(persistenceClient);
            MemLog.setMeasurementInstrument(instrument);
            MemLog.setPeerId(null);
            MemLog.setDIASNetworkId(diasNetworkId);
            MemLog.startTimer(60);

            System.out.printf( "MemLog setup\n" );


        }

        // add an object that we wish to track the memory footprint
        MemLog.add_object("TestBasicLog", "someList", someList);

        // add more objects to monitor
        // MemLog.add_object(...)


        // -------------------
        // -- Custom Logger --
        // -------------------

        // step 1. create the table in SQL -> sql/definitions/customlog.sql

        // step 2.  send the template to the Peristence daemon, so that it knows how to write the data to the database
        final String 				sql_insert_template_custom  = "INSERT INTO customlog(dt,run,iteration,dim_0,dim_1) VALUES({dt}, {run}, {iteration}, {dim_0}, {dim_1});";

        // step 3. send that string to the daemon
        persistenceClient.sendSqlInsertTemplate( new SqlInsertTemplate( "custom", sql_insert_template_custom ) );


        // -----------
        // -- start --
        // -----------


        long					counter = 0L;

        boolean 				b_loop = true;

        LinkedHashMap<String,String>	 outputMap = new LinkedHashMap<String,String> ();

        SimpleDateFormat				dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Random							randomNumberGenerator = new Random();

        // TODO: create a list and add to memlog

        // Start
        RawLog.print(1,"Hello!");
        EventLog.logEvent("TestBasicLog", "main", "loop start");


        while( b_loop )
        {
            ++counter;

            // wait
            try
            {
                RawLog.print(1,"Hello, oupss... any btw, my counter is " + Long.toString(counter));

                EventLog.logEvent("TestBasicLog", "main", "counter", Long.toString(counter));

                // send some data to the custom log
                // fields: dt,run,iteration,dim_0,dim_1
                LinkedHashMap<String,String>          record = new LinkedHashMap<String,String>();

                record.put("dt", "'" + dateFormatter.format( System.currentTimeMillis() ) + "'" );
                record.put("run", Long.toString(counter) );
                record.put("iteration", Long.toString(counter + 1000));
                record.put("dim_0", "100" );
                record.put("dim_1", "101" );

                persistenceClient.sendSqlDataItem( new SqlDataItem( "custom", record ) );


                // add some data to the list, so that we can see it's memory increasing
                someList.add(Long.toString(counter));

                // wait a bit
                Thread.currentThread().sleep(sleepTimeMilliSeconds);
            }
            catch (InterruptedException e)
            {
                b_loop = false;
            }


        }// while
    }// main
}// class
