What's this?
===========

This is a mock project that I use for my learning by doing experiments with [mongoDB](http://www.mongodb.org/) and the [Java mongoDB driver](http://docs.mongodb.org/ecosystem/drivers/java/).

It is a telnet based chat application, in which a  [replication set](https://docs.mongodb.org/manual/core/replication-introduction/) of mongoDB instances is abused as a distributed chat server with failover capabilities.

For a more detailed descripton see: [these Slides](https://docs.google.com/presentation/d/1BqhEDx_oNElXKKVE8cGEEw_gfe2tDQOUSeVi-Jl4uCQ/edit?usp=sharing).

Preparation
-----------

Before the chat application can be used, you need to install mongoDB, start one or more mongoDB instances and setup a replication set.

For mongoDB installation instructions [see here](http://docs.mongodb.org/manual/installation/).

The following test scenario uses three mongoDB instances on a Windows 7 virtual machine with static ip "192.168.200.131" and one instance on a Xubuntu 14.04 VM with static ip "192.168.200.3".

*Note that you need to open the ports in the Windows firewall, that you want to use for your mongoDB instances. In this scenario: 27017-19.*

1) Create data folders for your mongoDB instances on the windows client.

````bat
   mkdir \data\rs0-0
   mkdir \data\rs0-1
   mkdir \data\rs0-2
````

2) Go to the path of your mongoDB binaries and start the instances. [More information on the parameters used in this example](http://docs.mongodb.org/manual/tutorial/deploy-replica-set-for-testing/)

````bat
   mongod --port 27017 --dbpath /data/rs0-0 --replSet rs0 --smallfiles --oplogSize 128
   mongod --port 27018 --dbpath /data/rs0-1 --replSet rs0 --smallfiles --oplogSize 128
   mongod --port 27019 --dbpath /data/rs0-2 --replSet rs0 --smallfiles --oplogSize 128
````

3) On the linux client create the data folder for your mongoDB instance and start it.

````bash
   mkdir -p ~/data/rs0-3
   mongod --port 27020 --dbpath ~/data/rs0-3 --replSet rs0 --smallfiles --oplogSize 128
````

4) Open a connection to the mongoDB instance on the linux client and initialize the replication set. For testing purposes the linux instance (if available) will always be prefered as primary.
 
````bash
   mongo --port 27020
   rsconf = {
	"_id" : "rs0",
	"version" : 5,
	"members" : [
		{
			"_id" : 0,
			"host" : "192.168.200.131:27017",
			"priority" : 0.5
		},
		{
			"_id" : 1,
			"host" : "192.168.200.131:27018",
			"priority" : 0.5
		},
		{
			"_id" : 2,
			"host" : "192.168.200.131:27019",
			"priority" : 0.5
		},
		{
			"_id" : 3,
			"host" : "192.168.200.3:27020"
            "priority" : 1
		}
	]
   }
   rs.initiate(rsconf)
   use test
   db.createCollection("messages", {capped:true, autoIndexId:false, size:100000, max:20})
````

*Note: if you changed any IPs or ports in your setup, you need to update them also on the client side:* [src/main/resources/replica_config.json](src/main/resources/replica_config.json)

Usage
-----

1) Install telnet client for [windows](https://technet.microsoft.com/en-us/library/cc771275%28v=ws.10%29.aspx) or [android](https://play.google.com/store/apps/details?id=com.telnet). On ubuntu the package *telnet* is already installed by default.
2) Run the Main method of the MongoChat application
3) Use your telnet client to connect to the port 4321

*Note: Any connection to port 4321 will result in an independent chat client participating in the chat. This means that you can share your telnet port with other users that dont have a local MongoChat application running. This is especially useful, if you are using telnet on your mobile device.*

Chat Commands
-------------

- `!quit`: disconnects the client from the chat
- `!name NAME`: Changes client user name to *NAME*
- `!archive HOURS [LIMIT]`: Displays messages from the archive. The archive holds all messages except the recent 20 messages, which are still visible in the clients output stream. The *HOURS* defines a time span from now to now-HOURS to limit the number of displayed messages. For a more strict limit, use the optional LIMIT parameter, that gives the maximal number of messages to be displayed.

