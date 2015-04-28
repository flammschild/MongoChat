

Preparation
-----------

Setting up a mongodb replication cluster for testing purposes on a Windows machine (192.168.200.131) and one Linux client (192.168.200.3) (for other operating systems change the dbpath accordingly) and create the MessageCollection. 

````bat

   ## Auf 192.168.200.131
   mkdir \data\rs0-0
   mkdir \data\rs0-1
   mkdir \data\rs0-2
   mongod --port 27017 --dbpath /data/rs0-0 --replSet rs0 --smallfiles --oplogSize 128
   mongod --port 27018 --dbpath /data/rs0-1 --replSet rs0 --smallfiles --oplogSize 128
   mongod --port 27019 --dbpath /data/rs0-2 --replSet rs0 --smallfiles --oplogSize 128

   ## Auf 192.168.200.3
   mkdir -p \data\rs0-3
   mongod --port 27020 --dbpath ~/data/rs0-3 --replSet rs0 --smallfiles --oplogSize 128
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
   db.createCollection("messages", {capped:true, autoIndexId:false, size:100000, max:20})
````
